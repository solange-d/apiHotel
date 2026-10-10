package com.esos.apiHotel.service;

import com.esos.apiHotel.entity.Reserva;
import com.esos.apiHotel.entity.Quarto;
import com.esos.apiHotel.entity.Hospede;
import com.esos.apiHotel.dto.request.ReservaRequest;
import com.esos.apiHotel.dto.request.ReservaAtualizacaoRequest;
import com.esos.apiHotel.dto.request.DadosReserva;
import com.esos.apiHotel.exception.RegraNegocioException;
import java.time.LocalDate;
import com.esos.apiHotel.dto.response.ReservaResponse;
import com.esos.apiHotel.repository.ReservaRepository;
import com.esos.apiHotel.repository.QuartoRepository;
import com.esos.apiHotel.repository.HospedeRepository;
import com.esos.apiHotel.repository.PagamentoRepository;
import com.esos.apiHotel.mapper.ReservaMapper;
import com.esos.apiHotel.exception.RecursoNaoEncontradoException;
import com.esos.apiHotel.exception.ConflitoException;
import com.esos.apiHotel.enums.StatusReserva;
import com.esos.apiHotel.enums.StatusPagamento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;

@Service
@Transactional
public class ReservaService {
    private final ReservaRepository repository;
    private final HospedeRepository hospedes;
    private final QuartoRepository quartos;
    private final PagamentoRepository pagamentos;
    private final ReservaMapper mapper;
    private final ValidacaoReserva validacao;
    private final EntityManager entityManager;

    public ReservaService(ReservaRepository repository, HospedeRepository hospedes,
            QuartoRepository quartos, PagamentoRepository pagamentos, ReservaMapper mapper,
            ValidacaoReserva validacao, EntityManager entityManager) {
        this.repository = repository;
        this.hospedes = hospedes;
        this.quartos = quartos;
        this.pagamentos = pagamentos;
        this.mapper = mapper;
        this.validacao = validacao;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public Page<ReservaResponse> listar(Pageable pagina) {
        return listar(pagina, (root, query, cb) -> cb.conjunction());
    }

    @Transactional(readOnly = true)
    public Page<ReservaResponse> listar(Pageable pagina, Specification<Reserva> filtro) {
        Specification<Reserva> visiveis = (root, query, cb) -> cb.isFalse(root.get("excluido"));
        return repository.findAll(visiveis.and(filtro), pagina).map(mapper::toResponse);
    }

    private Reserva buscar(Long id) {
        return repository.findByIdAndExcluidoFalse(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada."));
    }

    private Reserva bloquear(Long id) {
        Reserva reserva = repository.buscarComBloqueio(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada."));
        // Atualiza o estado depois de esperar um bloqueio de outra transação.
        entityManager.refresh(reserva);
        if (reserva.isExcluido()) {
            throw new RecursoNaoEncontradoException("Reserva não encontrada.");
        }
        return reserva;
    }

    @Transactional(readOnly = true)
    public ReservaResponse obter(Long id) {
        return mapper.toResponse(buscar(id));
    }

    private Quarto bloquearQuarto(Long id) {
        Quarto quarto = quartos.buscarComBloqueio(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quarto não encontrado."));
        entityManager.refresh(quarto);
        return quarto;
    }

    private void preencher(Reserva reserva, DadosReserva dados, Quarto quarto, Long ignorarId, BigDecimal diariaContratada) {
        Hospede hospede = hospedes.findById(dados.hospedeId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Hóspede não encontrado."));
        BigDecimal total = validacao.calcular(dados, quarto, diariaContratada);
        if (repository.contarConflitos(quarto.getId(), dados.entrada(), dados.saida(), ignorarId) > 0) {
            throw new ConflitoException("Quarto indisponível no período.");
        }
        reserva.setHospede(hospede);
        reserva.setQuarto(quarto);
        reserva.setEntrada(dados.entrada());
        reserva.setSaida(dados.saida());
        reserva.setQuantidadeHospedes(dados.quantidadeHospedes());
        reserva.setValorDiariaContratada(diariaContratada);
        reserva.setValorTotal(total);
    }

    public ReservaResponse criar(ReservaRequest dados) {
        if (dados.entrada().isBefore(LocalDate.now())) {
            throw new RegraNegocioException("Entrada não pode ser passada na criação.");
        }
        Quarto quarto = bloquearQuarto(dados.quartoId());
        Reserva reserva = new Reserva();
        preencher(reserva, dados, quarto, -1L, quarto.getValorDiaria());
        reserva.setStatus(StatusReserva.CONFIRMADA);
        return mapper.toResponse(repository.save(reserva));
    }

    public ReservaResponse atualizar(Long id, ReservaAtualizacaoRequest dados) {
        Long quartoAnterior = repository.buscarQuartoId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada."));
        // Todos os quartos são bloqueados por ID crescente, antes da reserva.
        bloquearQuarto(Math.min(quartoAnterior, dados.quartoId()));
        Quarto destino = bloquearQuarto(Math.max(quartoAnterior, dados.quartoId()));
        if (!destino.getId().equals(dados.quartoId())) {
            destino = bloquearQuarto(dados.quartoId());
        }
        Reserva reserva = bloquear(id);
        if (!reserva.getQuarto().getId().equals(quartoAnterior)) {
            throw new ConflitoException("Reserva alterada simultaneamente. Refaça a operação.");
        }
        if (reserva.getStatus() != StatusReserva.CONFIRMADA) {
            throw new ConflitoException("Reserva cancelada não pode ser alterada.");
        }
        if (pagamentos.existsByReservaIdAndStatusAndExcluidoFalse(id, StatusPagamento.CONFIRMADO)) {
            throw new ConflitoException("Estorne os pagamentos antes de alterar a reserva.");
        }
        validarPeriodoAtualizacao(reserva, dados);
        BigDecimal diaria = reserva.getQuarto().getId().equals(destino.getId())
                ? reserva.getValorDiariaContratada() : destino.getValorDiaria();
        preencher(reserva, dados, destino, id, diaria);
        return mapper.toResponse(reserva);
    }

    private void validarPeriodoAtualizacao(Reserva reserva, ReservaAtualizacaoRequest dados) {
        LocalDate hoje = LocalDate.now();
        if (!reserva.getSaida().isAfter(hoje)) {
            throw new ConflitoException("Reserva encerrada não pode ser alterada.");
        }
        if (reserva.getEntrada().isBefore(hoje)) {
            if (!dados.entrada().equals(reserva.getEntrada()) || !dados.quartoId().equals(reserva.getQuarto().getId())) {
                throw new RegraNegocioException("Reserva em andamento deve manter entrada e quarto originais.");
            }
            if (!dados.saida().isAfter(hoje)) {
                throw new RegraNegocioException("Saída da reserva em andamento deve ser posterior a hoje.");
            }
        } else if (dados.entrada().isBefore(hoje)) {
            throw new RegraNegocioException("Uma alteração não pode antecipar a entrada para uma data passada.");
        }
    }

    public ReservaResponse cancelar(Long id) {
        Reserva reserva = bloquear(id);
        exigirSemPagamentos(reserva);
        reserva.setStatus(StatusReserva.CANCELADA);
        return mapper.toResponse(reserva);
    }

    public void excluir(Long id) {
        Reserva reserva = bloquear(id);
        exigirSemPagamentos(reserva);
        reserva.setStatus(StatusReserva.CANCELADA);
        reserva.setExcluido(true);
    }

    private void exigirSemPagamentos(Reserva reserva) {
        if (pagamentos.existsByReservaIdAndStatusAndExcluidoFalse(reserva.getId(), StatusPagamento.CONFIRMADO)) {
            throw new ConflitoException("Estorne os pagamentos antes de cancelar ou excluir.");
        }
    }
}

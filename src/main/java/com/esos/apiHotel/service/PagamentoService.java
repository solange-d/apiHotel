package com.esos.apiHotel.service;

import com.esos.apiHotel.entity.Reserva;
import com.esos.apiHotel.entity.Pagamento;
import com.esos.apiHotel.dto.request.PagamentoRequest;
import com.esos.apiHotel.dto.response.PagamentoResponse;
import com.esos.apiHotel.repository.ReservaRepository;
import com.esos.apiHotel.repository.PagamentoRepository;
import com.esos.apiHotel.mapper.PagamentoMapper;
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

@Service
@Transactional
public class PagamentoService {
    private final PagamentoRepository repository;
    private final ReservaRepository reservas;
    private final PagamentoMapper mapper;
    private final EntityManager entityManager;

    public PagamentoService(PagamentoRepository repository, ReservaRepository reservas,
            PagamentoMapper mapper, EntityManager entityManager) {
        this.repository = repository;
        this.reservas = reservas;
        this.mapper = mapper;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public Page<PagamentoResponse> listar(Pageable pagina) {
        return listar(pagina, (root, query, cb) -> cb.conjunction());
    }

    @Transactional(readOnly = true)
    public Page<PagamentoResponse> listar(Pageable pagina, Specification<Pagamento> filtro) {
        Specification<Pagamento> visiveis = (root, query, cb) -> cb.isFalse(root.get("excluido"));
        return repository.findAll(visiveis.and(filtro), pagina).map(mapper::toResponse);
    }

    private Pagamento buscar(Long id) {
        return repository.findByIdAndExcluidoFalse(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento não encontrado."));
    }

    @Transactional(readOnly = true)
    public PagamentoResponse obter(Long id) {
        return mapper.toResponse(buscar(id));
    }

    private Reserva bloquearReserva(Long id) {
        Reserva reserva = reservas.buscarComBloqueio(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada."));
        entityManager.refresh(reserva);
        if (reserva.isExcluido()) {
            throw new RecursoNaoEncontradoException("Reserva não encontrada.");
        }
        return reserva;
    }

    public PagamentoResponse criar(PagamentoRequest dados) {
        Reserva reserva = bloquearReserva(dados.reservaId());
        if (reserva.getStatus() != StatusReserva.CONFIRMADA) {
            throw new ConflitoException("Reserva cancelada não aceita pagamento.");
        }
        if (repository.totalPago(reserva.getId()).add(dados.valor()).compareTo(reserva.getValorTotal()) > 0) {
            throw new ConflitoException("Pagamento excede o saldo da reserva.");
        }
        Pagamento pagamento = new Pagamento();
        pagamento.setReserva(reserva);
        pagamento.setValor(dados.valor());
        pagamento.setForma(dados.forma());
        pagamento.setStatus(StatusPagamento.CONFIRMADO);
        return mapper.toResponse(repository.save(pagamento));
    }

    private Pagamento bloquearPagamento(Long id) {
        Long reservaId = repository.buscarReservaId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento não encontrado."));
        // A reserva excluída continua permitindo a manutenção do histórico estornado.
        Reserva reserva = reservas.findById(reservaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada."));
        entityManager.lock(reserva, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(reserva);
        Pagamento pagamento = buscar(id);
        entityManager.refresh(pagamento);
        if (pagamento.isExcluido()) {
            throw new RecursoNaoEncontradoException("Pagamento não encontrado.");
        }
        return pagamento;
    }

    public PagamentoResponse atualizar(Long id, PagamentoRequest dados) {
        Pagamento pagamento = bloquearPagamento(id);
        if (pagamento.getStatus() != StatusPagamento.ESTORNADO) {
            throw new ConflitoException("Pagamento confirmado é imutável. Estorne antes de corrigir.");
        }
        if (!pagamento.getReserva().getId().equals(dados.reservaId())) {
            throw new ConflitoException("O vínculo com a reserva não pode ser alterado.");
        }
        pagamento.setValor(dados.valor());
        pagamento.setForma(dados.forma());
        return mapper.toResponse(pagamento);
    }

    public void excluir(Long id) {
        Pagamento pagamento = bloquearPagamento(id);
        if (pagamento.getStatus() != StatusPagamento.ESTORNADO) {
            throw new ConflitoException("Estorne o pagamento antes de excluir.");
        }
        pagamento.setExcluido(true);
    }

    public PagamentoResponse estornar(Long id) {
        Pagamento pagamento = bloquearPagamento(id);
        pagamento.setStatus(StatusPagamento.ESTORNADO);
        return mapper.toResponse(pagamento);
    }
}

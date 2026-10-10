package com.esos.apiHotel.service;

import com.esos.apiHotel.entity.Quarto;
import com.esos.apiHotel.dto.request.QuartoRequest;
import com.esos.apiHotel.dto.response.QuartoResponse;
import com.esos.apiHotel.repository.QuartoRepository;
import com.esos.apiHotel.repository.ReservaRepository;
import com.esos.apiHotel.mapper.QuartoMapper;
import com.esos.apiHotel.exception.ConflitoException;
import com.esos.apiHotel.exception.RecursoNaoEncontradoException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import jakarta.persistence.EntityManager;

@Service
@Transactional
public class QuartoService {
    private final QuartoRepository repository;
    private final QuartoMapper mapper;
    private final ReservaRepository reservas;
    private final EntityManager entityManager;

    public QuartoService(QuartoRepository repository, QuartoMapper mapper, ReservaRepository reservas, EntityManager entityManager) {
        this.repository = repository;
        this.mapper = mapper;
        this.reservas = reservas;
        this.entityManager = entityManager;
    }

    private Quarto buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quarto não encontrado."));
    }

    @Transactional(readOnly = true)
    public Page<QuartoResponse> listar(Pageable pagina) {
        return listar(pagina, (root, query, cb) -> cb.conjunction());
    }

    @Transactional(readOnly = true)
    public Page<QuartoResponse> listar(Pageable pagina, Specification<Quarto> filtro) {
        return repository.findAll(filtro, pagina).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public QuartoResponse obter(Long id) {
        return mapper.toResponse(buscar(id));
    }

    public QuartoResponse criar(QuartoRequest dados) {
        if (repository.existsByNumero(dados.numero().trim())) {
            throw new ConflitoException("Numero já cadastrado.");
        }
        Quarto entidade = new Quarto();
        mapper.atualizar(entidade, dados);
        return mapper.toResponse(repository.save(entidade));
    }

    public QuartoResponse atualizar(Long id, QuartoRequest dados) {
        Quarto entidade = repository.buscarComBloqueio(id).orElseThrow(() -> new RecursoNaoEncontradoException("Quarto não encontrado."));
        if (repository.existsByNumeroAndIdNot(dados.numero().trim(), id)) {
            throw new ConflitoException("Numero já cadastrado.");
        }

        entityManager.refresh(entidade);
        if (!dados.ativo() && reservas.contarReservasVigentes(id, LocalDate.now()) > 0) {
            throw new ConflitoException("Cancele ou realoque as reservas vigentes ou futuras antes de inativar.");
        }
        if (reservas.contarAcimaCapacidade(id, LocalDate.now(), dados.capacidade()) > 0) {
            throw new ConflitoException("A capacidade proposta não atende às reservas vigentes ou futuras.");
        }

        mapper.atualizar(entidade, dados);
        return mapper.toResponse(repository.save(entidade));
    }

    public void excluir(Long id) {
        Quarto entidade = repository.buscarComBloqueio(id).orElseThrow(() -> new RecursoNaoEncontradoException("Quarto não encontrado."));
        if (reservas.existsByQuartoId(id)) {
            throw new ConflitoException("Cadastro possui reservas vinculadas.");
        }
        repository.delete(entidade);
    }
}
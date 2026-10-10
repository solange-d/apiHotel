package com.esos.apiHotel.service;
import com.esos.apiHotel.entity.Hospede;
import com.esos.apiHotel.dto.request.HospedeRequest;
import com.esos.apiHotel.dto.response.HospedeResponse;
import com.esos.apiHotel.repository.HospedeRepository;
import com.esos.apiHotel.repository.ReservaRepository;
import com.esos.apiHotel.mapper.HospedeMapper;
import com.esos.apiHotel.exception.ConflitoException;
import com.esos.apiHotel.exception.RecursoNaoEncontradoException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

@Service
@Transactional
public class HospedeService {
    private final HospedeRepository repository;
    private final HospedeMapper mapper;
    private final ReservaRepository reservas;
    private final EntityManager entityManager;

    public HospedeService(HospedeRepository repository, HospedeMapper mapper, ReservaRepository reservas, EntityManager entityManager) {
        this.repository = repository;
        this.mapper = mapper;
        this.reservas = reservas;
        this.entityManager = entityManager;
    }

    private Hospede buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Hospede não encontrado."));
    }

    @Transactional(readOnly = true)
    public Page<HospedeResponse> listar(Pageable pagina) {
        return listar(pagina, (root, query, cb) -> cb.conjunction());
    }

    @Transactional(readOnly = true)
    public Page<HospedeResponse> listar(Pageable pagina, Specification<Hospede> filtro) {
        return repository.findAll(filtro, pagina).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public HospedeResponse obter(Long id) {
        return mapper.toResponse(buscar(id));
    }

    public HospedeResponse criar(HospedeRequest dados) {
        if (repository.existsByDocumento(dados.documento().trim())) {
            throw new ConflitoException("Documento já cadastrado.");
        }
        Hospede entidade = new Hospede();
        mapper.atualizar(entidade, dados);
        return mapper.toResponse(repository.save(entidade));
    }

    public HospedeResponse atualizar(Long id, HospedeRequest dados) {
        Hospede entidade = buscar(id);
        if (repository.existsByDocumentoAndIdNot(dados.documento().trim(), id)) {
            throw new ConflitoException("Documento já cadastrado.");
        }

        mapper.atualizar(entidade, dados);
        return mapper.toResponse(repository.save(entidade));
    }

    public void excluir(Long id) {
        Hospede entidade = buscar(id);
        if (reservas.existsByHospedeId(id)) {
            throw new ConflitoException("Cadastro possui reservas vinculadas.");
        }
        repository.delete(entidade);
    }
}

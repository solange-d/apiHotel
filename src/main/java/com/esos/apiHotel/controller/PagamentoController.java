package com.esos.apiHotel.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.esos.apiHotel.service.Filtros;
import com.esos.apiHotel.service.PagamentoService;
import com.esos.apiHotel.dto.request.PagamentoRequest;
import com.esos.apiHotel.dto.response.PagamentoResponse;

@RestController
@RequestMapping("/api/v1/pagamentos")
public class PagamentoController {
    private final PagamentoService service;

    public PagamentoController(PagamentoService service) {
        this.service = service;
    }

    @GetMapping
    public Page<PagamentoResponse> listar(
            @RequestParam(defaultValue="0") int pagina,
            @RequestParam(defaultValue="20") int tamanho,
            @RequestParam(defaultValue="id") String ordenarPor,
            @RequestParam(defaultValue="asc") String direcao,
            @RequestParam(required=false) Long reservaId, @RequestParam(required=false) com.esos.apiHotel.enums.StatusPagamento status, @RequestParam(required=false) com.esos.apiHotel.enums.FormaPagamento forma) {
        return service.listar(Paginacao.criar(pagina, tamanho, ordenarPor, direcao,
                Set.of("id", "valor", "status", "forma")), Filtros.pagamentos(reservaId, status, forma));
    }

    @GetMapping("/{id}")
    public PagamentoResponse obter(@PathVariable Long id) {
        return service.obter(id);
    }

    @PostMapping
    public ResponseEntity<PagamentoResponse> criar(@Valid @RequestBody PagamentoRequest dados) {
        var resposta = service.criar(dados);
        return ResponseEntity.created(URI.create("/api/v1/pagamentos/" + resposta.id())).body(resposta);
    }

    @PutMapping("/{id}")
    public PagamentoResponse atualizar(@PathVariable Long id, @Valid @RequestBody PagamentoRequest dados) {
        return service.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/estornar")
    public PagamentoResponse estornar(@PathVariable Long id) {
        return service.estornar(id);
    }
}

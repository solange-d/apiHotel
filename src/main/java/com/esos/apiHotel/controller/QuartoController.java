package com.esos.apiHotel.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.esos.apiHotel.service.Filtros;
import com.esos.apiHotel.service.QuartoService;
import com.esos.apiHotel.dto.request.QuartoRequest;
import com.esos.apiHotel.dto.response.QuartoResponse;

@RestController
@RequestMapping("/api/v1/quartos")
public class QuartoController {
    private final QuartoService service;

    public QuartoController(QuartoService service) {
        this.service = service;
    }

    @GetMapping
    public Page<QuartoResponse> listar(
            @RequestParam(defaultValue="0") int pagina,
            @RequestParam(defaultValue="20") int tamanho,
            @RequestParam(defaultValue="id") String ordenarPor,
            @RequestParam(defaultValue="asc") String direcao,
            @RequestParam(required=false) String numero, @RequestParam(required=false) String tipo, @RequestParam(required=false) Boolean ativo, @RequestParam(required=false) Integer capacidadeMinima) {
        return service.listar(Paginacao.criar(pagina, tamanho, ordenarPor, direcao,
                Set.of("id", "numero", "tipo", "capacidade", "valorDiaria")), Filtros.quartos(numero, tipo, ativo, capacidadeMinima));
    }

    @GetMapping("/{id}")
    public QuartoResponse obter(@PathVariable Long id) {
        return service.obter(id);
    }

    @PostMapping
    public ResponseEntity<QuartoResponse> criar(@Valid @RequestBody QuartoRequest dados) {
        var resposta = service.criar(dados);
        return ResponseEntity.created(URI.create("/api/v1/quartos/" + resposta.id())).body(resposta);
    }

    @PutMapping("/{id}")
    public QuartoResponse atualizar(@PathVariable Long id, @Valid @RequestBody QuartoRequest dados) {
        return service.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}

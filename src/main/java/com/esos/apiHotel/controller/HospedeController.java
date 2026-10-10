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
import com.esos.apiHotel.service.HospedeService;
import com.esos.apiHotel.dto.request.HospedeRequest;
import com.esos.apiHotel.dto.response.HospedeResponse;

@RestController
@RequestMapping("/api/v1/hospedes")
public class HospedeController {
    private final HospedeService service;

    public HospedeController(HospedeService service) {
        this.service = service;
    }

    @GetMapping
    public Page<HospedeResponse> listar(
            @RequestParam(defaultValue="0") int pagina,
            @RequestParam(defaultValue="20") int tamanho,
            @RequestParam(defaultValue="id") String ordenarPor,
            @RequestParam(defaultValue="asc") String direcao,
            @RequestParam(required=false) String nome, @RequestParam(required=false) String documento, @RequestParam(required=false) String email) {
        return service.listar(Paginacao.criar(pagina, tamanho, ordenarPor, direcao,
                Set.of("id", "nome", "documento", "email")), Filtros.hospedes(nome, documento, email));
    }

    @GetMapping("/{id}")
    public HospedeResponse obter(@PathVariable Long id) {
        return service.obter(id);
    }

    @PostMapping
    public ResponseEntity<HospedeResponse> criar(@Valid @RequestBody HospedeRequest dados) {
        var resposta = service.criar(dados);
        return ResponseEntity.created(URI.create("/api/v1/hospedes/" + resposta.id())).body(resposta);
    }

    @PutMapping("/{id}")
    public HospedeResponse atualizar(@PathVariable Long id, @Valid @RequestBody HospedeRequest dados) {
        return service.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}

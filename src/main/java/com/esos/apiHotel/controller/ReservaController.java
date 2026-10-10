package com.esos.apiHotel.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.Set;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
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
import com.esos.apiHotel.service.ReservaService;
import com.esos.apiHotel.dto.request.ReservaRequest;
import com.esos.apiHotel.dto.request.ReservaAtualizacaoRequest;
import com.esos.apiHotel.dto.response.ReservaResponse;

@RestController
@RequestMapping("/api/v1/reservas")
public class ReservaController {
    private final ReservaService service;

    public ReservaController(ReservaService service) {
        this.service = service;
    }

    @GetMapping
    public Page<ReservaResponse> listar(
            @RequestParam(defaultValue="0") int pagina,
            @RequestParam(defaultValue="20") int tamanho,
            @RequestParam(defaultValue="id") String ordenarPor,
            @RequestParam(defaultValue="asc") String direcao,
            @RequestParam(required=false) Long hospedeId, @RequestParam(required=false) Long quartoId, @RequestParam(required=false) com.esos.apiHotel.enums.StatusReserva status, @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate entrada, @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate saida) {
        return service.listar(Paginacao.criar(pagina, tamanho, ordenarPor, direcao,
                Set.of("id", "entrada", "saida", "valorTotal", "status")), Filtros.reservas(hospedeId, quartoId, status, entrada, saida));
    }

    @GetMapping("/{id}")
    public ReservaResponse obter(@PathVariable Long id) {
        return service.obter(id);
    }

    @PostMapping
    public ResponseEntity<ReservaResponse> criar(@Valid @RequestBody ReservaRequest dados) {
        var resposta = service.criar(dados);
        return ResponseEntity.created(URI.create("/api/v1/reservas/" + resposta.id())).body(resposta);
    }

    @PutMapping("/{id}")
    public ReservaResponse atualizar(@PathVariable Long id, @Valid @RequestBody ReservaAtualizacaoRequest dados) {
        return service.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/cancelar")
    public ReservaResponse cancelar(@PathVariable Long id) {
        return service.cancelar(id);
    }
}

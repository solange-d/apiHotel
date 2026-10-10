package com.esos.apiHotel.mapper;

import com.esos.apiHotel.entity.Quarto;
import com.esos.apiHotel.dto.request.QuartoRequest;
import com.esos.apiHotel.dto.response.QuartoResponse;
import org.springframework.stereotype.Component;

@Component
public class QuartoMapper {
    public QuartoResponse toResponse(Quarto entidade) {
        return new QuartoResponse(
                entidade.getId(),
                entidade.getNumero(),
                entidade.getTipo(),
                entidade.getCapacidade(),
                entidade.getValorDiaria(),
                entidade.getAtivo());
    }

    public void atualizar(Quarto entidade, QuartoRequest dados) {
        entidade.setNumero(dados.numero().trim());
        entidade.setTipo(dados.tipo().trim());
        entidade.setCapacidade(dados.capacidade());
        entidade.setValorDiaria(dados.valorDiaria());
        entidade.setAtivo(dados.ativo());
    }
}

package com.esos.apiHotel.mapper;

import com.esos.apiHotel.entity.Hospede;
import com.esos.apiHotel.dto.request.HospedeRequest;
import com.esos.apiHotel.dto.response.HospedeResponse;
import org.springframework.stereotype.Component;

@Component
public class HospedeMapper {
    public HospedeResponse toResponse(Hospede entidade) {
        return new HospedeResponse(
                entidade.getId(),
                entidade.getNome(),
                entidade.getDocumento(),
                entidade.getEmail(),
                entidade.getTelefone());
    }

    public void atualizar(Hospede entidade, HospedeRequest dados) {
        entidade.setNome(dados.nome().trim());
        entidade.setDocumento(dados.documento().trim());
        entidade.setEmail(dados.email().trim());
        entidade.setTelefone(dados.telefone().trim());
    }
}


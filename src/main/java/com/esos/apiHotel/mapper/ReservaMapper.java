package com.esos.apiHotel.mapper;

import com.esos.apiHotel.entity.Reserva;
import com.esos.apiHotel.dto.response.ReservaResponse;
import org.springframework.stereotype.Component;

@Component
public class ReservaMapper {
    public ReservaResponse toResponse(Reserva entidade) {
        return new ReservaResponse(
                entidade.getId(),
                entidade.getHospede().getId(),
                entidade.getQuarto().getId(),
                entidade.getEntrada(),
                entidade.getSaida(),
                entidade.getQuantidadeHospedes(),
                entidade.getValorTotal(),
                entidade.getValorDiariaContratada(),
                entidade.getStatus());
    }
}

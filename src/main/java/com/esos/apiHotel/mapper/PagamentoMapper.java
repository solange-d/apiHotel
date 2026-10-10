package com.esos.apiHotel.mapper;

import com.esos.apiHotel.entity.Pagamento;
import com.esos.apiHotel.dto.response.PagamentoResponse;
import org.springframework.stereotype.Component;

@Component
public class PagamentoMapper {
    public PagamentoResponse toResponse(Pagamento entidade) {
        return new PagamentoResponse(
                entidade.getId(),
                entidade.getReserva().getId(),
                entidade.getValor(),
                entidade.getForma(),
                entidade.getStatus());
    }
}

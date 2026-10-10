package com.esos.apiHotel.dto.response;

import java.math.BigDecimal;
import com.esos.apiHotel.enums.FormaPagamento;
import com.esos.apiHotel.enums.StatusPagamento;
public record PagamentoResponse(
        Long id,
        Long reservaId,
        BigDecimal valor,
        FormaPagamento forma,
        StatusPagamento status) { }

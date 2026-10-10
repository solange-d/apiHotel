package com.esos.apiHotel.dto.response;

import java.math.BigDecimal;

public record QuartoResponse(
        Long id,
        String numero,
        String tipo,
        Integer capacidade,
        BigDecimal valorDiaria,
        Boolean ativo) { }
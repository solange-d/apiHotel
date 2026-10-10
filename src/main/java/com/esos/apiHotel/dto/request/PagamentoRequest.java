package com.esos.apiHotel.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import com.esos.apiHotel.enums.FormaPagamento;
public record PagamentoRequest(
        @NotNull @Positive Long reservaId,
        @NotNull @DecimalMin("0.01") @Digits(integer=10, fraction=2) BigDecimal valor,
        @NotNull FormaPagamento forma) { }

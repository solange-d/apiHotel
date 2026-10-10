package com.esos.apiHotel.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record QuartoRequest(
        @NotBlank @Size(max=20) String numero,
        @NotBlank @Size(max=50) String tipo,
        @NotNull @Positive Integer capacidade,
        @NotNull @DecimalMin("0.01") @Digits(integer=10, fraction=2) BigDecimal valorDiaria,
        @NotNull Boolean ativo) { }

package com.esos.apiHotel.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record ReservaRequest(
        @NotNull @Positive Long hospedeId,
        @NotNull @Positive Long quartoId,
        @NotNull @FutureOrPresent LocalDate entrada,
        @NotNull LocalDate saida,
        @NotNull @Positive Integer quantidadeHospedes) implements DadosReserva { }

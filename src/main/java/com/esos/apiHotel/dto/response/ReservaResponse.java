package com.esos.apiHotel.dto.response;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.esos.apiHotel.enums.StatusReserva;
public record ReservaResponse(
        Long id,
        Long hospedeId,
        Long quartoId,
        LocalDate entrada,
        LocalDate saida,
        Integer quantidadeHospedes,
        BigDecimal valorTotal,
        BigDecimal valorDiariaContratada,
        StatusReserva status) { }
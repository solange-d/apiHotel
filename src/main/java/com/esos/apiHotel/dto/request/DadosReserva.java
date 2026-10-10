package com.esos.apiHotel.dto.request;

import java.time.LocalDate;

public interface DadosReserva {
    Long hospedeId();
    Long quartoId();
    LocalDate entrada();
    LocalDate saida();
    Integer quantidadeHospedes();
}

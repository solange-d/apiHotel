package com.esos.apiHotel.dto.response;

public record HospedeResponse(
        Long id,
        String nome,
        String documento,
        String email,
        String telefone) { }

package com.esos.apiHotel.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HospedeRequest(
        @NotBlank @Size(max=120) String nome,
        @NotBlank @Size(max=30) String documento,
        @NotBlank @Email @Size(max=254) String email,
        @NotBlank @Size(max=30) String telefone) { }

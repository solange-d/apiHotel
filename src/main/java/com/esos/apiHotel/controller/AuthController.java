package com.esos.apiHotel.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.esos.apiHotel.security.AutenticacaoService;
import com.esos.apiHotel.dto.request.LoginRequest;
import com.esos.apiHotel.dto.response.LoginResponse;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AutenticacaoService service;

    public AuthController(AutenticacaoService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(service.login(request));
    }
}

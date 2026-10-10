package com.esos.apiHotel.dto.response;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) { }

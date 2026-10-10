package com.esos.apiHotel;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import com.esos.apiHotel.exception.GlobalExceptionHandler;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTests {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void falhaInesperadaNaoExpoeDetalhes() {
        var resposta = handler.inesperado(new IllegalStateException("senha-interna-que-nao-deve-aparecer"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resposta.getStatusCode());
        assertEquals("Falha interna ao processar a solicitação.", resposta.getBody().getDetail());
    }

}

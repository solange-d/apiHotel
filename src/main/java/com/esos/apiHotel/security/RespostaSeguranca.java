package com.esos.apiHotel.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class RespostaSeguranca {
    private RespostaSeguranca() { }

    public static void enviar(HttpServletResponse response, int status) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        if (status == 401) response.setHeader("WWW-Authenticate", "Bearer");
        String title = status == 401 ? "Unauthorized" : "Forbidden";
        String detail = status == 401 ? "Credenciais ausentes ou inválidas." : "Perfil sem permissão para esta operação.";
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + title
                + "\",\"status\":" + status + ",\"detail\":\"" + detail + "\"}");
    }
}

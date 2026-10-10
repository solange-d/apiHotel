package com.esos.apiHotel.service;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;
import com.esos.apiHotel.dto.request.DadosReserva;
import com.esos.apiHotel.entity.Quarto;
import com.esos.apiHotel.exception.ConflitoException;
import com.esos.apiHotel.exception.RegraNegocioException;

@Component
public class ValidacaoReserva {
    public static final long MAXIMO_NOITES = 365;
    public static final BigDecimal MAXIMO_TOTAL = new BigDecimal("9999999999.99");

    public BigDecimal calcular(DadosReserva dados, Quarto quarto, BigDecimal diariaContratada) {
        long noites = ChronoUnit.DAYS.between(dados.entrada(), dados.saida());
        if (noites < 1 || noites > MAXIMO_NOITES) {
            throw new RegraNegocioException("A estadia deve ter entre 1 e 365 noites.");
        }
        if (!quarto.getAtivo()) {
            throw new ConflitoException("Quarto inativo.");
        }
        if (dados.quantidadeHospedes() > quarto.getCapacidade()) {
            throw new RegraNegocioException("Capacidade do quarto excedida.");
        }
        BigDecimal total = diariaContratada.multiply(BigDecimal.valueOf(noites));
        if (total.compareTo(MAXIMO_TOTAL) > 0) {
            throw new RegraNegocioException("O total da reserva excede 9999999999.99.");
        }
        return total;
    }
}

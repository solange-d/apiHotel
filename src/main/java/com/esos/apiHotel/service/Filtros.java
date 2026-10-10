package com.esos.apiHotel.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import com.esos.apiHotel.entity.Hospede;
import com.esos.apiHotel.entity.Quarto;
import com.esos.apiHotel.entity.Reserva;
import com.esos.apiHotel.entity.Pagamento;
import com.esos.apiHotel.enums.StatusReserva;
import com.esos.apiHotel.enums.StatusPagamento;
import com.esos.apiHotel.enums.FormaPagamento;
import com.esos.apiHotel.exception.RegraNegocioException;

public final class Filtros {
    private Filtros() { }

    public static Specification<Hospede> hospedes(String nome, String documento, String email) {
        return (root, query, cb) -> {
            List<Predicate> filtros = new ArrayList<>();
            if (nome != null) filtros.add(cb.like(cb.lower(root.get("nome")), contem(nome), '\\'));
            if (documento != null) filtros.add(cb.equal(root.get("documento"), documento.trim()));
            if (email != null) filtros.add(cb.equal(cb.lower(root.get("email")), email.trim().toLowerCase(java.util.Locale.ROOT)));
            return cb.and(filtros.toArray(Predicate[]::new));
        };
    }

    public static Specification<Quarto> quartos(String numero, String tipo, Boolean ativo, Integer capacidadeMinima) {
        if (capacidadeMinima != null && capacidadeMinima < 1) throw new RegraNegocioException("Capacidade mínima deve ser positiva.");
        return (root, query, cb) -> {
            List<Predicate> filtros = new ArrayList<>();
            if (numero != null) filtros.add(cb.equal(root.get("numero"), numero.trim()));
            if (tipo != null) filtros.add(cb.equal(cb.lower(root.get("tipo")), tipo.trim().toLowerCase(java.util.Locale.ROOT)));
            if (ativo != null) filtros.add(cb.equal(root.get("ativo"), ativo));
            if (capacidadeMinima != null) filtros.add(cb.greaterThanOrEqualTo(root.get("capacidade"), capacidadeMinima));
            return cb.and(filtros.toArray(Predicate[]::new));
        };
    }

    public static Specification<Reserva> reservas(Long hospedeId, Long quartoId, StatusReserva status,
            LocalDate entrada, LocalDate saida) {
        positivo(hospedeId); positivo(quartoId);
        return (root, query, cb) -> {
            List<Predicate> filtros = new ArrayList<>();
            if (hospedeId != null) filtros.add(cb.equal(root.get("hospede").get("id"), hospedeId));
            if (quartoId != null) filtros.add(cb.equal(root.get("quarto").get("id"), quartoId));
            if (status != null) filtros.add(cb.equal(root.get("status"), status));
            if (entrada != null) filtros.add(cb.equal(root.get("entrada"), entrada));
            if (saida != null) filtros.add(cb.equal(root.get("saida"), saida));
            return cb.and(filtros.toArray(Predicate[]::new));
        };
    }

    public static Specification<Pagamento> pagamentos(Long reservaId, StatusPagamento status, FormaPagamento forma) {
        positivo(reservaId);
        return (root, query, cb) -> {
            List<Predicate> filtros = new ArrayList<>();
            if (reservaId != null) filtros.add(cb.equal(root.get("reserva").get("id"), reservaId));
            if (status != null) filtros.add(cb.equal(root.get("status"), status));
            if (forma != null) filtros.add(cb.equal(root.get("forma"), forma));
            return cb.and(filtros.toArray(Predicate[]::new));
        };
    }

    private static void positivo(Long id) {
        if (id != null && id < 1) throw new RegraNegocioException("IDs devem ser positivos.");
    }

    private static String contem(String valor) {
        return "%" + valor.trim().toLowerCase(java.util.Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }
}

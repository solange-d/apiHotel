package com.esos.apiHotel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.esos.apiHotel.enums.StatusReserva;

@Entity
@Table(name="reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean excluido;

    @Version
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long versao;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(nullable=false)
    private Hospede hospede;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(nullable=false)
    private Quarto quarto;

    @Column(nullable=false)
    private LocalDate entrada;

    @Column(nullable=false)
    private LocalDate saida;

    @Column(nullable=false)
    private Integer quantidadeHospedes;

    @Column(nullable=false, precision=12, scale=2)
    private BigDecimal valorTotal;

    // Nullable para compatibilidade com bancos anteriores ao snapshot de preço.
    @Column(precision = 12, scale = 2)
    private BigDecimal valorDiariaContratada;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private StatusReserva status;

    public Long getId() {
        return id;
    }

    public boolean isExcluido() {
        return excluido;
    }

    public void setExcluido(boolean excluido) {
        this.excluido = excluido;
    }

    public Hospede getHospede() {
        return hospede;
    }

    public void setHospede(Hospede hospede) {
        this.hospede = hospede;
    }

    public Quarto getQuarto() {
        return quarto;
    }

    public void setQuarto(Quarto quarto) {
        this.quarto = quarto;
    }

    public LocalDate getEntrada() {
        return entrada;
    }

    public void setEntrada(LocalDate entrada) {
        this.entrada = entrada;
    }

    public LocalDate getSaida() {
        return saida;
    }

    public void setSaida(LocalDate saida) {
        this.saida = saida;
    }

    public Integer getQuantidadeHospedes() {
        return quantidadeHospedes;
    }

    public void setQuantidadeHospedes(Integer quantidadeHospedes) {
        this.quantidadeHospedes = quantidadeHospedes;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public StatusReserva getStatus() {
        return status;
    }

    public void setStatus(StatusReserva status) {
        this.status = status;
    }

    public BigDecimal getValorDiariaContratada() {
        if (valorDiariaContratada != null) return valorDiariaContratada;
        // Registros legados recuperam a diária do próprio total, nunca do preço atual do quarto.
        long noites = java.time.temporal.ChronoUnit.DAYS.between(entrada, saida);
        return valorTotal.divide(BigDecimal.valueOf(noites), 2, java.math.RoundingMode.UNNECESSARY);
    }

    public void setValorDiariaContratada(BigDecimal valorDiariaContratada) {
        this.valorDiariaContratada = valorDiariaContratada;
    }
}


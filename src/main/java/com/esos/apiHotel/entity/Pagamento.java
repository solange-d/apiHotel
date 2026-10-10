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
import com.esos.apiHotel.enums.FormaPagamento;
import com.esos.apiHotel.enums.StatusPagamento;

@Entity
@Table(name="pagamentos")
public class Pagamento {

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
    private Reserva reserva;

    @Column(nullable=false, precision=12, scale=2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private FormaPagamento forma;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private StatusPagamento status;

    public Long getId() {
        return id;
    }

    public boolean isExcluido() {
        return excluido;
    }

    public void setExcluido(boolean excluido) {
        this.excluido = excluido;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public FormaPagamento getForma() {
        return forma;
    }

    public void setForma(FormaPagamento forma) {
        this.forma = forma;
    }

    public StatusPagamento getStatus() {
        return status;
    }

    public void setStatus(StatusPagamento status) {
        this.status = status;
    }
}

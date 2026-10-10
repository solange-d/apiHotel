package com.esos.apiHotel.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.esos.apiHotel.enums.*;

@Entity
@Table(name="pagamentos")
public class Pagamento {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 public Long getId() { return id; }
 @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(nullable=false) private Reserva reserva;
 public Reserva getReserva() { return reserva; }
 public void setReserva(Reserva value) { this.reserva = value; }
 @Column(nullable=false, precision=12, scale=2) private BigDecimal valor;
 public BigDecimal getValor() { return valor; }
 public void setValor(BigDecimal value) { this.valor = value; }
 @Enumerated(EnumType.STRING) @Column(nullable=false) private FormaPagamento forma;
 public FormaPagamento getForma() { return forma; }
 public void setForma(FormaPagamento value) { this.forma = value; }
 @Enumerated(EnumType.STRING) @Column(nullable=false) private StatusPagamento status;
 public StatusPagamento getStatus() { return status; }
 public void setStatus(StatusPagamento value) { this.status = value; }
}

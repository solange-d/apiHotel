package com.esos.apiHotel.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.esos.apiHotel.enums.*;

@Entity
@Table(name="hospedes")
public class Hospede {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 public Long getId() { return id; }
 @Column(nullable=false, length=120) private String nome;
 public String getNome() { return nome; }
 public void setNome(String value) { this.nome = value; }
 @Column(nullable=false, unique=true, length=30) private String documento;
 public String getDocumento() { return documento; }
 public void setDocumento(String value) { this.documento = value; }
 @Column(nullable=false, length=254) private String email;
 public String getEmail() { return email; }
 public void setEmail(String value) { this.email = value; }
 @Column(nullable=false, length=30) private String telefone;
 public String getTelefone() { return telefone; }
 public void setTelefone(String value) { this.telefone = value; }
}
package com.esos.apiHotel;

import com.esos.apiHotel.service.HospedeService;
import com.esos.apiHotel.service.QuartoService;
import com.esos.apiHotel.service.ReservaService;
import com.esos.apiHotel.service.PagamentoService;
import com.esos.apiHotel.dto.request.HospedeRequest;
import com.esos.apiHotel.dto.request.QuartoRequest;
import com.esos.apiHotel.dto.request.ReservaRequest;
import com.esos.apiHotel.dto.request.PagamentoRequest;
import com.esos.apiHotel.enums.StatusPagamento;
import com.esos.apiHotel.enums.StatusReserva;
import com.esos.apiHotel.enums.FormaPagamento;
import com.esos.apiHotel.exception.ConflitoException;
import com.esos.apiHotel.exception.RegraNegocioException;
import com.esos.apiHotel.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.BeforeEach;
import com.esos.apiHotel.repository.HospedeRepository;
import com.esos.apiHotel.repository.QuartoRepository;
import com.esos.apiHotel.repository.ReservaRepository;
import com.esos.apiHotel.repository.PagamentoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest @ActiveProfiles("test")
class ApiHotelApplicationTests {
 @Autowired HospedeRepository hospedeRepository;
 @Autowired QuartoRepository quartoRepository;
 @Autowired ReservaRepository reservaRepository;
 @Autowired PagamentoRepository pagamentoRepository;
 @BeforeEach void limpar() {
  pagamentoRepository.deleteAll(); reservaRepository.deleteAll(); quartoRepository.deleteAll(); hospedeRepository.deleteAll();
 }
 @Autowired HospedeService hospedes;
 @Autowired QuartoService quartos;
 @Autowired ReservaService reservas;
 @Autowired PagamentoService pagamentos;
 @Test void fluxoReservaPagamentoCancelamento() {
  var h=hospedes.criar(new HospedeRequest("Alan","DOC-001","alan@example.com","47999999999"));
  var q=quartos.criar(new QuartoRequest("101","CASAL",2,new BigDecimal("150.00"),true));
  var inicio=LocalDate.now().plusDays(10);
  var d=new ReservaRequest(h.id(),q.id(),inicio,inicio.plusDays(2),2);
  var reserva=reservas.criar(d);
  assertEquals(0,new BigDecimal("300.00").compareTo(reserva.valorTotal()));
  assertThrows(ConflitoException.class,()->reservas.criar(d));
  var vizinha=reservas.criar(new ReservaRequest(h.id(),q.id(),inicio.plusDays(2),inicio.plusDays(3),1));
  assertNotNull(vizinha.id());
  var p=pagamentos.criar(new PagamentoRequest(reserva.id(),new BigDecimal("200.00"),FormaPagamento.PIX));
  assertThrows(ConflitoException.class,()->pagamentos.criar(new PagamentoRequest(reserva.id(),new BigDecimal("100.01"),FormaPagamento.PIX)));
  assertThrows(ConflitoException.class,()->reservas.cancelar(reserva.id()));
  assertEquals(StatusPagamento.ESTORNADO,pagamentos.estornar(p.id()).status());
  assertEquals(StatusReserva.CANCELADA,reservas.cancelar(reserva.id()).status());
  assertThrows(ConflitoException.class,()->pagamentos.criar(new PagamentoRequest(reserva.id(),BigDecimal.ONE,FormaPagamento.PIX)));
  assertNotNull(reservas.criar(d).id());
 }
 @Test void rejeitaDadosDeNegocioInvalidos() {
  var h=hospedes.criar(new HospedeRequest("Alan","DOC-002","alan@example.com","47999999999"));
  assertThrows(ConflitoException.class,()->hospedes.criar(new HospedeRequest("Outro","DOC-002","outro@example.com","47999999999")));
  var q=quartos.criar(new QuartoRequest("102","SOLTEIRO",1,new BigDecimal("100.00"),true));
  var dia=LocalDate.now().plusDays(10);
  assertThrows(RegraNegocioException.class,()->reservas.criar(new ReservaRequest(h.id(),q.id(),dia,dia,1)));
  assertThrows(RegraNegocioException.class,()->reservas.criar(new ReservaRequest(h.id(),q.id(),dia,dia.plusDays(1),2)));
  assertThrows(RecursoNaoEncontradoException.class,()->hospedes.obter(Long.MAX_VALUE));
 }
 @Test void limiteMonetarioEDuracao() {
  var h=hospedes.criar(new HospedeRequest("Limite","DOC-LIMITE","limite@example.com","47999999999"));
  var q=quartos.criar(new QuartoRequest("999","LUXO",2,new BigDecimal("9999999999.99"),true));
  var dia=LocalDate.now().plusDays(10);
  assertEquals(0, new BigDecimal("9999999999.99").compareTo(reservas.criar(new ReservaRequest(h.id(),q.id(),dia,dia.plusDays(1),1)).valorTotal()));
  assertThrows(RegraNegocioException.class,()->reservas.criar(new ReservaRequest(h.id(),q.id(),dia.plusDays(2),dia.plusDays(4),1)));
  assertThrows(RegraNegocioException.class,()->reservas.criar(new ReservaRequest(h.id(),q.id(),dia.plusDays(2),dia.plusDays(368),1)));
 }
}

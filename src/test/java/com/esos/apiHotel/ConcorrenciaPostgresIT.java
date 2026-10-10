package com.esos.apiHotel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.esos.apiHotel.dto.request.HospedeRequest;
import com.esos.apiHotel.dto.request.QuartoRequest;
import com.esos.apiHotel.dto.request.ReservaRequest;
import com.esos.apiHotel.dto.request.PagamentoRequest;
import com.esos.apiHotel.service.HospedeService;
import com.esos.apiHotel.service.QuartoService;
import com.esos.apiHotel.service.ReservaService;
import com.esos.apiHotel.service.PagamentoService;
import com.esos.apiHotel.repository.HospedeRepository;
import com.esos.apiHotel.repository.QuartoRepository;
import com.esos.apiHotel.repository.ReservaRepository;
import com.esos.apiHotel.repository.PagamentoRepository;
import com.esos.apiHotel.enums.FormaPagamento;
import com.esos.apiHotel.exception.ConflitoException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
    "spring.datasource.url=${TEST_DB_URL}",
    "spring.datasource.username=${TEST_DB_USER}",
    "spring.datasource.password=${TEST_DB_PASSWORD}",
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named="TEST_DB_URL", matches="jdbc:postgresql:.*")
class ConcorrenciaPostgresIT {
    @Autowired HospedeService hospedes;
    @Autowired QuartoService quartos;
    @Autowired ReservaService reservas;
    @Autowired PagamentoService pagamentos;
    @Autowired HospedeRepository hospedeRepository;
    @Autowired QuartoRepository quartoRepository;
    @Autowired ReservaRepository reservaRepository;
    @Autowired PagamentoRepository pagamentoRepository;

    @BeforeEach
    void limpar() {
        pagamentoRepository.deleteAll(); reservaRepository.deleteAll(); quartoRepository.deleteAll(); hospedeRepository.deleteAll();
    }

    private void disputar(Callable<?> operacao) throws Exception {
        // Cada thread atravessa o proxy @Transactional e realiza seu próprio commit.
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);
        Callable<Boolean> tentativa = () -> {
            prontas.countDown();
            if (!inicio.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout na barreira.");
            try {
                operacao.call();
                return true;
            } catch (ConflitoException conflito) {
                return false;
            }
        };
        try {
            List<Future<Boolean>> resultados = List.of(executor.submit(tentativa), executor.submit(tentativa));
            assertTrue(prontas.await(10, TimeUnit.SECONDS));
            inicio.countDown();
            long sucesso = 0;
            for (Future<Boolean> resultado : resultados) if (resultado.get(20, TimeUnit.SECONDS)) sucesso++;
            assertEquals(1, sucesso, "Uma transação deve confirmar e a outra deve detectar conflito.");
        } finally {
            inicio.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void duasReservasMesmoPeriodo() throws Exception {
        var h = hospedes.criar(new HospedeRequest("Concorrência","PG-1","pg@example.com","47999999999"));
        var q = quartos.criar(new QuartoRequest("PG-101","CASAL",2,new BigDecimal("100.00"),true));
        LocalDate dia = LocalDate.now().plusDays(10);
        var dados = new ReservaRequest(h.id(),q.id(),dia,dia.plusDays(2),2);
        disputar(() -> reservas.criar(dados));
        assertEquals(1, reservaRepository.count());
    }

    @Test
    void pagamentosSimultaneosNaoExcedemSaldo() throws Exception {
        var h = hospedes.criar(new HospedeRequest("Concorrência","PG-2","pg@example.com","47999999999"));
        var q = quartos.criar(new QuartoRequest("PG-102","CASAL",2,new BigDecimal("100.00"),true));
        LocalDate dia = LocalDate.now().plusDays(10);
        var r = reservas.criar(new ReservaRequest(h.id(),q.id(),dia,dia.plusDays(1),1));
        disputar(() -> pagamentos.criar(new PagamentoRequest(r.id(),new BigDecimal("60.00"),FormaPagamento.PIX)));
        assertEquals(1, pagamentoRepository.count());
        assertEquals(0, new BigDecimal("60.00").compareTo(pagamentoRepository.totalPago(r.id())));
    }
}

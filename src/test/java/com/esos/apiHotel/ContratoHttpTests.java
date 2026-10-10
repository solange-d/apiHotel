package com.esos.apiHotel;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.jayway.jsonpath.JsonPath;
import com.esos.apiHotel.repository.HospedeRepository;
import com.esos.apiHotel.enums.StatusReserva;
import com.esos.apiHotel.entity.Reserva;
import java.math.BigDecimal;
import com.esos.apiHotel.repository.QuartoRepository;
import com.esos.apiHotel.repository.ReservaRepository;
import com.esos.apiHotel.repository.PagamentoRepository;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ContratoHttpTests {
    @Autowired MockMvc mvc;
    @Autowired HospedeRepository hospedes;
    @Autowired QuartoRepository quartos;
    @Autowired ReservaRepository reservas;
    @Autowired PagamentoRepository pagamentos;

    @BeforeEach
    void limpar() {
        pagamentos.deleteAll(); reservas.deleteAll(); quartos.deleteAll(); hospedes.deleteAll();
    }

    private MockHttpServletRequestBuilder admin(MockHttpServletRequestBuilder request) {
        return request.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private long criar(String recurso, String json) throws Exception {
        var result = mvc.perform(admin(post("/api/v1/" + recurso)).contentType("application/json").content(json))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andReturn().getResponse();
        Number id = JsonPath.read(result.getContentAsString(), "$.id");
        assertEquals("/api/v1/" + recurso + "/" + id.longValue(), result.getHeader("Location"));
        return id.longValue();
    }

    private String hospede(String documento) {
        return "{\"nome\":\"Alan\",\"documento\":\"" + documento + "\",\"email\":\"alan@example.com\",\"telefone\":\"47999999999\"}";
    }

    private String quarto(String numero, int capacidade, boolean ativo, int valor) {
        return "{\"numero\":\"" + numero + "\",\"tipo\":\"CASAL\",\"capacidade\":" + capacidade
                + ",\"valorDiaria\":" + valor + ",\"ativo\":" + ativo + "}";
    }

    private String reserva(long hospede, long quarto, LocalDate inicio, int noites, int pessoas) {
        return "{\"hospedeId\":" + hospede + ",\"quartoId\":" + quarto + ",\"entrada\":\"" + inicio
                + "\",\"saida\":\"" + inicio.plusDays(noites) + "\",\"quantidadeHospedes\":" + pessoas + "}";
    }

    private String pagamento(long reserva, int valor) {
        return "{\"reservaId\":" + reserva + ",\"valor\":" + valor + ",\"forma\":\"PIX\"}";
    }

    @Test
    void contratoDosVinteEndpointsCrudEHistorico() throws Exception {
        long h = criar("hospedes", hospede("HTTP-001"));
        long q = criar("quartos", quarto("101", 2, true, 100));
        LocalDate dia = LocalDate.now().plusDays(10);
        long r = criar("reservas", reserva(h, q, dia, 2, 2));
        long p = criar("pagamentos", pagamento(r, 100));
        for (String caminho : new String[]{"hospedes/"+h,"quartos/"+q,"reservas/"+r,"pagamentos/"+p}) {
            mvc.perform(admin(get("/api/v1/"+caminho))).andExpect(status().isOk()).andExpect(jsonPath("$.id").exists());
            mvc.perform(admin(get("/api/v1/"+caminho.split("/")[0]))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
        mvc.perform(admin(put("/api/v1/hospedes/"+h)).contentType("application/json").content(hospede("HTTP-ALTERADO")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.documento").value("HTTP-ALTERADO"));
        mvc.perform(admin(put("/api/v1/quartos/"+q)).contentType("application/json").content(quarto("101",2,true,200)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valorDiaria").value(200));
        mvc.perform(admin(get("/api/v1/reservas/"+r))).andExpect(jsonPath("$.valorTotal").value(200));
        mvc.perform(admin(put("/api/v1/reservas/"+r)).contentType("application/json").content(reserva(h,q,dia,3,2)))
                .andExpect(status().isConflict());
        mvc.perform(admin(put("/api/v1/pagamentos/"+p)).contentType("application/json").content(pagamento(r,50)))
                .andExpect(status().isConflict());
        mvc.perform(admin(delete("/api/v1/reservas/"+r))).andExpect(status().isConflict());
        mvc.perform(admin(delete("/api/v1/pagamentos/"+p))).andExpect(status().isConflict());
        mvc.perform(admin(patch("/api/v1/pagamentos/"+p+"/estornar"))).andExpect(status().isOk());
        mvc.perform(admin(put("/api/v1/pagamentos/"+p)).contentType("application/json").content(pagamento(r,50)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ESTORNADO"));
        mvc.perform(admin(put("/api/v1/reservas/"+r)).contentType("application/json").content(reserva(h,q,dia,3,2)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valorTotal").value(300));
        mvc.perform(admin(delete("/api/v1/pagamentos/"+p))).andExpect(status().isNoContent());
        mvc.perform(admin(delete("/api/v1/reservas/"+r))).andExpect(status().isNoContent());
        mvc.perform(admin(get("/api/v1/pagamentos/"+p))).andExpect(status().isNotFound());
        mvc.perform(admin(get("/api/v1/reservas/"+r))).andExpect(status().isNotFound());
        mvc.perform(admin(get("/api/v1/reservas"))).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(admin(get("/api/v1/pagamentos"))).andExpect(jsonPath("$.totalElements").value(0));
        assertTrue(reservas.findById(r).orElseThrow().isExcluido());
        assertTrue(pagamentos.findById(p).orElseThrow().isExcluido());
        // Os vínculos físicos permanecem preservados e impedem a exclusão dos cadastros históricos.
        mvc.perform(admin(delete("/api/v1/hospedes/"+h))).andExpect(status().isConflict());
        mvc.perform(admin(delete("/api/v1/quartos/"+q))).andExpect(status().isConflict());
        long hLivre = criar("hospedes", hospede("SEM-RESERVA"));
        long qLivre = criar("quartos", quarto("102",2,true,100));
        mvc.perform(admin(delete("/api/v1/hospedes/"+hLivre))).andExpect(status().isNoContent());
        mvc.perform(admin(delete("/api/v1/quartos/"+qLivre))).andExpect(status().isNoContent());
    }

    @Test
    void validacaoFiltrosOrdenacaoEAtualizacaoQuarto() throws Exception {
        mvc.perform(admin(post("/api/v1/hospedes")).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.erros").isArray());
        long h = criar("hospedes", hospede("FILTRO"));
        long q = criar("quartos", quarto("301",3,true,100));
        LocalDate dia = LocalDate.now().plusDays(10);
        long r = criar("reservas", reserva(h,q,dia,2,2));
        mvc.perform(admin(get("/api/v1/hospedes?documento=OUTRO"))).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(admin(get("/api/v1/hospedes?documento=FILTRO&ordenarPor=nome&direcao=desc")))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(admin(get("/api/v1/quartos?ativo=false"))).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(admin(get("/api/v1/reservas?hospedeId="+h+"&quartoId="+q+"&entrada="+dia+"&status=CONFIRMADA")))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(admin(put("/api/v1/quartos/"+q)).contentType("application/json").content(quarto("301",1,true,150)))
                .andExpect(status().isConflict());
        mvc.perform(admin(put("/api/v1/quartos/"+q)).contentType("application/json").content(quarto("301",3,false,150)))
                .andExpect(status().isConflict());
        mvc.perform(admin(patch("/api/v1/reservas/"+r+"/cancelar"))).andExpect(status().isOk());
        mvc.perform(admin(put("/api/v1/quartos/"+q)).contentType("application/json").content(quarto("301",1,false,150)))
                .andExpect(status().isOk());
        mvc.perform(admin(get("/api/v1/reservas?status=CANCELADA"))).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(admin(get("/api/v1/hospedes?tamanho=101"))).andExpect(status().isBadRequest());
        mvc.perform(admin(get("/api/v1/hospedes?ordenarPor=password"))).andExpect(status().isBadRequest());
        mvc.perform(admin(get("/api/v1/hospedes?pagina=abc"))).andExpect(status().isBadRequest());
        mvc.perform(admin(get("/api/v1/hospedes/9999999"))).andExpect(status().isNotFound());
        mvc.perform(admin(patch("/api/v1/hospedes/"+h))).andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow")).andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void autenticacaoRealEPermissoes() throws Exception {
        mvc.perform(get("/api/v1/hospedes")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        mvc.perform(get("/api/v1/hospedes").header("Authorization","Bearer invalido"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content("{\"username\":\"admin-test\",\"password\":\"errada\"}"))
                .andExpect(status().isUnauthorized());
        var result = mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content("{\"username\":\"admin-test\",\"password\":\"teste-admin-12345\"}"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.tokenType").value("Bearer")).andReturn();
        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
        mvc.perform(get("/api/v1/hospedes").header("Authorization","Bearer "+token)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/hospedes").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CONSULTA")))
                .contentType("application/json").content(hospede("SEM-PERMISSAO")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        mvc.perform(get("/api/v1/pagamentos").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RECEPCAO"))))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/pagamentos/1").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_FINANCEIRO"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/openapi.yaml")).andExpect(status().isOk());
    }

    @Test
    void atualizacaoPreservaPrecoContratadoAposReajuste() throws Exception {
        long h = criar("hospedes", hospede("PRECO-CONTRATADO"));
        long q = criar("quartos", quarto("401", 3, true, 150));
        LocalDate dia = LocalDate.now().plusDays(10);
        long r = criar("reservas", reserva(h, q, dia, 2, 1));
        mvc.perform(admin(put("/api/v1/quartos/"+q)).contentType("application/json").content(quarto("401",3,true,200)))
                .andExpect(status().isOk());
        String novosDados = reserva(h,q,dia,2,2);
        mvc.perform(admin(put("/api/v1/reservas/"+r)).contentType("application/json").content(novosDados))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantidadeHospedes").value(2))
                .andExpect(jsonPath("$.valorTotal").value(300))
                .andExpect(jsonPath("$.valorDiariaContratada").value(150));
        mvc.perform(admin(put("/api/v1/reservas/"+r)).contentType("application/json").content(novosDados))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valorTotal").value(300));
        mvc.perform(admin(put("/api/v1/reservas/"+r)).contentType("application/json").content(reserva(h,q,dia,3,2)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valorTotal").value(450));
        long outro = criar("quartos", quarto("402",3,true,250));
        mvc.perform(admin(put("/api/v1/reservas/"+r)).contentType("application/json").content(reserva(h,outro,dia,3,2)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valorTotal").value(750))
                .andExpect(jsonPath("$.valorDiariaContratada").value(250));
    }

    @Test
    void reservaEmAndamentoMantemEntradaMasAceitaAtualizacao() throws Exception {
        long h = criar("hospedes", hospede("EM-ANDAMENTO"));
        long q = criar("quartos", quarto("501",3,true,200));
        // Fixture passada representa uma reserva criada anteriormente; não passa pelo POST atual.
        LocalDate ontem = LocalDate.now().minusDays(1);
        Reserva entidade = new Reserva();
        entidade.setHospede(hospedes.findById(h).orElseThrow());
        entidade.setQuarto(quartos.findById(q).orElseThrow());
        entidade.setEntrada(ontem);
        entidade.setSaida(ontem.plusDays(2));
        entidade.setQuantidadeHospedes(1);
        entidade.setValorDiariaContratada(new BigDecimal("150.00"));
        entidade.setValorTotal(new BigDecimal("300.00"));
        entidade.setStatus(StatusReserva.CONFIRMADA);
        long r = reservas.saveAndFlush(entidade).getId();
        mvc.perform(admin(put("/api/v1/reservas/"+r)).contentType("application/json").content(reserva(h,q,ontem,2,2)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.entrada").value(ontem.toString()))
                .andExpect(jsonPath("$.valorTotal").value(300));
        mvc.perform(admin(put("/api/v1/reservas/"+r)).contentType("application/json").content(reserva(h,q,ontem,3,2)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valorTotal").value(450));
        mvc.perform(admin(put("/api/v1/reservas/"+r)).contentType("application/json").content(reserva(h,q,LocalDate.now(),2,2)))
                .andExpect(status().isBadRequest());
        long outro = criar("quartos", quarto("502",3,true,200));
        mvc.perform(admin(put("/api/v1/reservas/"+r)).contentType("application/json").content(reserva(h,outro,ontem,3,2)))
                .andExpect(status().isBadRequest());
        mvc.perform(admin(post("/api/v1/reservas")).contentType("application/json").content(reserva(h,outro,ontem,2,2)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros").isArray());
    }

    @Test
    void reservaLegadaRecuperaDiariaPeloTotal() throws Exception {
        long h = criar("hospedes", hospede("LEGADO"));
        long q = criar("quartos", quarto("601",3,true,200));
        LocalDate dia = LocalDate.now().plusDays(10);
        Reserva entidade = new Reserva();
        entidade.setHospede(hospedes.findById(h).orElseThrow());
        entidade.setQuarto(quartos.findById(q).orElseThrow());
        entidade.setEntrada(dia); entidade.setSaida(dia.plusDays(2));
        entidade.setQuantidadeHospedes(1); entidade.setValorTotal(new BigDecimal("300.00"));
        entidade.setStatus(StatusReserva.CONFIRMADA);
        long r = reservas.saveAndFlush(entidade).getId();
        mvc.perform(admin(put("/api/v1/reservas/"+r)).contentType("application/json").content(reserva(h,q,dia,2,2)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valorDiariaContratada").value(150))
                .andExpect(jsonPath("$.valorTotal").value(300));
    }
}

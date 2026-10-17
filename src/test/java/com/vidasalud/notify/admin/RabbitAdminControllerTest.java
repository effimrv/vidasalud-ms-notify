package com.vidasalud.notify.admin;

import com.vidasalud.notify.admin.dto.ColaInfoResponse;
import com.vidasalud.notify.admin.dto.CrearColaRequest;
import com.vidasalud.notify.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Los builders de MockMvc y Spring Security Test no declaran nulabilidad: se silencia ese aviso del IDE. */
@SuppressWarnings("null")
@WebMvcTest(RabbitAdminController.class)
@Import(SecurityConfig.class)
class RabbitAdminControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private RabbitAdminService service;

    @MockBean
    private JwtDecoder jwtDecoder;

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin"));
    }

    @Test
    void creaUnaColaValida() throws Exception {
        when(service.crearCola(any())).thenReturn(new ColaInfoResponse("pruebas.demo", 0, 0));

        mvc.perform(post("/api/rabbit/queues").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"pruebas.demo\",\"ttlMensajesMs\":60000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("pruebas.demo"));
    }

    @Test
    void rechazaUnaColaConNombreVacio() throws Exception {
        mvc.perform(post("/api/rabbit/queues").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.nombre").exists());
        verify(service, never()).crearCola(any(CrearColaRequest.class));
    }

    @Test
    void rechazaNombresReservadosPorRabbit() throws Exception {
        mvc.perform(post("/api/rabbit/queues").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"amq.mia\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaTtlNegativoYDeadLetterIncompleto() throws Exception {
        mvc.perform(post("/api/rabbit/queues").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"pruebas.demo\",\"ttlMensajesMs\":-5,\"routingKeyDeadLetter\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.ttlMensajesMs").exists())
                .andExpect(jsonPath("$.detalles.deadLetterConsistente").exists());
    }

    @Test
    void rechazaUnTipoDeExchangeInvalido() throws Exception {
        mvc.perform(post("/api/rabbit/exchanges").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"pruebas.ex\",\"tipo\":\"CUALQUIERA\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eliminaUnaCola() throws Exception {
        mvc.perform(delete("/api/rabbit/queues/pruebas.demo").with(admin()))
                .andExpect(status().isNoContent());
        verify(service).eliminarCola("pruebas.demo");
    }

    @Test
    void devuelve409AlEliminarUnRecursoProtegido() throws Exception {
        doThrow(new OperacionNoPermitidaException("protegido")).when(service).eliminarCola("vidasalud.tickets");

        mvc.perform(delete("/api/rabbit/queues/vidasalud.tickets").with(admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("protegido"));
    }

    @Test
    void eliminaUnBindingPorQueryParams() throws Exception {
        mvc.perform(delete("/api/rabbit/bindings").with(admin())
                        .param("exchange", "pruebas.ex").param("cola", "pruebas.demo").param("routingKey", "a.b"))
                .andExpect(status().isNoContent());
    }

    @Test
    void exigeToken() throws Exception {
        mvc.perform(delete("/api/rabbit/queues/pruebas.demo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exigeRolAdmin() throws Exception {
        mvc.perform(delete("/api/rabbit/queues/pruebas.demo")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Paciente"))))
                .andExpect(status().isForbidden());
    }
}

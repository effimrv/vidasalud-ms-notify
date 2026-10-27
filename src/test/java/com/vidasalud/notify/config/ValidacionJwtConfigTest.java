package com.vidasalud.notify.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** El filtro JWT acepta solo tokens vigentes, de nuestro tenant y dirigidos a la API de VidaSalud. */
class ValidacionJwtConfigTest {

    private static final String EMISOR = "https://login.microsoftonline.com/tenant-vidasalud/v2.0";
    private static final String API = "api://vidasalud";

    private final OAuth2TokenValidator<Jwt> validador =
            ValidacionJwtConfig.validador(List.of(EMISOR), List.of(API));

    @Test
    void aceptaUnTokenDelTenantParaLaApi() {
        assertThat(validador.validate(token(EMISOR, API, Instant.now().plusSeconds(600))).hasErrors()).isFalse();
    }

    @Test
    void rechazaUnTokenDeOtroTenant() {
        assertThat(validador.validate(token("https://login.microsoftonline.com/otro/v2.0", API,
                Instant.now().plusSeconds(600))).hasErrors()).isTrue();
    }

    @Test
    void rechazaUnTokenParaOtraAplicacion() {
        assertThat(validador.validate(token(EMISOR, "api://otra-app", Instant.now().plusSeconds(600))).hasErrors())
                .isTrue();
    }

    @Test
    void rechazaUnTokenVencido() {
        assertThat(validador.validate(token(EMISOR, API, Instant.now().minusSeconds(600))).hasErrors()).isTrue();
    }

    private static Jwt token(String emisor, String audiencia, Instant expira) {
        return Jwt.withTokenValue("t").header("alg", "RS256")
                .issuer(emisor).audience(List.of(audiencia))
                .issuedAt(expira.minusSeconds(3600)).expiresAt(expira)
                .build();
    }
}

package com.arnaldsouza.libraryapi.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET =
            "5468697349734150726f6a656374536563726574466f724c6962726172794170694a5754";
    private static final long ONE_HOUR = 3_600_000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, ONE_HOUR);
    }

    @Test
    @DisplayName("generated token should carry the username as subject")
    void tokenShouldCarryUsername() {
        String token = jwtService.generateToken("arnald", "USER");

        assertThat(jwtService.extractUsername(token)).isEqualTo("arnald");
    }

    @Test
    @DisplayName("generated token should carry the role claim")
    void tokenShouldCarryRole() {
        String token = jwtService.generateToken("admin", "ADMIN");

        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("a freshly generated token should be valid")
    void freshTokenShouldBeValid() {
        String token = jwtService.generateToken("arnald", "USER");

        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("a tampered token should be rejected")
    void tamperedTokenShouldBeRejected() {
        String token = jwtService.generateToken("arnald", "USER");

        assertThat(jwtService.isTokenValid(token + "x")).isFalse();
    }

    @Test
    @DisplayName("a token signed with another secret should be rejected")
    void tokenFromAnotherSecretShouldBeRejected() {
        String otherSecret =
                "4e6f7441757468656e7469634b65794e6f7441757468656e7469634b65794142434445";
        JwtService otherService = new JwtService(otherSecret, ONE_HOUR);
        String foreignToken = otherService.generateToken("attacker", "ADMIN");

        assertThat(jwtService.isTokenValid(foreignToken)).isFalse();
    }

    @Test
    @DisplayName("an expired token should be rejected")
    void expiredTokenShouldBeRejected() {
        JwtService shortLived = new JwtService(SECRET, -1_000L);
        String token = shortLived.generateToken("arnald", "USER");

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    @DisplayName("garbage input should be rejected without throwing")
    void garbageShouldBeRejected() {
        assertThat(jwtService.isTokenValid("not.a.token")).isFalse();
        assertThat(jwtService.isTokenValid("")).isFalse();
    }
}
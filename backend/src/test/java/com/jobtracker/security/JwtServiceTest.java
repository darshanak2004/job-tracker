package com.jobtracker.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jobtracker.entity.Role;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-1234567890";
    private final AuthUser user = new AuthUser(1L, "ann@test.com", "hash", Role.RECRUITER);

    @Test
    void generatedToken_isValid_andContainsEmail() {
        JwtService jwt = new JwtService(SECRET, 60_000);

        String token = jwt.generateToken(user);

        assertThat(jwt.isValid(token)).isTrue();
        assertThat(jwt.extractUsername(token)).isEqualTo("ann@test.com");
    }

    @Test
    void tamperedToken_isInvalid() {
        JwtService jwt = new JwtService(SECRET, 60_000);
        String token = jwt.generateToken(user);

        assertThat(jwt.isValid(token + "x")).isFalse();
    }

    @Test
    void tokenSignedWithAnotherSecret_isInvalid() {
        String token = new JwtService(SECRET, 60_000).generateToken(user);
        JwtService other = new JwtService("a-completely-different-secret-key-0987654321", 60_000);

        assertThat(other.isValid(token)).isFalse();
    }

    @Test
    void expiredToken_isInvalid() {
        JwtService jwt = new JwtService(SECRET, -1_000);
        String token = jwt.generateToken(user);

        assertThat(jwt.isValid(token)).isFalse();
        assertThatThrownBy(() -> jwt.extractUsername(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void garbage_isInvalid() {
        JwtService jwt = new JwtService(SECRET, 60_000);

        assertThat(jwt.isValid("not-a-jwt")).isFalse();
        assertThat(jwt.isValid("")).isFalse();
    }

    @Test
    void authUser_exposesRolePrefixedAuthority() {
        assertThat(user.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_RECRUITER");
        assertThat(user.getUsername()).isEqualTo("ann@test.com");
        assertThat(user.getId()).isEqualTo(1L);
    }
}

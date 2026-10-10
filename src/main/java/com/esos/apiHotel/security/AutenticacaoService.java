package com.esos.apiHotel.security;

import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import com.esos.apiHotel.dto.request.LoginRequest;
import com.esos.apiHotel.dto.response.LoginResponse;

@Service
public class AutenticacaoService {
    private final UserDetailsService users;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final String issuer;
    private final long lifetime;
    private final String dummyHash;

    public AutenticacaoService(UserDetailsService users, PasswordEncoder passwords, JwtEncoder encoder,
            @Value("${hotel.security.issuer}") String issuer,
            @Value("${hotel.security.token-seconds}") long lifetime) {
        if (lifetime < 60 || lifetime > 3600) throw new IllegalStateException("Validade do JWT deve ficar entre 60 e 3600 segundos.");
        this.users = users;
        this.passwords = passwords;
        this.encoder = encoder;
        this.issuer = issuer;
        this.lifetime = lifetime;
        this.dummyHash = passwords.encode(UUID.randomUUID().toString());
    }

    public LoginResponse login(LoginRequest request) {
        UserDetails user;
        try {
            user = users.loadUserByUsername(request.username());
        } catch (UsernameNotFoundException exception) {
            passwords.matches(request.password(), dummyHash);
            throw new BadCredentialsException("Credenciais inválidas.");
        }
        if (!passwords.matches(request.password(), user.getPassword()) || !user.isEnabled()) {
            throw new BadCredentialsException("Credenciais inválidas.");
        }
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer).subject(user.getUsername()).issuedAt(now).notBefore(now)
                .expiresAt(now.plusSeconds(lifetime)).id(UUID.randomUUID().toString())
                .claim("roles", user.getAuthorities().stream()
                        .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", "")).toList())
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new LoginResponse(token, "Bearer", lifetime);
    }
}

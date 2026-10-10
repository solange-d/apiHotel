package com.esos.apiHotel.config;


import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import com.esos.apiHotel.security.RespostaSeguranca;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    UserDetailsService userDetailsService(Environment env, PasswordEncoder encoder) {
        List<UserDetails> users = new ArrayList<>();
        addUser(users, env, encoder, "admin", "ADMIN", true);
        addUser(users, env, encoder, "recepcao", "RECEPCAO", false);
        addUser(users, env, encoder, "financeiro", "FINANCEIRO", false);
        addUser(users, env, encoder, "consulta", "CONSULTA", false);
        return new InMemoryUserDetailsManager(users);
    }

    private void addUser(List<UserDetails> users, Environment env, PasswordEncoder encoder,
            String key, String role, boolean mandatory) {
        String username = env.getProperty("hotel.security." + key + "-user", "");
        String password = env.getProperty("hotel.security." + key + "-password", "");
        if (!mandatory && username.isBlank() && password.isBlank())
            return;
        int bytes = password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        System.out.printf(
                "Configuração do perfil %s: usuário vazio=%s, senha carregada=%d bytes%n",
                key, username.isBlank(), bytes);
        if (username.isBlank() || bytes < 12 || bytes > 72) {
            throw new IllegalStateException("Configure usuário e senha de 12 a 72 bytes para o perfil " + key + ".");
        }
        users.add(User.withUsername(username).password(encoder.encode(password)).roles(role).build());
    }

    @Bean
    SecretKey jwtKey(@Value("${hotel.security.jwt-secret}") String secret) {
        byte[] decoded = Base64.getDecoder().decode(secret);
        if (decoded.length < 32)
            throw new IllegalStateException("JWT_SECRET deve conter pelo menos 32 bytes em Base64.");
        return new SecretKeySpec(decoded, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key, @Value("${hotel.security.issuer}") String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);

        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/openapi.yaml", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/pagamentos", "/api/v1/pagamentos/**")
                        .hasAnyRole("ADMIN", "FINANCEIRO")
                        .requestMatchers(HttpMethod.GET, "/api/v1/**")
                        .hasAnyRole("ADMIN", "RECEPCAO", "FINANCEIRO", "CONSULTA")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/pagamentos", "/api/v1/pagamentos/**")
                        .hasAnyRole("ADMIN", "FINANCEIRO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/quartos").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/quartos/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/hospedes", "/api/v1/hospedes/**", "/api/v1/reservas",
                                "/api/v1/reservas/**")
                        .hasAnyRole("ADMIN", "RECEPCAO")
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, error) -> RespostaSeguranca.enviar(response, 401))
                        .accessDeniedHandler((request, response, error) -> RespostaSeguranca.enviar(response, 403)))
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint((request, response, error) -> RespostaSeguranca.enviar(response, 401))
                        .accessDeniedHandler((request, response, error) -> RespostaSeguranca.enviar(response, 403)));
        return http.build();
    }
}

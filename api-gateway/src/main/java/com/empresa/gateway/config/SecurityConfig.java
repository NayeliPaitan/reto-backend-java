package com.empresa.gateway.config;

import com.empresa.gateway.error.JsonAccessDeniedHandler;
import com.empresa.gateway.error.JsonAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            JsonAuthenticationEntryPoint entryPoint,
            JsonAccessDeniedHandler accessDeniedHandler) {

        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/**", "/v3/api-docs/**", "/docs/**", "/swagger-ui.html",
                        "/swagger-ui/**", "/webjars/**").permitAll()
                .anyExchange().authenticated()
            )
            // Errores 401/403 en el formato { timestamp, status, code, message, traceId }
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .oauth2ResourceServer(oauth2 -> oauth2
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler(accessDeniedHandler)
                .jwt(jwt -> {}));

        return http.build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder(
            @Value("${app.security.jwk-set-uri}") String jwkSetUri,
            @Value("${app.security.issuer}") String issuer) {

        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }
}

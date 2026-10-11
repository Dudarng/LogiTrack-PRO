package com.logitrack.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // TODO: Configuração temporária liberando todas as rotas (permitAll) para a Fase 2,
    // mantendo o CSRF ativo. Será substituída na Fase 4 pela autenticação completa e controle de acesso.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            .addFilterAfter(new CsrfEagerResolutionFilter(), CsrfFilter.class);

        return http.build();
    }

    /**
     * Filtro para resolução antecipada do token CSRF antes da renderização do template,
     * garantindo que a sessão e os cabeçalhos HTTP sejam preparados antes do commit do buffer do Tomcat.
     */
    private static class CsrfEagerResolutionFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }
}

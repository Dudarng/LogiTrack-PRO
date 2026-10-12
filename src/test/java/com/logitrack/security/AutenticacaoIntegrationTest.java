package com.logitrack.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AutenticacaoIntegrationTest {

    static {
        System.setProperty("api.version", "1.44");
        System.setProperty("docker.api.version", "1.44");
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            if (System.getenv("DOCKER_HOST") == null) {
                System.setProperty("DOCKER_HOST", "npipe:////./pipe/docker_engine_linux");
            }
        }
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("logitrack_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // =========================================================================
    // 1. VALIDAÇÃO DO HASH BCrypt DA MIGRATION V3
    // =========================================================================

    @Test
    @DisplayName("Deve validar que o hash lido do banco para o usuário admin confere com a senha admin123")
    void deveValidarHashDaMigrationV3ComBCryptPasswordEncoder() {
        Usuario admin = usuarioRepository.findByUsername("admin")
                .orElseThrow(() -> new AssertionError("Usuário admin não encontrado após execução das migrations"));

        assertThat(admin.getRole()).isEqualTo("ADMIN");
        assertThat(admin.getPasswordHash()).isNotBlank();
        assertThat(passwordEncoder.matches("admin123", admin.getPasswordHash())).isTrue();
    }

    // =========================================================================
    // 2. CONTROLE DE ACESSO E REDIRECIONAMENTOS
    // =========================================================================

    @Test
    @DisplayName("Rota protegida /dashboard deve redirecionar para /login quando não autenticado")
    void deveRedirecionarParaLoginAoAcessarDashboardSemAutenticacao() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Rota protegida /manutencoes deve redirecionar para /login quando não autenticado")
    void deveRedirecionarParaLoginAoAcessarManutencoesSemAutenticacao() throws Exception {
        mockMvc.perform(get("/manutencoes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Rota /login deve ser pública e responder com status 200 OK")
    void devePermitirAcessoPublicoAPaginaDeLogin() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Página de login pública deve conter data-theme-toggle e não conter bootstrap")
    void deveConterDataThemeToggleENaoConterBootstrapNaPaginaDeLogin() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String html = result.getResponse().getContentAsString().toLowerCase();
                    assertThat(html).contains("data-theme-toggle");
                    assertThat(html).doesNotContain("bootstrap");
                });
    }

    @Test
    @DisplayName("Recursos estáticos essenciais (/fonts/**, /css/**, /js/**) devem ser acessíveis sem autenticação com status 200")
    void devePermitirAcessoPublicoAosRecursosEstaticos() throws Exception {
        mockMvc.perform(get("/fonts/poppins-400.woff2"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/css/logitrack.css"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/js/theme.js"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Com usuário autenticado (@WithMockUser), /dashboard responde 200 OK")
    void devePermitirAcessoAoDashboardComUsuarioAutenticado() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Com usuário autenticado (@WithMockUser), /manutencoes responde 200 OK")
    void devePermitirAcessoAManutencoesComUsuarioAutenticado() throws Exception {
        mockMvc.perform(get("/manutencoes"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // 3. PROTEÇÃO CSRF
    // =========================================================================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("POST sem token CSRF deve ser rejeitado com status 403 Forbidden")
    void deveRejeitarPostSemTokenCsrfComStatus403() throws Exception {
        mockMvc.perform(post("/manutencoes"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("POST com token CSRF válido deve ser aceito (status diferente de 403 Forbidden)")
    void deveAceitarPostComTokenCsrf() throws Exception {
        MvcResult result = mockMvc.perform(post("/manutencoes").with(csrf()))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isNotEqualTo(403);
    }

    // =========================================================================
    // 4. LOGOUT SEGURO
    // =========================================================================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Logout via POST com token CSRF deve redirecionar para /login?logout")
    void deveRealizarLogoutViaPostComCsrfERedirecionar() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    // =========================================================================
    // 5. FLUXO REAL DE LOGIN COM FORM LOGIN
    // =========================================================================

    @Test
    @DisplayName("Login real com admin e admin123 deve autenticar com sucesso e redirecionar para /dashboard")
    void deveAutenticarComSucessoComCredenciaisAdmin() throws Exception {
        mockMvc.perform(formLogin("/login").user("admin").password("admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(authenticated().withUsername("admin"));
    }

    @Test
    @DisplayName("Login com senha incorreta deve falhar e redirecionar para /login?error")
    void deveFalharAutenticacaoComSenhaIncorreta() throws Exception {
        mockMvc.perform(formLogin("/login").user("admin").password("senha_errada"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }
}

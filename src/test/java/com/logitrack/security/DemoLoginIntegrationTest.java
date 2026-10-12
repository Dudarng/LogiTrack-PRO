package com.logitrack.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
    "logitrack.demo.enabled=true",
    "logitrack.demo.username=admin",
    "logitrack.demo.password=admin123"
})
class DemoLoginIntegrationTest {

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
    private DemoLoginProperties demoLoginProperties;

    @Test
    @DisplayName("Deve carregar as propriedades de login demo corretamente via @ConfigurationProperties")
    void deveCarregarPropriedadesDemoLogin() {
        assertThat(demoLoginProperties.enabled()).isTrue();
        assertThat(demoLoginProperties.username()).isEqualTo("admin");
        assertThat(demoLoginProperties.password()).isEqualTo("admin123");
    }

    @Test
    @DisplayName("Com logitrack.demo.enabled=true, GET /login deve conter o botão, textos e atributos de demonstração")
    void deveExibirBotaoECredenciaisDemoQuandoHabilitado() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String html = result.getResponse().getContentAsString();
                    assertThat(html).contains("Usar credenciais de demonstração");
                    assertThat(html).contains("Ambiente de demonstração");
                    assertThat(html).contains("btnDemoLogin");
                    assertThat(html).contains("data-user=\"admin\"");
                    assertThat(html).contains("data-pass=\"admin123\"");
                    assertThat(html).contains("/js/login-demo.js");
                });
    }
}

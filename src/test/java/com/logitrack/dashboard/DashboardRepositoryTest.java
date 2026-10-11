package com.logitrack.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.logitrack.dashboard.projection.CronogramaManutencaoProjection;
import com.logitrack.dashboard.projection.RankingUtilizacaoProjection;
import com.logitrack.dashboard.projection.VolumeCategoriaProjection;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DashboardRepositoryTest {

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
    private DashboardRepository dashboardRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Limpa todas as tabelas e reinicia sequências antes de cada teste para garantir isolamento e determinismo
        jdbcTemplate.execute("TRUNCATE TABLE viagens, manutencoes, veiculos RESTART IDENTITY CASCADE");
    }

    private void inserirVeiculo(int id, String placa, String modelo, String tipo, int ano) {
        jdbcTemplate.update(
                "INSERT INTO veiculos (id, placa, modelo, tipo, ano) VALUES (?, ?, ?, ?, ?)",
                id, placa, modelo, tipo, ano
        );
    }

    // =========================================================================
    // MÉTRICA 1: TOTAL DE KM PERCORRIDO
    // =========================================================================

    @Test
    @DisplayName("Métrica 1: Deve calcular total de KM de toda a frota desconsiderando viagens em andamento")
    void deveCalcularTotalKmSemFiltroExcluindoViagensEmAndamento() {
        inserirVeiculo(1, "ABC-1234", "Fiorino", "LEVE", 2022);
        inserirVeiculo(2, "XYZ-9876", "Volvo FH", "PESADO", 2021);

        // Viagens concluídas
        jdbcTemplate.update("INSERT INTO viagens (veiculo_id, data_saida, data_chegada, km_percorrida) " +
                "VALUES (1, NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day', 150.50)");
        jdbcTemplate.update("INSERT INTO viagens (veiculo_id, data_saida, data_chegada, km_percorrida) " +
                "VALUES (2, NOW() - INTERVAL '4 days', NOW() - INTERVAL '3 days', 300.25)");

        // Viagem em andamento (data_chegada IS NULL) -> deve ser excluída
        jdbcTemplate.update("INSERT INTO viagens (veiculo_id, data_saida, data_chegada, km_percorrida) " +
                "VALUES (1, NOW() - INTERVAL '5 hours', NULL, 800.00)");

        BigDecimal totalKm = dashboardRepository.findTotalKm(null);

        assertThat(totalKm).isEqualByComparingTo(new BigDecimal("450.75"));
    }

    @Test
    @DisplayName("Métrica 1: Deve filtrar total de KM por veículo específico e retornar 0 para veículo sem viagens")
    void deveFiltrarTotalKmPorVeiculoERetornarZeroSeSemViagens() {
        inserirVeiculo(1, "ABC-1234", "Fiorino", "LEVE", 2022);
        inserirVeiculo(2, "XYZ-9876", "Volvo FH", "PESADO", 2021);
        inserirVeiculo(3, "KJG-1122", "Mercedes Sprinter", "LEVE", 2020);

        jdbcTemplate.update("INSERT INTO viagens (veiculo_id, data_saida, data_chegada, km_percorrida) " +
                "VALUES (1, NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day', 220.00)");
        jdbcTemplate.update("INSERT INTO viagens (veiculo_id, data_saida, data_chegada, km_percorrida) " +
                "VALUES (2, NOW() - INTERVAL '3 days', NOW() - INTERVAL '2 days', 500.00)");

        // Filtro veículo 1
        BigDecimal kmVeiculo1 = dashboardRepository.findTotalKm(1);
        assertThat(kmVeiculo1).isEqualByComparingTo(new BigDecimal("220.00"));

        // Filtro veículo 2
        BigDecimal kmVeiculo2 = dashboardRepository.findTotalKm(2);
        assertThat(kmVeiculo2).isEqualByComparingTo(new BigDecimal("500.00"));

        // Veículo 3 cadastrado mas sem viagens realizadas
        BigDecimal kmVeiculo3 = dashboardRepository.findTotalKm(3);
        assertThat(kmVeiculo3).isEqualByComparingTo(BigDecimal.ZERO);

        // ID inexistente
        BigDecimal kmInexistente = dashboardRepository.findTotalKm(999);
        assertThat(kmInexistente).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // =========================================================================
    // MÉTRICA 2: VOLUME POR CATEGORIA (LEVE VS PESADO)
    // =========================================================================

    @Test
    @DisplayName("Métrica 2: Deve calcular volume e percentual por categoria sem divisão por zero e com getters funcionais")
    void deveCalcularVolumePorCategoriaComViagensRealizadas() {
        inserirVeiculo(1, "ABC-1234", "Fiorino", "LEVE", 2022);
        inserirVeiculo(2, "XYZ-9876", "Volvo FH", "PESADO", 2021);

        // 3 viagens para LEVE
        jdbcTemplate.update("INSERT INTO viagens (veiculo_id, data_saida, data_chegada, km_percorrida) VALUES (1, NOW() - INTERVAL '3 days', NOW() - INTERVAL '2 days', 100.00)");
        jdbcTemplate.update("INSERT INTO viagens (veiculo_id, data_saida, data_chegada, km_percorrida) VALUES (1, NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day', 120.00)");
        jdbcTemplate.update("INSERT INTO viagens (veiculo_id, data_saida, data_chegada, km_percorrida) VALUES (1, NOW() - INTERVAL '1 day', NOW() - INTERVAL '12 hours', 80.00)");

        // 1 viagem para PESADO + 1 viagem em andamento (não conta)
        jdbcTemplate.update("INSERT INTO viagens (veiculo_id, data_saida, data_chegada, km_percorrida) VALUES (2, NOW() - INTERVAL '4 days', NOW() - INTERVAL '3 days', 400.00)");
        jdbcTemplate.update("INSERT INTO viagens (veiculo_id, data_saida, data_chegada, km_percorrida) VALUES (2, NOW() - INTERVAL '1 day', NULL, 500.00)");

        List<VolumeCategoriaProjection> volume = dashboardRepository.findVolumePorCategoria();

        assertThat(volume).hasSize(2);

        VolumeCategoriaProjection leve = volume.stream().filter(v -> "LEVE".equals(v.getTipo())).findFirst().orElseThrow();
        assertThat(leve.getTipo()).isEqualTo("LEVE");
        assertThat(leve.getTotalViagens()).isEqualTo(3L);
        assertThat(leve.getTotalGeral()).isEqualTo(4L);
        assertThat(leve.getPercentual()).isEqualByComparingTo(new BigDecimal("75.0"));

        VolumeCategoriaProjection pesado = volume.stream().filter(v -> "PESADO".equals(v.getTipo())).findFirst().orElseThrow();
        assertThat(pesado.getTipo()).isEqualTo("PESADO");
        assertThat(pesado.getTotalViagens()).isEqualTo(1L);
        assertThat(pesado.getTotalGeral()).isEqualTo(4L);
        assertThat(pesado.getPercentual()).isEqualByComparingTo(new BigDecimal("25.0"));
    }

    @Test
    @DisplayName("Métrica 2 (Caso de Borda): Deve retornar LEVE e PESADO com zero viagens e percentual 0 sem erro de divisão por zero")
    void deveRetornarCategoriasComZeroViagensSemErroDeDivisaoPorZero() {
        // Banco sem nenhuma viagem (com ou sem veículos cadastrados)
        List<VolumeCategoriaProjection> volume = dashboardRepository.findVolumePorCategoria();

        assertThat(volume).hasSize(2);

        VolumeCategoriaProjection leve = volume.stream().filter(v -> "LEVE".equals(v.getTipo())).findFirst().orElseThrow();
        assertThat(leve.getTipo()).isEqualTo("LEVE");
        assertThat(leve.getTotalViagens()).isEqualTo(0L);
        assertThat(leve.getTotalGeral()).isEqualTo(0L);
        assertThat(leve.getPercentual()).isEqualByComparingTo(BigDecimal.ZERO);

        VolumeCategoriaProjection pesado = volume.stream().filter(v -> "PESADO".equals(v.getTipo())).findFirst().orElseThrow();
        assertThat(pesado.getTipo()).isEqualTo("PESADO");
        assertThat(pesado.getTotalViagens()).isEqualTo(0L);
        assertThat(pesado.getTotalGeral()).isEqualTo(0L);
        assertThat(pesado.getPercentual()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // =========================================================================
    // MÉTRICA 3: CRONOGRAMA DAS PRÓXIMAS 5 MANUTENÇÕES
    // =========================================================================

    @Test
    @DisplayName("Métrica 3: Deve listar próximas 5 manutenções ordenadas por data e desempate por id, excluindo CONCLUIDA")
    void deveListarProximasCincoManutencoesCorretamente() {
        inserirVeiculo(1, "ABC-1234", "Fiorino", "LEVE", 2022);
        inserirVeiculo(2, "XYZ-9876", "Volvo FH", "PESADO", 2021);

        // m1: hoje (CURRENT_DATE) -> deve entrar!
        jdbcTemplate.update("INSERT INTO manutencoes (id, veiculo_id, data_inicio, data_finalizacao, tipo_servico, custo_estimado, status) " +
                "VALUES (101, 1, CURRENT_DATE, CURRENT_DATE + INTERVAL '1 day', 'Serviço 1', 100.00, 'PENDENTE')");

        // m2: amanhã -> deve entrar!
        jdbcTemplate.update("INSERT INTO manutencoes (id, veiculo_id, data_inicio, data_finalizacao, tipo_servico, custo_estimado, status) " +
                "VALUES (102, 2, CURRENT_DATE + INTERVAL '1 day', CURRENT_DATE + INTERVAL '2 days', 'Serviço 2', 200.00, 'EM_REALIZACAO')");

        // m3: futuro porém CONCLUIDA -> deve ser EXCLUÍDA!
        jdbcTemplate.update("INSERT INTO manutencoes (id, veiculo_id, data_inicio, data_finalizacao, tipo_servico, custo_estimado, status) " +
                "VALUES (103, 1, CURRENT_DATE + INTERVAL '2 days', CURRENT_DATE + INTERVAL '3 days', 'Serviço Concluído', 300.00, 'CONCLUIDA')");

        // m4: passado (CURRENT_DATE - 1 day) -> deve ser EXCLUÍDA!
        jdbcTemplate.update("INSERT INTO manutencoes (id, veiculo_id, data_inicio, data_finalizacao, tipo_servico, custo_estimado, status) " +
                "VALUES (104, 2, CURRENT_DATE - INTERVAL '1 day', CURRENT_DATE, 'Serviço Passado', 400.00, 'PENDENTE')");

        // m5, m6, m7: futuros ativos
        jdbcTemplate.update("INSERT INTO manutencoes (id, veiculo_id, data_inicio, data_finalizacao, tipo_servico, custo_estimado, status) " +
                "VALUES (105, 1, CURRENT_DATE + INTERVAL '3 days', NULL, 'Serviço 5', 500.00, 'PENDENTE')");
        jdbcTemplate.update("INSERT INTO manutencoes (id, veiculo_id, data_inicio, data_finalizacao, tipo_servico, custo_estimado, status) " +
                "VALUES (106, 2, CURRENT_DATE + INTERVAL '4 days', NULL, 'Serviço 6', 600.00, 'PENDENTE')");
        jdbcTemplate.update("INSERT INTO manutencoes (id, veiculo_id, data_inicio, data_finalizacao, tipo_servico, custo_estimado, status) " +
                "VALUES (107, 1, CURRENT_DATE + INTERVAL '5 days', NULL, 'Serviço 7', 700.00, 'EM_REALIZACAO')");

        // m8: 6º elemento futuro ativo -> deve ser cortado pelo LIMIT 5
        jdbcTemplate.update("INSERT INTO manutencoes (id, veiculo_id, data_inicio, data_finalizacao, tipo_servico, custo_estimado, status) " +
                "VALUES (108, 2, CURRENT_DATE + INTERVAL '6 days', NULL, 'Serviço 8', 800.00, 'PENDENTE')");

        List<CronogramaManutencaoProjection> cronograma = dashboardRepository.findCronogramaManutencoes();

        assertThat(cronograma).hasSize(5);

        // Verifica os IDs na ordem correta
        assertThat(cronograma).extracting(CronogramaManutencaoProjection::getId)
                .containsExactly(101, 102, 105, 106, 107);

        // Verifica o mapeamento de propriedades e aliases (sem null)
        CronogramaManutencaoProjection primeiro = cronograma.get(0);
        assertThat(primeiro.getId()).isEqualTo(101);
        assertThat(primeiro.getDataInicio()).isNotNull();
        assertThat(primeiro.getDataFinalizacao()).isNotNull();
        assertThat(primeiro.getTipoServico()).isEqualTo("Serviço 1");
        assertThat(primeiro.getCustoEstimado()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(primeiro.getStatus()).isEqualTo("PENDENTE");
        assertThat(primeiro.getVeiculoId()).isEqualTo(1);
        assertThat(primeiro.getVeiculoPlaca()).isEqualTo("ABC-1234");
        assertThat(primeiro.getVeiculoModelo()).isEqualTo("Fiorino");
    }

    @Test
    @DisplayName("Métrica 3 (Caso de Borda): Manutenção com data_inicio = CURRENT_DATE entra no cronograma")
    void deveIncluirManutencaoComDataInicioIgualCurrentDate() {
        inserirVeiculo(1, "ABC-1234", "Fiorino", "LEVE", 2022);

        // Manutenção exatamente em CURRENT_DATE
        jdbcTemplate.update("INSERT INTO manutencoes (id, veiculo_id, data_inicio, tipo_servico, custo_estimado, status) " +
                "VALUES (1, 1, CURRENT_DATE, 'Revisão do Dia', 250.00, 'PENDENTE')");

        // Manutenção de ontem
        jdbcTemplate.update("INSERT INTO manutencoes (id, veiculo_id, data_inicio, tipo_servico, custo_estimado, status) " +
                "VALUES (2, 1, CURRENT_DATE - INTERVAL '1 day', 'Ontem', 300.00, 'PENDENTE')");

        List<CronogramaManutencaoProjection> cronograma = dashboardRepository.findCronogramaManutencoes();

        assertThat(cronograma).hasSize(1);
        assertThat(cronograma.get(0).getId()).isEqualTo(1);
        assertThat(cronograma.get(0).getTipoServico()).isEqualTo("Revisão do Dia");
    }

    // =========================================================================
    // MÉTRICA 4: RANKING DE UTILIZAÇÃO DA FROTA
    // =========================================================================

    @Test
    @DisplayName("Métrica 4: Deve identificar veículo líder em KM com projeção mapeada corretamente")
    void deveIdentificarVeiculoLiderEmKm() {
        inserirVeiculo(1, "ABC-1234", "Fiorino", "LEVE", 2022);
        inserirVeiculo(2, "XYZ-9876", "Volvo FH", "PESADO", 2021);
        inserirVeiculo(3, "KJG-1122", "Mercedes Sprinter", "LEVE", 2020);

        // Veículo 1: 300 km
        jdbcTemplate.update("INSERT INTO viagens (veiculo_id, data_saida, data_chegada, km_percorrida) VALUES (1, NOW() - INTERVAL '3 days', NOW() - INTERVAL '2 days', 300.00)");
        // Veículo 2: 850 km concluídos + 500 km em andamento (não entra)
        jdbcTemplate.update("INSERT INTO viagens (veiculo_id, data_saida, data_chegada, km_percorrida) VALUES (2, NOW() - INTERVAL '4 days', NOW() - INTERVAL '3 days', 850.00)");
        jdbcTemplate.update("INSERT INTO viagens (veiculo_id, data_saida, data_chegada, km_percorrida) VALUES (2, NOW() - INTERVAL '1 day', NULL, 500.00)");
        // Veículo 3: 400 km
        jdbcTemplate.update("INSERT INTO viagens (veiculo_id, data_saida, data_chegada, km_percorrida) VALUES (3, NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day', 400.00)");

        Optional<RankingUtilizacaoProjection> liderOpt = dashboardRepository.findRankingUtilizacao();

        assertThat(liderOpt).isPresent();
        RankingUtilizacaoProjection lider = liderOpt.get();
        assertThat(lider.getVeiculoId()).isEqualTo(2);
        assertThat(lider.getPlaca()).isEqualTo("XYZ-9876");
        assertThat(lider.getModelo()).isEqualTo("Volvo FH");
        assertThat(lider.getTipo()).isEqualTo("PESADO");
        assertThat(lider.getTotalKm()).isEqualByComparingTo(new BigDecimal("850.00"));
    }

    @Test
    @DisplayName("Métrica 4 (Caso de Borda): Deve retornar Optional.empty() quando não houver viagens realizadas")
    void deveRetornarOptionalVazioQuandoNaoHouverViagensRealizadas() {
        inserirVeiculo(1, "ABC-1234", "Fiorino", "LEVE", 2022);

        // Apenas viagem em andamento
        jdbcTemplate.update("INSERT INTO viagens (veiculo_id, data_saida, data_chegada, km_percorrida) " +
                "VALUES (1, NOW() - INTERVAL '1 hour', NULL, 150.00)");

        Optional<RankingUtilizacaoProjection> liderOpt = dashboardRepository.findRankingUtilizacao();

        assertThat(liderOpt).isEmpty();
    }

    @Test
    @DisplayName("Métrica 4 (Desempate): Deve desempatar pelo veiculo_id ascendente em caso de KM igual")
    void deveDesempatarPorVeiculoIdAscendente() {
        inserirVeiculo(1, "ABC-1234", "Fiorino", "LEVE", 2022);
        inserirVeiculo(2, "XYZ-9876", "Volvo FH", "PESADO", 2021);

        // Ambos com 500 km
        jdbcTemplate.update("INSERT INTO viagens (veiculo_id, data_saida, data_chegada, km_percorrida) VALUES (1, NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day', 500.00)");
        jdbcTemplate.update("INSERT INTO viagens (veiculo_id, data_saida, data_chegada, km_percorrida) VALUES (2, NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day', 500.00)");

        Optional<RankingUtilizacaoProjection> liderOpt = dashboardRepository.findRankingUtilizacao();

        assertThat(liderOpt).isPresent();
        assertThat(liderOpt.get().getVeiculoId()).isEqualTo(1);
    }

    // =========================================================================
    // MÉTRICA 5: PROJEÇÃO FINANCEIRA DO MÊS ATUAL
    // =========================================================================

    @Test
    @DisplayName("Métrica 5 (Casos de Borda): Dia 1 do mês atual ENTRA; Dia 1 do mês seguinte NÃO ENTRA; Mês anterior NÃO ENTRA")
    void deveValidarLimitesDeDataDaProjecaoFinanceira() {
        inserirVeiculo(1, "ABC-1234", "Fiorino", "LEVE", 2022);

        // Borda Inferior: Exatamente no dia 1 do mês atual -> DEVE ENTRAR (R$ 1.000,00)
        jdbcTemplate.update("INSERT INTO manutencoes (id, veiculo_id, data_inicio, tipo_servico, custo_estimado, status) " +
                "VALUES (1, 1, DATE_TRUNC('month', CURRENT_DATE)::DATE, 'Revisão Início do Mês', 1000.00, 'PENDENTE')");

        // Dia intermediário do mês atual -> DEVE ENTRAR (R$ 450.50)
        jdbcTemplate.update("INSERT INTO manutencoes (id, veiculo_id, data_inicio, tipo_servico, custo_estimado, status) " +
                "VALUES (2, 1, DATE_TRUNC('month', CURRENT_DATE)::DATE + INTERVAL '10 days', 'Revisão Meio do Mês', 450.50, 'EM_REALIZACAO')");

        // Borda Superior: Exatamente no dia 1 do mês seguinte -> NÃO DEVE ENTRAR (R$ 2.500,00)
        jdbcTemplate.update("INSERT INTO manutencoes (id, veiculo_id, data_inicio, tipo_servico, custo_estimado, status) " +
                "VALUES (3, 1, (DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month')::DATE, 'Revisão Próximo Mês', 2500.00, 'PENDENTE')");

        // Mês anterior (último dia do mês anterior) -> NÃO DEVE ENTRAR (R$ 3.000,00)
        jdbcTemplate.update("INSERT INTO manutencoes (id, veiculo_id, data_inicio, tipo_servico, custo_estimado, status) " +
                "VALUES (4, 1, (DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '1 day')::DATE, 'Revisão Mês Passado', 3000.00, 'CONCLUIDA')");

        BigDecimal projecao = dashboardRepository.findProjecaoFinanceiraMesAtual();

        // 1000.00 + 450.50 = 1450.50
        assertThat(projecao).isEqualByComparingTo(new BigDecimal("1450.50"));
    }

    @Test
    @DisplayName("Métrica 5: Deve retornar 0.00 quando não houver manutenções no mês atual")
    void deveRetornarZeroQuandoSemManutencoesNoMesAtual() {
        BigDecimal projecao = dashboardRepository.findProjecaoFinanceiraMesAtual();

        assertThat(projecao).isEqualByComparingTo(BigDecimal.ZERO);
    }
}

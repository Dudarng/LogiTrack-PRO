package com.logitrack.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.logitrack.dashboard.dto.DashboardResponse;
import com.logitrack.dashboard.projection.CronogramaManutencaoProjection;
import com.logitrack.dashboard.projection.RankingUtilizacaoProjection;
import com.logitrack.dashboard.projection.VolumeCategoriaProjection;
import com.logitrack.manutencao.exception.EntidadeNaoEncontradaException;
import com.logitrack.veiculo.TipoVeiculo;
import com.logitrack.veiculo.Veiculo;
import com.logitrack.veiculo.VeiculoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardRepository dashboardRepository;

    @Mock
    private VeiculoRepository veiculoRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("Deve montar DashboardResponse completo para toda a frota com sucesso")
    void deveMontarDashboardResponseParaTodaAFrota() {
        // Arrange
        when(dashboardRepository.findTotalKm(null)).thenReturn(new BigDecimal("4659.00"));

        VolumeCategoriaProjection volLeve = mock(VolumeCategoriaProjection.class);
        when(volLeve.getTipo()).thenReturn("LEVE");
        when(volLeve.getTotalViagens()).thenReturn(7L);
        when(volLeve.getTotalGeral()).thenReturn(12L);
        when(volLeve.getPercentual()).thenReturn(new BigDecimal("58.3"));

        VolumeCategoriaProjection volPesado = mock(VolumeCategoriaProjection.class);
        when(volPesado.getTipo()).thenReturn("PESADO");
        when(volPesado.getTotalViagens()).thenReturn(5L);
        when(volPesado.getPercentual()).thenReturn(new BigDecimal("41.7"));

        when(dashboardRepository.findVolumePorCategoria()).thenReturn(List.of(volLeve, volPesado));

        CronogramaManutencaoProjection cron = mock(CronogramaManutencaoProjection.class);
        when(cron.getId()).thenReturn(7);
        when(cron.getDataInicio()).thenReturn(LocalDate.of(2026, 10, 12));
        when(cron.getDataFinalizacao()).thenReturn(LocalDate.of(2026, 10, 13));
        when(cron.getTipoServico()).thenReturn("Troca de Filtros e Óleo");
        when(cron.getCustoEstimado()).thenReturn(new BigDecimal("620.00"));
        when(cron.getStatus()).thenReturn("EM_REALIZACAO");
        when(cron.getVeiculoId()).thenReturn(4);
        when(cron.getVeiculoPlaca()).thenReturn("LMN-4455");
        when(cron.getVeiculoModelo()).thenReturn("Scania R500");

        when(dashboardRepository.findCronogramaManutencoes()).thenReturn(List.of(cron));

        RankingUtilizacaoProjection rank = mock(RankingUtilizacaoProjection.class);
        when(rank.getVeiculoId()).thenReturn(2);
        when(rank.getPlaca()).thenReturn("XYZ-9876");
        when(rank.getModelo()).thenReturn("Volvo FH");
        when(rank.getTipo()).thenReturn("PESADO");
        when(rank.getTotalKm()).thenReturn(new BigDecimal("2360.00"));

        when(dashboardRepository.findRankingUtilizacao()).thenReturn(Optional.of(rank));
        when(dashboardRepository.findProjecaoFinanceiraMesAtual()).thenReturn(new BigDecimal("11400.00"));

        // Act
        DashboardResponse response = dashboardService.obterDashboard(null);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getFiltroVeiculoId()).isNull();
        assertThat(response.getFiltroVeiculoDescricao()).isEqualTo("Toda a Frota");
        assertThat(response.getTotalKm()).isEqualByComparingTo("4659.00");
        assertThat(response.getProjecaoFinanceira()).isEqualByComparingTo("11400.00");
        assertThat(response.getTotalViagensRealizadas()).isEqualTo(12L);
        assertThat(response.getVolumeCategorias()).hasSize(2);
        assertThat(response.hasVeiculoLider()).isTrue();
        assertThat(response.getVeiculoLider().getPlaca()).isEqualTo("XYZ-9876");
        assertThat(response.hasCronograma()).isTrue();
        assertThat(response.getCronograma().get(0).getVeiculoPlaca()).isEqualTo("LMN-4455");
    }

    @Test
    @DisplayName("Deve filtrar métrica de KM por veículo existente com sucesso")
    void deveFiltrarKmPorVeiculoExistente() {
        Veiculo veiculo = new Veiculo(1, "ABC-1234", "Fiorino", TipoVeiculo.LEVE, 2022);
        when(veiculoRepository.findById(1)).thenReturn(Optional.of(veiculo));
        when(dashboardRepository.findTotalKm(1)).thenReturn(new BigDecimal("719.00"));
        when(dashboardRepository.findVolumePorCategoria()).thenReturn(Collections.emptyList());
        when(dashboardRepository.findCronogramaManutencoes()).thenReturn(Collections.emptyList());
        when(dashboardRepository.findRankingUtilizacao()).thenReturn(Optional.empty());
        when(dashboardRepository.findProjecaoFinanceiraMesAtual()).thenReturn(BigDecimal.ZERO);

        DashboardResponse response = dashboardService.obterDashboard(1);

        assertThat(response).isNotNull();
        assertThat(response.getFiltroVeiculoId()).isEqualTo(1);
        assertThat(response.getFiltroVeiculoDescricao()).isEqualTo("ABC-1234 - Fiorino");
        assertThat(response.getTotalKm()).isEqualByComparingTo("719.00");
        assertThat(response.hasVeiculoLider()).isFalse();
        assertThat(response.hasCronograma()).isFalse();
    }

    @Test
    @DisplayName("Deve lançar EntidadeNaoEncontradaException ao buscar com ID de veículo inexistente")
    void deveLancarExcecaoAoBuscarVeiculoInexistente() {
        when(veiculoRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(EntidadeNaoEncontradaException.class, () -> dashboardService.obterDashboard(999));
    }
}

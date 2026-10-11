package com.logitrack.dashboard;

import com.logitrack.dashboard.dto.CronogramaManutencaoDTO;
import com.logitrack.dashboard.dto.DashboardResponse;
import com.logitrack.dashboard.dto.RankingUtilizacaoDTO;
import com.logitrack.dashboard.dto.VolumeCategoriaDTO;
import com.logitrack.dashboard.projection.CronogramaManutencaoProjection;
import com.logitrack.dashboard.projection.RankingUtilizacaoProjection;
import com.logitrack.dashboard.projection.VolumeCategoriaProjection;
import com.logitrack.manutencao.exception.EntidadeNaoEncontradaException;
import com.logitrack.veiculo.Veiculo;
import com.logitrack.veiculo.VeiculoRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final VeiculoRepository veiculoRepository;

    public DashboardResponse obterDashboard(Integer veiculoId) {
        String filtroDescricao = "Toda a Frota";

        if (veiculoId != null) {
            Veiculo veiculo = veiculoRepository.findById(veiculoId)
                    .orElseThrow(() -> new EntidadeNaoEncontradaException("Veículo com ID " + veiculoId + " não encontrado."));
            filtroDescricao = veiculo.getPlaca() + " - " + veiculo.getModelo();
        }

        // 1. Total de KM (com filtro opcional por veículo)
        BigDecimal totalKm = dashboardRepository.findTotalKm(veiculoId);

        // 2. Volume por categoria (com percentual e total geral calculados via SQL)
        List<VolumeCategoriaProjection> volumeProjections = dashboardRepository.findVolumePorCategoria();
        Long totalViagens = 0L;
        if (!volumeProjections.isEmpty() && volumeProjections.get(0).getTotalGeral() != null) {
            totalViagens = volumeProjections.get(0).getTotalGeral();
        }

        List<VolumeCategoriaDTO> volumeCategorias = volumeProjections.stream()
                .map(p -> VolumeCategoriaDTO.builder()
                        .tipo(p.getTipo())
                        .totalViagens(p.getTotalViagens())
                        .percentual(p.getPercentual() != null ? p.getPercentual() : BigDecimal.ZERO)
                        .build())
                .toList();

        // 3. Cronograma (próximas 5 manutenções não concluídas)
        List<CronogramaManutencaoProjection> cronogramaProjections = dashboardRepository.findCronogramaManutencoes();
        List<CronogramaManutencaoDTO> cronograma = cronogramaProjections.stream()
                .map(p -> CronogramaManutencaoDTO.builder()
                        .id(p.getId())
                        .dataInicio(p.getDataInicio())
                        .dataFinalizacao(p.getDataFinalizacao())
                        .tipoServico(p.getTipoServico())
                        .custoEstimado(p.getCustoEstimado())
                        .status(p.getStatus())
                        .veiculoId(p.getVeiculoId())
                        .veiculoPlaca(p.getVeiculoPlaca())
                        .veiculoModelo(p.getVeiculoModelo())
                        .build())
                .toList();

        // 4. Ranking de utilização (veículo líder em KM percorrida)
        RankingUtilizacaoDTO veiculoLider = dashboardRepository.findRankingUtilizacao()
                .map(p -> RankingUtilizacaoDTO.builder()
                        .veiculoId(p.getVeiculoId())
                        .placa(p.getPlaca())
                        .modelo(p.getModelo())
                        .tipo(p.getTipo())
                        .totalKm(p.getTotalKm())
                        .build())
                .orElse(null);

        // 5. Projeção financeira (manutenções no mês atual)
        BigDecimal projecaoFinanceira = dashboardRepository.findProjecaoFinanceiraMesAtual();

        return DashboardResponse.builder()
                .totalKm(totalKm != null ? totalKm : BigDecimal.ZERO)
                .filtroVeiculoId(veiculoId)
                .filtroVeiculoDescricao(filtroDescricao)
                .volumeCategorias(volumeCategorias)
                .totalViagensRealizadas(totalViagens)
                .veiculoLider(veiculoLider)
                .cronograma(cronograma)
                .projecaoFinanceira(projecaoFinanceira != null ? projecaoFinanceira : BigDecimal.ZERO)
                .build();
    }
}

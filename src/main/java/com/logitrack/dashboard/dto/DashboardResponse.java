package com.logitrack.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    private BigDecimal totalKm;
    private Integer filtroVeiculoId;
    private String filtroVeiculoDescricao;
    private List<VolumeCategoriaDTO> volumeCategorias;
    private Long totalViagensRealizadas;
    private RankingUtilizacaoDTO veiculoLider;
    private List<CronogramaManutencaoDTO> cronograma;
    private BigDecimal projecaoFinanceira;

    public boolean hasVeiculoLider() {
        return veiculoLider != null;
    }

    public boolean hasCronograma() {
        return cronograma != null && !cronograma.isEmpty();
    }

    public boolean hasViagensRealizadas() {
        return totalViagensRealizadas != null && totalViagensRealizadas > 0;
    }
}

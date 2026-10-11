package com.logitrack.dashboard.dto;

import java.math.BigDecimal;
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
public class RankingUtilizacaoDTO {

    private Integer veiculoId;
    private String placa;
    private String modelo;
    private String tipo;
    private BigDecimal totalKm;

    public String getVeiculoDescricao() {
        if (placa == null) {
            return "N/D";
        }
        return placa + " - " + (modelo != null ? modelo : "");
    }
}

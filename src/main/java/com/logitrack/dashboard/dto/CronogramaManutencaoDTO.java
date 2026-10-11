package com.logitrack.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
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
public class CronogramaManutencaoDTO {

    private Integer id;
    private LocalDate dataInicio;
    private LocalDate dataFinalizacao;
    private String tipoServico;
    private BigDecimal custoEstimado;
    private String status;
    private Integer veiculoId;
    private String veiculoPlaca;
    private String veiculoModelo;

    public String getVeiculoDescricao() {
        if (veiculoPlaca == null) {
            return "N/D";
        }
        return veiculoPlaca + " - " + (veiculoModelo != null ? veiculoModelo : "");
    }
}

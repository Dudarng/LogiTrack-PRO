package com.logitrack.manutencao.dto;

import com.logitrack.manutencao.Manutencao;
import com.logitrack.manutencao.StatusManutencao;
import com.logitrack.veiculo.TipoVeiculo;
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
public class ManutencaoResponse {

    private Integer id;
    private Integer veiculoId;
    private String veiculoPlaca;
    private String veiculoModelo;
    private TipoVeiculo veiculoTipo;
    private LocalDate dataInicio;
    private LocalDate dataFinalizacao;
    private String tipoServico;
    private BigDecimal custoEstimado;
    private StatusManutencao status;

    public boolean isConcluida() {
        return status != null && status.isConcluida();
    }

    public String getVeiculoDescricao() {
        if (veiculoPlaca == null) {
            return "Veículo N/D";
        }
        return veiculoPlaca + " - " + (veiculoModelo != null ? veiculoModelo : "");
    }

    public static ManutencaoResponse fromEntity(Manutencao manutencao) {
        if (manutencao == null) {
            return null;
        }

        Integer vId = null;
        String vPlaca = null;
        String vModelo = null;
        TipoVeiculo vTipo = null;

        if (manutencao.getVeiculo() != null) {
            vId = manutencao.getVeiculo().getId();
            vPlaca = manutencao.getVeiculo().getPlaca();
            vModelo = manutencao.getVeiculo().getModelo();
            vTipo = manutencao.getVeiculo().getTipo();
        }

        return ManutencaoResponse.builder()
                .id(manutencao.getId())
                .veiculoId(vId)
                .veiculoPlaca(vPlaca)
                .veiculoModelo(vModelo)
                .veiculoTipo(vTipo)
                .dataInicio(manutencao.getDataInicio())
                .dataFinalizacao(manutencao.getDataFinalizacao())
                .tipoServico(manutencao.getTipoServico())
                .custoEstimado(manutencao.getCustoEstimado())
                .status(manutencao.getStatus())
                .build();
    }

    public ManutencaoRequest toRequest() {
        return ManutencaoRequest.builder()
                .veiculoId(veiculoId)
                .dataInicio(dataInicio)
                .dataFinalizacao(dataFinalizacao)
                .tipoServico(tipoServico)
                .custoEstimado(custoEstimado)
                .status(status)
                .build();
    }
}

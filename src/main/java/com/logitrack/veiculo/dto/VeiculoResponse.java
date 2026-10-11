package com.logitrack.veiculo.dto;

import com.logitrack.veiculo.TipoVeiculo;
import com.logitrack.veiculo.Veiculo;
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
public class VeiculoResponse {

    private Integer id;
    private String placa;
    private String modelo;
    private TipoVeiculo tipo;
    private Integer ano;

    public String getDescricaoCompleta() {
        String tipoDesc = tipo != null ? tipo.getDescricao() : "N/D";
        return placa + " - " + modelo + " (" + tipoDesc + ")";
    }

    public static VeiculoResponse fromEntity(Veiculo veiculo) {
        if (veiculo == null) {
            return null;
        }
        return VeiculoResponse.builder()
                .id(veiculo.getId())
                .placa(veiculo.getPlaca())
                .modelo(veiculo.getModelo())
                .tipo(veiculo.getTipo())
                .ano(veiculo.getAno())
                .build();
    }
}

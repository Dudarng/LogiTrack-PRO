package com.logitrack.manutencao.dto;

import com.logitrack.manutencao.StatusManutencao;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManutencaoRequest {

    @NotNull(message = "O veículo é obrigatório")
    private Integer veiculoId;

    @NotNull(message = "A data de início é obrigatória")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataInicio;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataFinalizacao;

    @NotBlank(message = "O tipo de serviço é obrigatório")
    @Size(max = 100, message = "O tipo de serviço não pode ultrapassar 100 caracteres")
    private String tipoServico;

    @NotNull(message = "O custo estimado é obrigatório")
    @DecimalMin(value = "0.00", message = "O custo estimado não pode ser negativo")
    private BigDecimal custoEstimado;

    @NotNull(message = "O status é obrigatório")
    private StatusManutencao status;
}

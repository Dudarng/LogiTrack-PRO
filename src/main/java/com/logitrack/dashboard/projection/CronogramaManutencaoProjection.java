package com.logitrack.dashboard.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface CronogramaManutencaoProjection {

    Integer getId();

    LocalDate getDataInicio();

    LocalDate getDataFinalizacao();

    String getTipoServico();

    BigDecimal getCustoEstimado();

    String getStatus();

    Integer getVeiculoId();

    String getVeiculoPlaca();

    String getVeiculoModelo();
}

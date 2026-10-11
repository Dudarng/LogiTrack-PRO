package com.logitrack.dashboard.projection;

import java.math.BigDecimal;

public interface RankingUtilizacaoProjection {

    Integer getVeiculoId();

    String getPlaca();

    String getModelo();

    String getTipo();

    BigDecimal getTotalKm();
}

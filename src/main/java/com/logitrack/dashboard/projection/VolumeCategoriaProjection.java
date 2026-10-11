package com.logitrack.dashboard.projection;

import java.math.BigDecimal;

public interface VolumeCategoriaProjection {

    String getTipo();

    Long getTotalViagens();

    Long getTotalGeral();

    BigDecimal getPercentual();
}

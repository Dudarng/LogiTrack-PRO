package com.logitrack.manutencao.exception;

import lombok.Getter;

@Getter
public class RegraNegocioException extends RuntimeException {

    private final String campo;

    public RegraNegocioException(String message) {
        super(message);
        this.campo = null;
    }

    public RegraNegocioException(String campo, String message) {
        super(message);
        this.campo = campo;
    }
}

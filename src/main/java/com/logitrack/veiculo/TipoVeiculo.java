package com.logitrack.veiculo;

public enum TipoVeiculo {
    LEVE("Leve"),
    PESADO("Pesado");

    private final String descricao;

    TipoVeiculo(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}

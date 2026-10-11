package com.logitrack.manutencao;

public enum StatusManutencao {
    PENDENTE("Pendente"),
    EM_REALIZACAO("Em Realização"),
    CONCLUIDA("Concluída");

    private final String descricao;

    StatusManutencao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isConcluida() {
        return this == CONCLUIDA;
    }
}

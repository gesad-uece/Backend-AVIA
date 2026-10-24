package br.com.avia.avia_api.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PapelProjeto {
    GERENTE("gerente", true),
    MEMBRO("membro", true),
    VISUALIZADOR("visualizador", true);

    private final String nomePapel;
    private final boolean isAtribuivel;

    PapelProjeto(String nomePapel, boolean isAtribuivel) {
        this.nomePapel = nomePapel;
        this.isAtribuivel = isAtribuivel;
    }

    @JsonValue
    public String getNomePapel(){return nomePapel;}

    public boolean isAtribuivel() {return isAtribuivel;}

    @JsonCreator
    public static PapelProjeto deString(String valor) {
        if (valor == null) return null;
        for (PapelProjeto cargo : PapelProjeto.values()) {
            if (cargo.getNomePapel().equalsIgnoreCase(valor) || cargo.name().equalsIgnoreCase(valor)) {
                return cargo;
            }
        }
        throw new IllegalArgumentException("Papel desconhecido: " + valor);
    }
}

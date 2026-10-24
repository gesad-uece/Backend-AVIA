package br.com.avia.avia_api.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;


public enum CargoProjeto {
    GERENTE ("Gerente", true),
    SCRUMMASTER ("Scrum Master", true),
    DESENVOLVEDOR ("Desenvolvedor", true),
    QA ("QA", true),
    ANALISTADEREQUISITOS ("Analista de Requisitos", true),
    DESIGNERDEUIUX ("Designer de UI/UX", true),
    ARQUITETODESOFTWARE ("Arquiteto de Software", true),
    DEVOPS ("DevOps", true);

    private final String nomeCargo;
    @Getter
    private final boolean isAtribuivel;

    CargoProjeto(String nomeCargo, boolean isAtribuivel) {
        this.nomeCargo = nomeCargo;
        this.isAtribuivel = isAtribuivel;
    }

    @JsonValue
    public String getNomeCargo() {
        return nomeCargo;
    }

    @JsonCreator
    public static CargoProjeto deString(String valor) {
        if (valor == null) return null;
        for (CargoProjeto cargo : CargoProjeto.values()) {
            if (cargo.getNomeCargo().equalsIgnoreCase(valor) || cargo.name().equalsIgnoreCase(valor)) {
                return cargo;
            }
        }
        throw new IllegalArgumentException("Cargo desconhecido: " + valor);
    }
}

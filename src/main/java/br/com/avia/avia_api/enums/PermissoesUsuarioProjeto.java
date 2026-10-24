package br.com.avia.avia_api.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PermissoesUsuarioProjeto {
    CRIARTAREFAS("Criar Tarefas"),
    EDITARTAREFAS("Editar Tarefas"),
    EXCLUIRTAREFAS("Excluir Tarefas"),
    MOVERTAREFAS("Mover Tarefas"),
    CRIARCOLUNAS("Criar Colunas"),
    EDITARESTRUTURA("Editar Estrutura"),
    GERENCIARSPRINTS("Gerenciar Sprints"),
    ADICIONARMEMBROS("Adicionar Membros"),
    // apenas gerentes podem remover
//    REMOVERMEMBROS("Remover Membros"),
    ATUALIZARPERMISSOES("Atualizar Permissoes"),
    VISUALIZARPROJETO("Visualizar Projeto");

    private final String nome;


    PermissoesUsuarioProjeto(String nome) {
        this.nome = nome;
    }

    @JsonValue
    public String getNome() {return nome;}

    @JsonCreator
    public static PermissoesUsuarioProjeto deString(String valor){
        if (valor == null) return null;
        for (PermissoesUsuarioProjeto permissao : PermissoesUsuarioProjeto.values()) {
            if (permissao.getNome().equalsIgnoreCase(valor) || permissao.name().equalsIgnoreCase(valor)) {
                return permissao;
            }
        }
        throw new IllegalArgumentException("Permissão desconhecido: " + valor);
    }
}

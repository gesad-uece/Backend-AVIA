package br.com.avia.avia_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Entity
@Table(name = "notificacoes_projeto_usuario")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificacaoProjetoUsuario {

    public NotificacaoProjetoUsuario(Long projetoId, Usuario usuario, Notificacao notificacao){
       this.projetoId = Objects.requireNonNull(projetoId, "O id do projeto é obrigatório");
       this.usuario = Objects.requireNonNull(usuario, "O usuário é obrigatório");
       this.notificacao = Objects.requireNonNull(notificacao, "A notificação é obritaória");
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // garantir que o usuario nao seja null antes de fazer a consulta no banco (mais rapido)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    // sem FK pois depende de como funcionara a remoção de projetos
    @Column(name = "projeto_id", nullable = false)
    private Long projetoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notificacao_id", nullable = false, updatable = false)
    private Notificacao notificacao;

    @Column(name = "lida", nullable = false)
    private Boolean lida = false;

    public void marcarLida(){
        this.lida = true;
    }
}
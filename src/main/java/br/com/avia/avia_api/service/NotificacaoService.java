package br.com.avia.avia_api.service;

import br.com.avia.avia_api.dto.notificacao.NotificacaoResponseDTO;
import br.com.avia.avia_api.entity.*;
import br.com.avia.avia_api.enums.TipoNotificacao;
import br.com.avia.avia_api.exception.projeto.ProjetoNaoEncontradoException;
import br.com.avia.avia_api.repository.NotificacaoProjetoUsuarioRepository;
import br.com.avia.avia_api.repository.NotificacaoRepository;
import br.com.avia.avia_api.repository.ProjetoRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final NotificacaoProjetoUsuarioRepository notificacaoProjetoUsuarioRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ProjetoRepository projetoRepository;

    public NotificacaoService(
            NotificacaoRepository notificacaoRepository,
            NotificacaoProjetoUsuarioRepository notificacaoProjetoUsuarioRepository,
            SimpMessagingTemplate messagingTemplate,
            ProjetoRepository projetoRepository
    ) {
        this.notificacaoRepository = notificacaoRepository;
        this.notificacaoProjetoUsuarioRepository = notificacaoProjetoUsuarioRepository;
        this.messagingTemplate = messagingTemplate;
        this.projetoRepository = projetoRepository;
    }

    public void notificarUsuarioAdicionadoAoProjeto(Usuario usuario, Long projetoId) {
        Notificacao notificacao = new Notificacao();
        notificacao.setTipo(TipoNotificacao.ADICIONADO_PROJETO);

        notificacaoRepository.save(notificacao);

        criarNotificacaoProjetoUsuario(usuario, projetoId, notificacao);
    }

    public void notificarUsuarioMencionadoEmComentario(Usuario usuario, Long projetoId, Comentario comentario) {
        Notificacao notificacao = new Notificacao();
        notificacao.setTipo(TipoNotificacao.MENCIONADO_COMENTARIO);
        notificacao.setComentario(comentario);

        notificacaoRepository.save(notificacao);

        criarNotificacaoProjetoUsuario(usuario, projetoId, notificacao);
    }

    private void criarNotificacaoProjetoUsuario(Usuario usuario, Long projetoId, Notificacao notificacao) {
        NotificacaoProjetoUsuario notificacaoProjetoUsuario =
                new NotificacaoProjetoUsuario(projetoId, usuario, notificacao);

        NotificacaoProjetoUsuario salva = notificacaoProjetoUsuarioRepository.save(notificacaoProjetoUsuario);
        enviarNotificacaoEmTempoReal(usuario, toResponseDTO(notificacaoProjetoUsuario));
    }

    private void enviarNotificacaoEmTempoReal(Usuario usuario, NotificacaoResponseDTO notificacao) {
        Projeto projeto = projetoRepository.findByNome(notificacao.nomeProjeto())
                .orElseThrow(ProjetoNaoEncontradoException::new);
        String emailUsuario = usuario.getEmail();
        String destino = "/queue/projetos/" + projeto.getId() + "/notificacoes";

        Runnable envio = () -> messagingTemplate.convertAndSendToUser(emailUsuario, destino, notificacao);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    envio.run();
                }
            });
            return;
        }

        envio.run();
    }

    @Transactional(readOnly = true)
    public List<NotificacaoResponseDTO> listarNotificacoesDoProjeto(Long projetoId, String emailUsuario) {
        return notificacaoProjetoUsuarioRepository
                .findByNotificacaoTarefaProjetoIdAndUsuarioEmailOrderByNotificacaoCriadoEmDesc(
                        projetoId,
                        emailUsuario
                )
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public Long contarNaoLidasDoProjeto(Long projetoId, String emailUsuario) {
        return notificacaoProjetoUsuarioRepository
                .countByNotificacaoTarefaProjetoIdAndUsuarioEmailAndLidaFalse(
                        projetoId,
                        emailUsuario
                );
    }

    @Transactional
    public void marcarComoLidaNoProjeto(Long projetoId, Long notificacaoProjetoUsuarioId, String emailUsuario) {
        NotificacaoProjetoUsuario notificacaoProjetoUsuario =
                notificacaoProjetoUsuarioRepository
                        .findByIdAndNotificacaoTarefaProjetoIdAndUsuarioEmail(
                                notificacaoProjetoUsuarioId,
                                projetoId,
                                emailUsuario
                        )
                        .orElseThrow(() -> new RuntimeException("Notificação não encontrada neste projeto."));

        notificacaoProjetoUsuario.marcarLida();

        notificacaoProjetoUsuarioRepository.save(notificacaoProjetoUsuario);
    }

    private NotificacaoResponseDTO toResponseDTO(NotificacaoProjetoUsuario notificacaoProjetoUsuario) {
        Notificacao notificacao = notificacaoProjetoUsuario.getNotificacao();
        Projeto projeto = acharProjeto(notificacaoProjetoUsuario.getProjetoId());

        String nomeProjeto = projeto.getNome();

        String mensagem = gerarMensagem(notificacaoProjetoUsuario);

        return new NotificacaoResponseDTO(
                notificacaoProjetoUsuario.getId(),
                notificacao.getTipo(),
                mensagem,
                nomeProjeto,
                notificacaoProjetoUsuario.getLida(),
                notificacao.getCriadoEm()
        );
    }

    private String gerarMensagem(NotificacaoProjetoUsuario notificacaoProjetoUsuario) {
        Notificacao notificacao = notificacaoProjetoUsuario.getNotificacao();
        Projeto projeto = acharProjeto(notificacaoProjetoUsuario.getProjetoId());

        return switch (notificacao.getTipo()) {
            case ADICIONADO_PROJETO -> "Você foi adicionado ao projeto "
                    + projeto.getNome() + ".";

            case RESPONSAVEL_TAREFA -> "Você foi adicionado como responsável por uma tarefa.";

            case MENCIONADO_COMENTARIO -> "Você foi mencionado em um comentário.";
        };
    }

    private Projeto acharProjeto(Long projetoId){
        return projetoRepository.findById(projetoId).orElseThrow(ProjetoNaoEncontradoException::new);
    }

}

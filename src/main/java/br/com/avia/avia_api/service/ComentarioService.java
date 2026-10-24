package br.com.avia.avia_api.service;

import br.com.avia.avia_api.dto.comentario.ComentarioResponseDTO;
import br.com.avia.avia_api.dto.comentario.CriarComentarioRequestDTO;
import br.com.avia.avia_api.entity.Comentario;
import br.com.avia.avia_api.entity.ProjetoUsuario;
import br.com.avia.avia_api.entity.Tarefa;
import br.com.avia.avia_api.repository.ComentarioRepository;
import br.com.avia.avia_api.repository.ProjetoUsuarioRepository;
import br.com.avia.avia_api.repository.TarefaRepository;
import br.com.avia.avia_api.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ComentarioService {

    private final ComentarioRepository comentarioRepository;
    private final TarefaRepository tarefaRepository;
    private final ProjetoUsuarioRepository projetoUsuarioRepository;
    private final NotificacaoService notificacaoService;
    private final UsuarioRepository usuarioRepository;

    public ComentarioService(ComentarioRepository comentarioRepository,
                             TarefaRepository tarefaRepository,
                             ProjetoUsuarioRepository projetoUsuarioRepository,
                             NotificacaoService notificacaoService,
                             UsuarioRepository usuarioRepository) {
        this.comentarioRepository = comentarioRepository;
        this.tarefaRepository = tarefaRepository;
        this.projetoUsuarioRepository = projetoUsuarioRepository;
        this.notificacaoService = notificacaoService;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public ComentarioResponseDTO criarComentario(Long projetoId, Long tarefaId, CriarComentarioRequestDTO dto, String emailUsuario) {
        Tarefa tarefa = tarefaRepository.findById(tarefaId)
                .orElseThrow(() -> new RuntimeException("Tarefa não encontrada"));

        if (!Objects.equals(tarefa.getProjetoId(), projetoId)) {
            throw new RuntimeException("Tarefa não pertence a esse projeto");
        }

        ProjetoUsuario autor = projetoUsuarioRepository.findByProjetoIdAndUsuarioEmail(projetoId, emailUsuario)
                .orElseThrow(() -> new RuntimeException("Usuário não é membro do projeto."));

        Comentario comentario = new Comentario(tarefaId, dto.descricao(), autor.getId());
        Comentario salvo = comentarioRepository.save(comentario);
        notificarUsuariosMencionados(salvo, projetoId, obterEmailsMencionados(dto));

        return converterParaDTO(salvo);
    }

    public List<ComentarioResponseDTO> listarComentarios(Long tarefaId) {
        tarefaRepository.findById(tarefaId)
                .orElseThrow(() -> new RuntimeException("Tarefa não encontrada"));
        return comentarioRepository.findByTarefaIdOrderByIdAsc(tarefaId)
                .stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ComentarioResponseDTO alterarFlag(Long comentarioId) {
        Comentario comentario = comentarioRepository.findById(comentarioId)
                .orElseThrow(() -> new RuntimeException("Comentário não encontrado"));
        comentario.setImpedimento(!comentario.isImpedimento());
        return converterParaDTO(comentarioRepository.save(comentario));
    }

    @Transactional
    public void deletarComentario(Long tarefaId, Long comentarioId) {
        Comentario comentario = comentarioRepository.findById(comentarioId)
                .orElseThrow(() -> new RuntimeException("Comentário não encontrado"));
        if (!comentario.getTarefaId().equals(tarefaId)) {
            throw new RuntimeException("Comentário não pertence a essa tarefa");
        }
        comentarioRepository.delete(comentario);
    }

    @Transactional
    public ComentarioResponseDTO editarComentario(Long tarefaId, Long comentarioId, String novaDescricao) {
        Comentario comentario = comentarioRepository.findById(comentarioId)
                .orElseThrow(() -> new RuntimeException("Comentário não encontrado"));
        if (!comentario.getTarefaId().equals(tarefaId)) {
            throw new RuntimeException("Comentário não pertence a essa tarefa");
        }
        comentario.setDescricao(novaDescricao);
        return converterParaDTO(comentarioRepository.save(comentario));
    }

    private ComentarioResponseDTO converterParaDTO(Comentario comentario) {
        return new ComentarioResponseDTO(
                comentario.getId(),
                comentario.getTarefaId(),
                comentario.getDescricao(),
                comentario.getProjetoUsuarioId(),
                comentario.isImpedimento(),
                comentario.getCriadoEm()
        );
    }

    private Set<String> obterEmailsMencionados(CriarComentarioRequestDTO dto) {
        Set<String> emails = new LinkedHashSet<>();

        if (dto.emailsMencionados() != null) {
            dto.emailsMencionados().forEach(email -> adicionarEmailNormalizado(emails, email));
        }

        return emails;
    }

    private void notificarUsuariosMencionados(Comentario comentario, Long projetoId, Set<String> emailsMencionados) {
        // depois verificar se caso o usuario nao pertenca ao projeto, ele nao receba
        emailsMencionados.forEach(email ->
                usuarioRepository.findByEmail(email)
                        .ifPresent(usuario ->
                                notificacaoService.notificarUsuarioMencionadoEmComentario(usuario, projetoId, comentario))
        );
    }

    private void adicionarEmailNormalizado(Set<String> emails, String email) {
        if (email != null && !email.isBlank()) {
            emails.add(email.trim().toLowerCase(Locale.ROOT));
        }
    }
}

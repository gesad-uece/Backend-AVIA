package br.com.avia.avia_api.service;

import br.com.avia.avia_api.dto.permissao.PermissoesMembroRequestDTO;
import br.com.avia.avia_api.dto.projeto.*;
import br.com.avia.avia_api.entity.*;
import br.com.avia.avia_api.enums.CargoProjeto;
import br.com.avia.avia_api.enums.PapelProjeto;
import br.com.avia.avia_api.enums.PermissoesUsuarioProjeto;
import br.com.avia.avia_api.exception.permissao.PermissaoNegadaException;
import br.com.avia.avia_api.exception.projeto.CargoException;
import br.com.avia.avia_api.exception.projeto.ProjetoNaoEncontradoException;
import br.com.avia.avia_api.exception.projeto.UltimoGerenteException;
import br.com.avia.avia_api.exception.projeto.UsuarioException;
import br.com.avia.avia_api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjetoService {

    private final PermissaoRepository permissaoRepository;
    private final ProjetoRepository projetoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProjetoUsuarioRepository projetoUsuarioRepository;
    private final PapelRepository papelRepository;
    private final NotificacaoService notificacaoService;

    public ProjetoService(PermissaoRepository permissaoRepository, ProjetoRepository projetoRepository,
                          UsuarioRepository usuarioRepository,
                          ProjetoUsuarioRepository projetoUsuarioRepository,
                          PapelRepository papelRepository,
                          NotificacaoService notificacaoService) {
        this.permissaoRepository = permissaoRepository;
        this.projetoRepository = projetoRepository;
        this.usuarioRepository = usuarioRepository;
        this.projetoUsuarioRepository = projetoUsuarioRepository;
        this.papelRepository = papelRepository;
        this.notificacaoService = notificacaoService;
    }

    @Transactional
    public ProjetoResponseDTO criarProjeto(CriarProjetoRequestDTO dto, String emailUsuario) {
        Usuario usuarioLogado = buscarUsuarioPorEmail(emailUsuario);

        Projeto projeto = new Projeto();
        projeto.setNome(dto.nome());
        projeto.setTipoEstimativa(dto.tipoEstimativa());
        Projeto projetoSalvo = projetoRepository.save(projeto);

        String papelGerente = PapelProjeto.GERENTE.name();
        Papel papel = papelRepository.findByNome(papelGerente)
                .orElseThrow(() -> new RuntimeException("Papel inexistente."));
        List<Permissao> permissoesGerente = new ArrayList<>(papel.getPermissoes());

        ProjetoUsuario vinculoGerente = new ProjetoUsuario(CargoProjeto.GERENTE, usuarioLogado, projetoSalvo, papel);
        vinculoGerente.setPermissoes(permissoesGerente);
        projetoUsuarioRepository.save(vinculoGerente);

        return converterParaDTO(projetoSalvo);
    }

    public ProjetoResponseDTO buscarPorId(Long projetoId, String emailUsuario) {
        Usuario usuarioLogado = buscarUsuarioPorEmail(emailUsuario);

        List<Projeto> projetosDoUsuario = projetoUsuarioRepository.findProjetosByMembroId(usuarioLogado.getId());

        boolean temAcesso = projetosDoUsuario.stream()
                .anyMatch(p -> p.getId().equals(projetoId));

        if (!temAcesso) {
            throw new ProjetoNaoEncontradoException("Projeto não encontrado ou você não tem permissão para visualizá-lo.");
        }

        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new ProjetoNaoEncontradoException("Projeto não encontrado."));

        return converterParaDTO(projeto);
    }

    public List<ProjetoResponseDTO> listarProjetosDoUsuario(String emailUsuario) {
        Usuario usuarioLogado = buscarUsuarioPorEmail(emailUsuario);

        List<Projeto> projetos = projetoUsuarioRepository.findProjetosByMembroId(usuarioLogado.getId());

        return projetos.stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    public List<MembroResponseDTO> listarMembrosDoProjeto(long idProjeto, String emailUsuario) {
        Usuario usuarioLogado = buscarUsuarioPorEmail(emailUsuario);

        List<Projeto> projetosDoUsuario = projetoUsuarioRepository.findProjetosByMembroId(usuarioLogado.getId());
        boolean temAcesso = projetosDoUsuario.stream().anyMatch(p -> p.getId() == idProjeto);

        if (!temAcesso) {
            throw new RuntimeException("Você não tem permissão para visualizar os membros deste projeto.");
        }

        List<ProjetoUsuario> membros = projetoUsuarioRepository.findMembrosByProjetoId(idProjeto);

        return membros.stream()
                .map(this::converterMembroParaDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public MembroResponseDTO atualizarMembroDoProjeto(Long projetoId, Long usuarioAlvoId,
                                                      AtualizarMembroRequestDTO dto, String emailSolicitante) {
        projetoRepository.buscarComLock(projetoId)
                .orElseThrow(ProjetoNaoEncontradoException::new);

        Usuario solicitante = buscarUsuarioPorEmail(emailSolicitante);
        // fazer 404 e esconder informações, falar apenas que algo não foi encontrado
        projetoUsuarioRepository.findByProjetoIdAndUsuarioId(projetoId, solicitante.getId())
                .orElseThrow(() -> new UsuarioException("Membro não encontrado no projeto."));
        ProjetoUsuario usuarioAlvo = projetoUsuarioRepository.findByProjetoIdAndUsuarioId(projetoId, usuarioAlvoId)
                .orElseThrow(() -> new UsuarioException("Membro não encontrado no projeto."));

        verificarPermissao(projetoId, solicitante.getId(), PermissoesUsuarioProjeto.ATUALIZARPERMISSOES);

        atribuirCargoEPapel(projetoId, dto, usuarioAlvo);

        projetoUsuarioRepository.save(usuarioAlvo);
        return converterMembroParaDTO(usuarioAlvo);
    }

    @Transactional
    public void deletarProjeto(Long projetoId, String emailUsuario) {
        Usuario usuarioLogado = buscarUsuarioPorEmail(emailUsuario);

        validarPermissaoGerente(projetoId, usuarioLogado.getId());

        projetoUsuarioRepository.deleteByProjetoId(projetoId);

        projetoRepository.deleteById(projetoId);
    }

    @Transactional
    public ProjetoResponseDTO editarInformacoesDoProjeto(Long projetoId, AtualizarProjetoRequestDTO dto,
                                                         String emailUsuario) {
        Usuario usuarioLogado = buscarUsuarioPorEmail(emailUsuario);

        validarPermissaoGerente(projetoId, usuarioLogado.getId());

        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new ProjetoNaoEncontradoException("Projeto não encontrado."));

        if (dto.nome() != null && !dto.nome().isBlank()) {
            projeto.setNome(dto.nome());
        }

        Projeto projetoAtualizado = projetoRepository.save(projeto);
        return converterParaDTO(projetoAtualizado);
    }

    @Transactional
    public void adicionarMembroAoProjeto(Long projetoId, AdicionarMembroProjetoRequestDTO dto, String emailMembro) {
        Usuario membro = buscarUsuarioPorEmail(emailMembro);

        verificarPermissao(projetoId, membro.getId(), PermissoesUsuarioProjeto.ADICIONARMEMBROS);

        if (!dto.cargo().isAtribuivel()) {
            throw new RuntimeException("Este cargo não pode ser atribuído manualmente.");
        }
        if (!dto.papel().isAtribuivel()) {
            throw new RuntimeException("Este papel não pode ser atribuído manualmente.");
        }
        validarCargoEPapel(dto.cargo(), dto.papel().name());

        Usuario novoMembro = buscarUsuarioPorEmail(dto.emailUsuario());

        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new ProjetoNaoEncontradoException("Projeto não encontrado."));

        boolean jaEhMembro = projetoUsuarioRepository.existsByProjetoIdAndUsuarioId(projetoId, novoMembro.getId());
        if (jaEhMembro) {
            throw new RuntimeException("Este usuário já é membro do projeto.");
        }

        String nomePapel = dto.papel().name();
        Papel papel = papelRepository.findByNome(nomePapel)
                .orElseThrow(() -> new RuntimeException("Papel inexistente."));

        List<Permissao> permissoesDoNovoMembro = new ArrayList<>(papel.getPermissoes());

        ProjetoUsuario pu = new ProjetoUsuario(dto.cargo(), novoMembro, projeto, papel);
        pu.setPermissoes(permissoesDoNovoMembro);
        projetoUsuarioRepository.save(pu);

        notificacaoService.notificarUsuarioAdicionadoAoProjeto(novoMembro, projetoId);
    }

    @Transactional
    public String removerMembroDoProjeto(Long projetoId, RemoverMembroRequestDTO dto, String emailSolicitante) {
        projetoRepository.buscarComLock(projetoId)
                .orElseThrow(ProjetoNaoEncontradoException::new);

        Usuario membroSolicitante = buscarUsuarioPorEmail(emailSolicitante);
        ProjetoUsuario usuarioSolicitante = projetoUsuarioRepository.
                findByProjetoIdAndUsuarioId(projetoId, membroSolicitante.getId())
                .orElseThrow(() -> new UsuarioException("Usuario não encontrado"));

        Usuario membroAlvo = buscarUsuarioPorEmail(dto.emailMembro());
        ProjetoUsuario projetoUsuarioAlvo = projetoUsuarioRepository.findByProjetoIdAndUsuarioId(projetoId, membroAlvo.getId())
                .orElseThrow(() -> new UsuarioException("Usuário não está no projeto."));

        verificarGerente(projetoId, usuarioSolicitante);
        if (projetoUsuarioAlvo.getCargo() == CargoProjeto.GERENTE)
            verificarQuantidadeDeGerentes(projetoId);

        projetoUsuarioRepository.delete(projetoUsuarioAlvo);

        return membroAlvo.getNome();
    }

    @Transactional
    public void editarPermissoesMembro(Long projetoId, Long usuarioId, PermissoesMembroRequestDTO dto,
                                       String emailGerente) {
        Usuario gerente = buscarUsuarioPorEmail(emailGerente);
        // so gerentes podem atualizar permissoes, portanto uma permissao atribuida nao pode ser a permissao de ATUALIZARPERMISSOES
        validarPermissaoGerente(projetoId, gerente.getId());

        ProjetoUsuario pu = projetoUsuarioRepository.findByUsuarioIdAndProjetoId(usuarioId, projetoId)
                .orElseThrow(() -> new UsuarioException("Usuário não está no projeto."));

        if(dto.permissoes().stream().anyMatch(p -> p == PermissoesUsuarioProjeto.ATUALIZARPERMISSOES || p == PermissoesUsuarioProjeto.ADICIONARMEMBROS)){
            // talvez especificar qual permissao nao é aceita?
            throw new PermissaoNegadaException();
        }

        List<Permissao> novasPermissoes = new ArrayList<>();

        for (PermissoesUsuarioProjeto permissaoEnum : dto.permissoes()) {
            String descricao = permissaoEnum.name();
            Permissao novaPermissao = permissaoRepository.findByDescricao(descricao);
            if (novaPermissao != null)
                novasPermissoes.add(novaPermissao);
        }

        pu.getPermissoes().clear();
        pu.getPermissoes().addAll(novasPermissoes);
        projetoUsuarioRepository.save(pu);
    }

    public List<PermissoesUsuarioProjeto> visualizarPermissoesMembro(Long projetoId, Long usuarioId,
                                                                     String emailMembro) {
        buscarUsuarioPorEmail(emailMembro);

        ProjetoUsuario pu = projetoUsuarioRepository.findByUsuarioIdAndProjetoId(usuarioId, projetoId)
                .orElseThrow(() -> new UsuarioException("Usuário não está no projeto."));

        List<Permissao> permissoes = pu.getPermissoes();

        return permissoes.stream()
                .map(permissao -> PermissoesUsuarioProjeto.deString(permissao.getDescricao()))
                .toList();
    }

    @Transactional
    public void restaurarPermissoesPadrao(Long projetoId, Long usuarioId, String emailGerente) {
        Usuario gerente = buscarUsuarioPorEmail(emailGerente);
        validarPermissaoGerente(projetoId, gerente.getId());

        ProjetoUsuario pu = projetoUsuarioRepository.findByUsuarioIdAndProjetoId(usuarioId, projetoId)
                .orElseThrow(() -> new UsuarioException("Usuário não está no projeto."));

        Papel papel = projetoUsuarioRepository.findPapelByProjetoIdAndUsuarioId(projetoId, usuarioId);

        pu.getPermissoes().clear();
        pu.getPermissoes().addAll(papel.getPermissoes());

        projetoUsuarioRepository.save(pu);
    }

    private void validarPermissaoGerente(Long projetoId, Long usuarioId) {
        ProjetoUsuario membro = projetoUsuarioRepository.findByProjetoIdAndUsuarioId(projetoId, usuarioId)
                .orElseThrow(() -> new UsuarioException("Usuário não é membro do projeto."));

        if (membro.getCargo() != CargoProjeto.GERENTE)
            throw new PermissaoNegadaException("Somente o gerente pode fazer isso.");
    }

    private Usuario buscarUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsuarioException("Usuário autenticado não encontrado."));
    }

    private ProjetoResponseDTO converterParaDTO(Projeto projeto) {
        return new ProjetoResponseDTO(
                projeto.getId(),
                projeto.getNome(),
                projeto.getTipoEstimativa());
    }

    private MembroResponseDTO converterMembroParaDTO(ProjetoUsuario pu) {
        PapelProjeto papelEnum = null;
        if (pu.getPapel() != null) {
            try {
                papelEnum = PapelProjeto.deString(pu.getPapel().getNome());
            } catch (Exception ignored) {
            }
        }
        return new MembroResponseDTO(
                pu.getUsuario().getId(),
                pu.getUsuario().getNome(),
                pu.getUsuario().getEmail(),
                pu.getCargo(),
                papelEnum);
    }

    // cargo gerente e papel gerente sempre sao implementados juntos por enquanto
    // por enquanto quem chega nesse metodo ja é gerente
    private void atribuirCargoEPapel(Long projetoId, AtualizarMembroRequestDTO dto, ProjetoUsuario usuarioAlvo) {
        CargoProjeto cargoAtual = usuarioAlvo.getCargo();
        CargoProjeto novoCargo = dto.cargo() != null ? dto.cargo() : cargoAtual;
        boolean cargoMudou = novoCargo != cargoAtual;

        String papelAtual = usuarioAlvo.getPapel() != null ? usuarioAlvo.getPapel().getNome() : null;
        String papelDesejado;
        if (dto.papel() != null) {
            // por enquanto nao existe papel nao atribuivel mas pode mudar
            if (!dto.papel().isAtribuivel()) {
                throw new CargoException("Este papel não pode ser atribuído manualmente.");
            }
            papelDesejado = dto.papel().name();
            // garantir que um usuario nao seja promovido a um cargo gerente e continue com papel membro
            // por enquanto o papel sempre acompanha o cargo
        } else if (cargoMudou && novoCargo == CargoProjeto.GERENTE) {
            papelDesejado = PapelProjeto.GERENTE.name();
        } else if (cargoMudou && cargoAtual == CargoProjeto.GERENTE) {
            papelDesejado = PapelProjeto.MEMBRO.name();
        } else {
            papelDesejado = papelAtual;
        }

        validarCargoEPapel(novoCargo, papelDesejado);

        if (cargoMudou && cargoAtual == CargoProjeto.GERENTE) {
            verificarQuantidadeDeGerentes(projetoId);
        }

        if (cargoMudou) {
            usuarioAlvo.setCargo(novoCargo);
        }

        // so troca papel e permissoes se o papel realmente mudou
        if (papelDesejado != null && !papelDesejado.equalsIgnoreCase(papelAtual)) {
            Papel novoPapel = papelRepository.findByNome(papelDesejado)
                    .orElseThrow(() -> new RuntimeException("Papel inexistente."));
            List<Permissao> novasPermissoes = new ArrayList<>(novoPapel.getPermissoes());
            usuarioAlvo.setPapel(novoPapel);
            usuarioAlvo.getPermissoes().clear();
            usuarioAlvo.getPermissoes().addAll(novasPermissoes);
        }
    }

    private void validarCargoEPapel(CargoProjeto cargo, String nomePapel) {
        boolean cargoGerente = cargo == CargoProjeto.GERENTE;
        boolean papelGerente = PapelProjeto.GERENTE.name().equalsIgnoreCase(nomePapel);
        if (cargoGerente != papelGerente) {
            throw new CargoException("O cargo de Gerente exige o papel de gerente," +
                                        " e o papel de gerente só existe junto com o cargo de Gerente.");
        }
    }

    private void verificarPermissao(Long projetoId, Long usuarioId, PermissoesUsuarioProjeto permissao) {
        ProjetoUsuario membro = projetoUsuarioRepository.findByUsuarioIdAndProjetoId(usuarioId, projetoId)
                .orElseThrow(() -> new UsuarioException("Usuário não é membro do projeto."));

        // ATUALIZARPERMISSOES e ADICIONARMEMBROS sao exclusivas de gerente por enquanto
        // decisao de atualizar ou adicionar vem do cargo, nao das permissoes
        if (permissao == PermissoesUsuarioProjeto.ATUALIZARPERMISSOES
                || permissao == PermissoesUsuarioProjeto.ADICIONARMEMBROS) {
            if (membro.getCargo() != CargoProjeto.GERENTE)
                throw new PermissaoNegadaException("Apenas gerentes podem executar essa ação.");
            return;
        }

        List<Permissao> permissoes = membro.getPermissoes().isEmpty()
                ? new ArrayList<>(membro.getPapel().getPermissoes())
                : membro.getPermissoes();

        //////////////////////////
        boolean possuiPermissao = permissoes.stream()
                .anyMatch(p -> p.getDescricao().equals(permissao.name()));
        //////////////////////////

        if (!possuiPermissao)
            throw new PermissaoNegadaException("Você não possui a permissão " + permissao.name() + " neste projeto.");
    }

    private void verificarGerente(Long projetoId, ProjetoUsuario membro){
        boolean ehGerente = membro.getCargo() == CargoProjeto.GERENTE;
        if(!ehGerente){
            throw new PermissaoNegadaException("Apenas gerentes podem executar essa ação.");
        }
    }
    private void verificarQuantidadeDeGerentes(Long projetoId){
        // aqui apenas verifica, se nao houver problema o metodo so termina,
        long numeroGerentesProjeto = projetoUsuarioRepository.countByProjeto_IdAndCargo(projetoId, CargoProjeto.GERENTE);
        if(numeroGerentesProjeto <= 1){
            throw new UltimoGerenteException("Um projeto tem que possuir ao menos um gerente");
        }
    }
}

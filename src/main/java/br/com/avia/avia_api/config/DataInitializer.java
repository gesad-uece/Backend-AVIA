package br.com.avia.avia_api.config;

import br.com.avia.avia_api.entity.Papel;
import br.com.avia.avia_api.entity.Permissao;
import br.com.avia.avia_api.repository.PapelRepository;
import br.com.avia.avia_api.repository.PermissaoRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final String[] TODAS_PERMISSOES = {
        "CRIARTAREFAS", "EDITARTAREFAS", "EXCLUIRTAREFAS", "MOVERTAREFAS",
        "CRIARCOLUNAS", "EDITARESTRUTURA", "GERENCIARSPRINTS",
        "ADICIONARMEMBROS", "ATUALIZARPERMISSOES", "VISUALIZARPROJETO"
    };

    private static final Map<String, String[]> PAPEL_PERMISSOES = Map.of(
        "GERENTE", TODAS_PERMISSOES,
        "MEMBRO", new String[]{
            "CRIARTAREFAS", "EDITARTAREFAS", "MOVERTAREFAS", "VISUALIZARPROJETO"
        },
        "VISUALIZADOR", new String[]{
            "VISUALIZARPROJETO"
        }
    );

    private final PermissaoRepository permissaoRepository;
    private final PapelRepository papelRepository;

    public DataInitializer(PermissaoRepository permissaoRepository, PapelRepository papelRepository) {
        this.permissaoRepository = permissaoRepository;
        this.papelRepository = papelRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Ensure all permission records exist
        for (String descricao : TODAS_PERMISSOES) {
            if (permissaoRepository.findByDescricao(descricao) == null) {
                permissaoRepository.save(new Permissao(descricao));
            }
        }

        // Ensure each papel exists with correct permissions
        for (Map.Entry<String, String[]> entry : PAPEL_PERMISSOES.entrySet()) {
            String nomePapel = entry.getKey();
            String[] permissoesDoPapel = entry.getValue();

            Papel papel = papelRepository.findByNome(nomePapel).orElse(null);
            if (papel == null) {
                papel = new Papel(nomePapel);
            }

            List<Permissao> permissoes = new ArrayList<>();
            for (String descricao : permissoesDoPapel) {
                Permissao p = permissaoRepository.findByDescricao(descricao);
                if (p != null) permissoes.add(p);
            }

            papel.getPermissoes().clear();
            papel.getPermissoes().addAll(permissoes);
            papelRepository.save(papel);
        }
    }
}

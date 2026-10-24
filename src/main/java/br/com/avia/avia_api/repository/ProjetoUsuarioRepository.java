package br.com.avia.avia_api.repository;

import br.com.avia.avia_api.entity.Papel;
import br.com.avia.avia_api.entity.Projeto;
import br.com.avia.avia_api.entity.ProjetoUsuario;
import br.com.avia.avia_api.entity.Usuario;
import br.com.avia.avia_api.enums.CargoProjeto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ProjetoUsuarioRepository extends JpaRepository<ProjetoUsuario, Long> {
    // Lista os projetos do usuário
    @Query("SELECT pu.projeto FROM ProjetoUsuario pu WHERE pu.usuario.id = :usuarioId")
    List<Projeto> findProjetosByMembroId(@Param("usuarioId") Long usuarioId);

    // Lista os membros de um projeto
    @Query("SELECT pu FROM ProjetoUsuario pu JOIN FETCH pu.usuario LEFT JOIN FETCH pu.papel WHERE pu.projeto.id = :projetoId")
    List<ProjetoUsuario> findMembrosByProjetoId(@Param("projetoId") Long projetoId);

    Optional<ProjetoUsuario> findByProjetoIdAndUsuarioId(Long projetoId, Long usuarioId);

    @Query("SELECT pu.papel FROM ProjetoUsuario pu WHERE pu.projeto.id = :projetoId AND pu.usuario.id = :usuarioId")
    Papel findPapelByProjetoIdAndUsuarioId(@Param("projetoId") Long projetoId,
                                           @Param("usuarioId") Long usuarioId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ProjetoUsuario pu WHERE pu.projeto.id = :projetoId")
    void deleteByProjetoId(@Param("projetoId") Long projetoId);

    @Transactional
    @Query("SELECT pu FROM ProjetoUsuario pu WHERE pu.usuario.id = :usuarioId AND pu.projeto.id = :projetoId")
    Optional<ProjetoUsuario> findByUsuarioIdAndProjetoId(@Param("usuarioId") Long usuarioId, @Param("projetoId") Long projetoId);

    @Query("SELECT pu FROM ProjetoUsuario pu WHERE pu.projeto.id = :projetoId AND LOWER(pu.usuario.email) = LOWER(:email)")
    Optional<ProjetoUsuario> findByProjetoIdAndUsuarioEmail(@Param("projetoId") Long projetoId, @Param("email") String email);

    long countByProjeto_IdAndCargo(Long projetoId, CargoProjeto cargoProjeto);
    boolean existsByProjetoIdAndUsuarioId(Long projetoId, Long usuarioId);
}

package br.gov.mandaguari.saude.autorizacao.repository;

import br.gov.mandaguari.saude.autorizacao.domain.UsuarioPermissao;
import br.gov.mandaguari.saude.autorizacao.domain.UsuarioPermissaoId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsuarioPermissaoRepository extends JpaRepository<UsuarioPermissao, UsuarioPermissaoId> {

    /** All program permissions for a user (the per-user grid; fallback tier). */
    List<UsuarioPermissao> findByUsuarioId(Integer usuarioId);
}

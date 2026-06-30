package br.gov.mandaguari.saude.autorizacao.repository;

import br.gov.mandaguari.saude.autorizacao.domain.PerfilPermissao;
import br.gov.mandaguari.saude.autorizacao.domain.PerfilPermissaoId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PerfilPermissaoRepository extends JpaRepository<PerfilPermissao, PerfilPermissaoId> {

    /** All program permissions for a profile (the per-profile grid). */
    List<PerfilPermissao> findByPerfilId(Integer perfilId);
}

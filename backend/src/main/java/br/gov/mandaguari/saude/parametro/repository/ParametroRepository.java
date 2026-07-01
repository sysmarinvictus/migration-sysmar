package br.gov.mandaguari.saude.parametro.repository;

import br.gov.mandaguari.saude.parametro.domain.Parametro;
import org.springframework.data.jpa.repository.JpaRepository;

/** SAU_PAR singleton config (keyed by empresa/tenant code ParEmpCod). */
public interface ParametroRepository extends JpaRepository<Parametro, Integer> {
}

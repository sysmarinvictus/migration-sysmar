package br.gov.mandaguari.saude.programa.repository;

import br.gov.mandaguari.saude.programa.domain.GrupoPrograma;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrupoProgramaRepository extends JpaRepository<GrupoPrograma, Integer> {
}

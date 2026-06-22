package br.gov.mandaguari.saude.unidade.repository;

import br.gov.mandaguari.saude.unidade.domain.Unidade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UnidadeRepository extends JpaRepository<Unidade, Integer> {

    /** R16: next UniCod from the dedicated sequence (seeded from MAX(UniCod)+1 in V4). */
    @Query(value = "select nextval('seq_sau_uni_cod')", nativeQuery = true)
    Integer nextCodigo();

    Page<Unidade> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    @Query("select u from Unidade u where lower(coalesce(u.nome,'')) like lower(concat('%',:q,'%')) order by u.nome")
    List<Unidade> lookup(@Param("q") String q, Pageable pageable);

    // FK existence checks
    @Query(value = "select count(*) > 0 from SYS_MUN where MunCod = :cod", nativeQuery = true)
    boolean municipioExists(@Param("cod") Integer cod);

    @Query(value = "select count(*) > 0 from SAU_DIS where DisCod = :cod", nativeQuery = true)
    boolean distritoExists(@Param("cod") Short cod);

    @Query(value = "select count(*) > 0 from SAU_TIPUNI where TipUniCod = :cod", nativeQuery = true)
    boolean tipoUnidadeExists(@Param("cod") Integer cod);

    // Delete guards — 14 tables reference SAU_UNI (T000E61–T000E74 from sau_uni_impl.java)
    @Query(value = "select count(*) > 0 from SAU_PROESP1 where ProEspUniCod = :cod", nativeQuery = true)
    boolean isReferencedByProEsp1(@Param("cod") Integer cod);

    @Query(value = "select count(*) > 0 from SAU_PAR5 where ParSalUniCod = :cod", nativeQuery = true)
    boolean isReferencedByPar5Sal(@Param("cod") Integer cod);

    @Query(value = "select count(*) > 0 from SAU_PAR5 where ParSolUniCod = :cod", nativeQuery = true)
    boolean isReferencedByPar5Sol(@Param("cod") Integer cod);

    @Query(value = "select count(*) > 0 from SAU_USUUNI where UniUsuCod = :cod", nativeQuery = true)
    boolean isReferencedByUsuUni(@Param("cod") Integer cod);

    @Query(value = "select count(*) > 0 from SAU_USU where UsuUniCod = :cod", nativeQuery = true)
    boolean isReferencedByUsu(@Param("cod") Integer cod);

    @Query(value = "select count(*) > 0 from SAU_REM1 where RemUniCod = :cod", nativeQuery = true)
    boolean isReferencedByRem1(@Param("cod") Integer cod);

    @Query(value = "select count(*) > 0 from SAU_REM_UNISETOR where RemUniSetorUniCod = :cod", nativeQuery = true)
    boolean isReferencedByRemUnisetor(@Param("cod") Integer cod);

    @Query(value = "select count(*) > 0 from SAU_PAC where PacPesCadAltUniCod = :cod", nativeQuery = true)
    boolean isReferencedByPacAlt(@Param("cod") Integer cod);

    @Query(value = "select count(*) > 0 from SAU_PAC where PacPesCadInsUniCod = :cod", nativeQuery = true)
    boolean isReferencedByPacIns(@Param("cod") Integer cod);

    @Query(value = "select count(*) > 0 from SAU_PAC where PacUniCod = :cod", nativeQuery = true)
    boolean isReferencedByPac(@Param("cod") Integer cod);

    @Query(value = "select count(*) > 0 from SAU_PAR2 where ParAgendDesUniCod = :cod", nativeQuery = true)
    boolean isReferencedByPar2Des(@Param("cod") Integer cod);

    @Query(value = "select count(*) > 0 from SAU_PAR2 where ParAgendUniCod = :cod", nativeQuery = true)
    boolean isReferencedByPar2Agend(@Param("cod") Integer cod);

    @Query(value = "select count(*) > 0 from SAU_UNISETOR where UniCod = :cod", nativeQuery = true)
    boolean isReferencedByUnisetor(@Param("cod") Integer cod);

    @Query(value = "select count(*) > 0 from SAU_RECESP where RecEspUniCod = :cod", nativeQuery = true)
    boolean isReferencedByRecesp(@Param("cod") Integer cod);
}

package br.gov.mandaguari.saude.medicamento.repository;

import br.gov.mandaguari.saude.medicamento.domain.Medicamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MedicamentoRepository
        extends JpaRepository<Medicamento, Integer>, JpaSpecificationExecutor<Medicamento> {

    /** R15: next RemCod from the dedicated sequence (seeded from MAX(RemCod)+1 in V5). */
    @Query(value = "select nextval('seq_sau_rem_cod')", nativeQuery = true)
    Integer nextCodigo();

    @Query("select m from Medicamento m where lower(coalesce(m.nome,'')) like lower(concat('%',:q,'%')) order by m.nome")
    List<Medicamento> lookup(@Param("q") String q, Pageable pageable);

    Page<Medicamento> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    // ── FK existence checks ───────────────────────────────────────────────────
    @Query(value = "select count(*) > 0 from SAU_TIPREM where TipRemCod = :c", nativeQuery = true)
    boolean tipoMedicamentoExists(@Param("c") Integer c);                    // R2

    @Query(value = "select count(*) > 0 from SAU_DCB where DcbCod = :c", nativeQuery = true)
    boolean dcbExists(@Param("c") String c);                                 // R3

    @Query(value = "select count(*) > 0 from SAU_APRREM where AprRemCod = :c", nativeQuery = true)
    boolean apresentacaoExists(@Param("c") Integer c);                       // R4

    @Query(value = "select count(*) > 0 from SAU_RENAMEATUALIZADO where RenameAtualCod = :c", nativeQuery = true)
    boolean renameAtualExists(@Param("c") String c);                         // R5

    @Query(value = "select count(*) > 0 from SAU_RENAME where RENAMECod = :c", nativeQuery = true)
    boolean renameExists(@Param("c") String c);                             // R6

    @Query(value = "select count(*) > 0 from SAU_RENAME_DEPARA where RENAMECod = :r and RenameAtualCod = :a", nativeQuery = true)
    boolean renameDeparaExists(@Param("r") String renameCod, @Param("a") String renameAtualCod); // R7

    @Query(value = "select count(*) > 0 from OBM where ObmCod = :c", nativeQuery = true)
    boolean obmExists(@Param("c") String c);                                 // R8

    @Query(value = "select count(*) > 0 from SAU_UNI where UniCod = :c", nativeQuery = true)
    boolean unidadeExists(@Param("c") Integer c);                            // R28, R32

    @Query(value = "select count(*) > 0 from SAU_UNISETOR where UniCod = :u and SetorCod = :s", nativeQuery = true)
    boolean unidadeSetorExists(@Param("u") Integer uniCod, @Param("s") Integer setorCod); // R29

    @Query(value = "select count(*) > 0 from SAU_REMOBS where RemObsCod = :c", nativeQuery = true)
    boolean posologiaExists(@Param("c") Integer c);                          // R34

    // ── R9/R10 RENAME-atualizado flags; R12 RENAME display ────────────────────
    @Query(value = "select RenameAtualBasico as basico, RenameAtualEstrategico as estrategico, "
            + "RenameAtualProprio as proprio, RenameAtualEspecializado as especializado "
            + "from SAU_RENAMEATUALIZADO where RenameAtualCod = :c", nativeQuery = true)
    Optional<RenameAtualFlags> renameAtualFlags(@Param("c") String renameAtualCod);

    @Query(value = "select RENAMEPrincAt as principioAtivo, RENAMEConc as concentracao, "
            + "RENAMEFormFarm as formaFarmaceutica, RENAMEVol as volume, RENAMEUnd as unidade "
            + "from SAU_RENAME where RENAMECod = :c", nativeQuery = true)
    Optional<RenameDisplay> renameDisplay(@Param("c") String renameCod);

    // ── Delete guards (parent) ────────────────────────────────────────────────
    @Query(value = "select count(*) > 0 from InteracaoMedicamentosa where InteraRemCod1 = :c", nativeQuery = true)
    boolean isReferencedByInteracaoRem1(@Param("c") Integer remCod);         // R20

    @Query(value = "select count(*) > 0 from InteracaoMedicamentosa where InteraRemCod2 = :c", nativeQuery = true)
    boolean isReferencedByInteracaoRem2(@Param("c") Integer remCod);         // R19

    @Query(value = "select count(*) > 0 from SAU_RECESP1 where RemCod = :c", nativeQuery = true)
    boolean isReferencedByRecespItens(@Param("c") Integer remCod);           // R21, R53

    @Query(value = "select count(*) > 0 from SAU_REMLOT where RemCod = :c", nativeQuery = true)
    boolean isReferencedByRemlot(@Param("c") Integer remCod);                // R22

    @Query(value = "select count(*) > 0 from SAU_REMLOT where RemCod = :r and RemUniCod = :u", nativeQuery = true)
    boolean isRem1ReferencedByRemlot(@Param("r") Integer remCod, @Param("u") Integer uniCod); // R27
}

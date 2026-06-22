package br.gov.mandaguari.saude.medicamento.repository;

/** Projection of SAU_RENAMEATUALIZADO product-type flags — used by R9 (compatibility) and R10 (derivation). */
public interface RenameAtualFlags {
    Boolean getBasico();
    Boolean getEstrategico();
    Boolean getProprio();
    Boolean getEspecializado();
}

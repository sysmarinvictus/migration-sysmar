package br.gov.mandaguari.saude.medicamento.domain;

import jakarta.persistence.*;

/** SAU_REM2 — Código de Barras (EAN-13). PK (RemCod, RemEan13); no non-key columns. */
@Entity
@Table(name = "SAU_REM2")
@IdClass(MedicamentoEan13Id.class)
public class MedicamentoEan13 {

    @Id @Column(name = "RemCod", nullable = false) private Integer remCod;
    @Id @Column(name = "RemEan13", nullable = false) private Long ean13;

    public Integer getRemCod() { return remCod; }
    public void setRemCod(Integer v) { this.remCod = v; }
    public Long getEan13() { return ean13; }
    public void setEan13(Long v) { this.ean13 = v; }
}

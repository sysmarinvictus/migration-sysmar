package br.gov.mandaguari.saude.medicamento.domain;

import jakarta.persistence.*;

/** SAU_REMPOSO — junction Medicamento ↔ Posologia. PK (RemCod, PosoRemObsCod); no non-key columns. */
@Entity
@Table(name = "SAU_REMPOSO")
@IdClass(MedicamentoPosologiaId.class)
public class MedicamentoPosologia {

    @Id @Column(name = "RemCod", nullable = false) private Integer remCod;
    @Id @Column(name = "PosoRemObsCod", nullable = false) private Integer posologiaCodigo;

    public Integer getRemCod() { return remCod; }
    public void setRemCod(Integer v) { this.remCod = v; }
    public Integer getPosologiaCodigo() { return posologiaCodigo; }
    public void setPosologiaCodigo(Integer v) { this.posologiaCodigo = v; }
}

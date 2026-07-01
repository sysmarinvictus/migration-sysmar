package br.gov.mandaguari.saude.profissionalespecialidade.domain;

import java.io.Serializable;
import java.util.Objects;

/** Composite PK for SAU_PROESP: (ProPesCod, EspCod). */
public class ProfissionalEspecialidadeId implements Serializable {

    private Long profissionalId;
    private Integer especialidadeId;

    public ProfissionalEspecialidadeId() {}

    public ProfissionalEspecialidadeId(Long profissionalId, Integer especialidadeId) {
        this.profissionalId = profissionalId;
        this.especialidadeId = especialidadeId;
    }

    public Long getProfissionalId() { return profissionalId; }
    public void setProfissionalId(Long profissionalId) { this.profissionalId = profissionalId; }
    public Integer getEspecialidadeId() { return especialidadeId; }
    public void setEspecialidadeId(Integer especialidadeId) { this.especialidadeId = especialidadeId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProfissionalEspecialidadeId that)) return false;
        return Objects.equals(profissionalId, that.profissionalId)
                && Objects.equals(especialidadeId, that.especialidadeId);
    }

    @Override
    public int hashCode() { return Objects.hash(profissionalId, especialidadeId); }
}

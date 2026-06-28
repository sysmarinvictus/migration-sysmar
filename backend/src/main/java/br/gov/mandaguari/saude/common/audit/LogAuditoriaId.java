package br.gov.mandaguari.saude.common.audit;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Composite primary key for {@link LogAuditoria} — {@code (logempcod, logdat, logusucod, logkey)}.
 *
 * <p>Plain mutable POJO with a no-arg constructor (required by Hibernate {@code @IdClass}),
 * {@code equals}/{@code hashCode} over all four members.
 */
public class LogAuditoriaId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer empresaCodigo;
    private LocalDateTime dataHora;
    private Integer usuarioCodigo;
    private String chaveRegistro;

    public LogAuditoriaId() {}

    public LogAuditoriaId(Integer empresaCodigo, LocalDateTime dataHora,
                          Integer usuarioCodigo, String chaveRegistro) {
        this.empresaCodigo = empresaCodigo;
        this.dataHora = dataHora;
        this.usuarioCodigo = usuarioCodigo;
        this.chaveRegistro = chaveRegistro;
    }

    public Integer getEmpresaCodigo() { return empresaCodigo; }
    public void setEmpresaCodigo(Integer empresaCodigo) { this.empresaCodigo = empresaCodigo; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public Integer getUsuarioCodigo() { return usuarioCodigo; }
    public void setUsuarioCodigo(Integer usuarioCodigo) { this.usuarioCodigo = usuarioCodigo; }
    public String getChaveRegistro() { return chaveRegistro; }
    public void setChaveRegistro(String chaveRegistro) { this.chaveRegistro = chaveRegistro; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LogAuditoriaId that)) return false;
        return Objects.equals(empresaCodigo, that.empresaCodigo)
                && Objects.equals(dataHora, that.dataHora)
                && Objects.equals(usuarioCodigo, that.usuarioCodigo)
                && Objects.equals(chaveRegistro, that.chaveRegistro);
    }

    @Override
    public int hashCode() {
        return Objects.hash(empresaCodigo, dataHora, usuarioCodigo, chaveRegistro);
    }
}

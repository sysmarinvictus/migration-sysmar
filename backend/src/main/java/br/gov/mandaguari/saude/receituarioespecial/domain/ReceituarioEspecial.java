package br.gov.mandaguari.saude.receituarioespecial.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

/**
 * Receituário Controle Especial — maps GeneXus {@code SAU_RECESP} (master header, 1510 live rows). The
 * controlled-substance prescription (Portaria SVS/MS 344/98). Composite PK ({@code RecEspUniCod},
 * {@code RecEspCod}) where {@code RecEspCod} is a per-unit sequential number server-allocated on insert
 * (R1, {@code psau_inc_recesp}). The prescribed drugs live in the child {@code SAU_RECESP1}
 * ({@link ReceituarioEspecialItem}). Rules mined in {@code sau_recesp_impl.java} — cited {@code // R<n>}.
 *
 * <p>No {@code @ManyToOne}: the live DB declares NO FK constraints (GeneXus enforces in-app), so patient/
 * prescriber/issuer/unit links are raw ids validated by existence queries (R8-R11). Types are LIVE-verified:
 * smallint→Integer+@JdbcTypeCode(SMALLINT), char→trimmed, bigint→Long.
 *
 * <p><b>PHI (regulated):</b> patient + prescriber identity and the prescribed controlled substance.
 * {@link #toString()} emits only the composite id; PHI is never logged. Every read/write is audited (R28).
 */
@Entity
@Table(name = "SAU_RECESP")
@IdClass(ReceituarioEspecialId.class)
public class ReceituarioEspecial {

    @Id
    @Column(name = "RecEspUniCod", nullable = false)
    private Integer unidadeId;

    @Id
    @Column(name = "RecEspCod", nullable = false)
    private Long codigo;                        // per-unit sequential number (R1)

    @Column(name = "RecEspDat")
    private LocalDate data;                     // required (R4)

    @Column(name = "FunPesCod")
    private Long funcionarioCodigo;             // issuer → SAU_FUN (optional, R9)

    @Column(name = "PacPesCod")
    private Long pacienteCodigo;                // patient → SAU_PAC (required, R5; pac↔recesp cycle)

    @Column(name = "RecEspProPesCod")
    private Long prescritorCodigo;              // prescriber → SAU_PRO (required, R6)

    @Column(name = "RecEspSeqUlt")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer sequenciaUltima;            // last child seq counter (R26)

    @Column(name = "RecEspUsuLogin", length = 20)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String usuarioLogin;                // server-stamped from principal (R17)

    @Column(name = "RecEspCon")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer situacao;                   // status/confirm — pass-through, no state machine (R30)

    @Column(name = "RecTip")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer tipoReceituario;            // type — all 0 live, meaning unknown (Q1); pass-through (R30)

    @Column(name = "RecObs", length = 300)
    private String observacao;                  // header notes, ≤300 (R7)

    public ReceituarioEspecial() {}

    public Integer getUnidadeId() { return unidadeId; }
    public void setUnidadeId(Integer unidadeId) { this.unidadeId = unidadeId; }
    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
    public Long getFuncionarioCodigo() { return funcionarioCodigo; }
    public void setFuncionarioCodigo(Long v) { this.funcionarioCodigo = v; }
    public Long getPacienteCodigo() { return pacienteCodigo; }
    public void setPacienteCodigo(Long v) { this.pacienteCodigo = v; }
    public Long getPrescritorCodigo() { return prescritorCodigo; }
    public void setPrescritorCodigo(Long v) { this.prescritorCodigo = v; }
    public Integer getSequenciaUltima() { return sequenciaUltima; }
    public void setSequenciaUltima(Integer v) { this.sequenciaUltima = v; }
    public String getUsuarioLogin() { return usuarioLogin == null ? null : usuarioLogin.trim(); }
    public void setUsuarioLogin(String v) { this.usuarioLogin = v; }
    public Integer getSituacao() { return situacao; }
    public void setSituacao(Integer situacao) { this.situacao = situacao; }
    public Integer getTipoReceituario() { return tipoReceituario; }
    public void setTipoReceituario(Integer v) { this.tipoReceituario = v; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    /** Only the composite id — PHI is never logged. */
    @Override
    public String toString() { return "ReceituarioEspecial{uni=" + unidadeId + ", cod=" + codigo + "}"; }
}

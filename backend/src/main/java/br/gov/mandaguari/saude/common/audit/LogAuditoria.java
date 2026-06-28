package br.gov.mandaguari.saude.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.domain.Persistable;

import java.sql.Types;
import java.time.LocalDateTime;

/**
 * Audit-trail row — maps the existing GeneXus table {@code SAU_LOG} (the system's append-only audit
 * trail, ~7.7M rows in production). Replaces the legacy {@code psau_inc_log} write path.
 *
 * <p>Physical column names are pinned to the GeneXus names (SLICE-SPEC SAU_LOG, schema verified
 * against the live DB 2026-06-22). The PK is composite — {@code (logempcod, logdat, logusucod,
 * logkey)} — modelled via {@link LogAuditoriaId} + {@link IdClass}.
 *
 * <p><b>CHAR PK member:</b> {@code logkey} is {@code CHAR(50)} in the live DB; it MUST carry
 * {@code @JdbcTypeCode(Types.CHAR)} or the bpchar space-padding breaks composite-PK matching.
 *
 * <p><b>Append-only / pure INSERT:</b> the entity implements {@link Persistable} with
 * {@link #isNew()} always {@code true} so {@code save} always issues an {@code INSERT} — no
 * pre-insert SELECT, no accidental UPDATE (R11: legacy allowed UPDATE/DELETE; modern is append-only).
 *
 * <p><b>LGPD (R8):</b> the PHI columns ({@code lognomepaciente}, {@code logpacpescod},
 * {@code loghistorico}, {@code lognomeprofissional}, {@code logsituacao}) are left NULL by the write
 * path and are excluded from {@link #toString()}. No patient/professional names are ever stored.
 *
 * @see <a>SLICE-SPEC SAU_LOG — JPA mapping table</a>
 */
@Entity
@Table(name = "SAU_LOG")
@IdClass(LogAuditoriaId.class)
public class LogAuditoria implements Persistable<LogAuditoriaId> {

    // ── Composite primary key (logempcod, logdat, logusucod, logkey) ──

    @Id
    @Column(name = "logempcod", nullable = false)
    private Integer empresaCodigo;

    @Id
    @Column(name = "logdat", nullable = false)
    private LocalDateTime dataHora;

    @Id
    @Column(name = "logusucod", nullable = false)
    private Integer usuarioCodigo;

    /** PK member; CHAR(50) → @JdbcTypeCode(CHAR) is mandatory (R5/R6 / CHAR-padding lesson). */
    @Id
    @JdbcTypeCode(Types.CHAR)
    @Column(name = "logkey", nullable = false, length = 50)
    private String chaveRegistro;

    // ── Real payload (R1: only these carry real data on the modern write path) ──

    @Column(name = "logope", length = 3)
    private String operacao;                 // R4: INS/UPD/DLT (mapped); pass-through 3-char else

    @Column(name = "logtab", nullable = false, length = 31)
    private String tabela;                   // R5: program/transaction name (e.g. SAU_ESP), uppercased

    @Column(name = "usucod")
    private Integer usuarioCodigoRef;        // R1: legacy duplicates LogUsuCod here; index isau_log5

    // ── R7 fix: real ids when known, else NULL (never the legacy hardcoded 0) ──

    @Column(name = "logpropescod")
    private Long profissionalCodigo;

    @Column(name = "logunicod")
    private Integer unidadeCodigo;

    // ── R8 / LGPD: PHI + free-text columns — write path keeps these NULL, never names ──

    @Column(name = "logsituacao", length = 50)
    private String situacao;

    @Column(name = "loghistorico")
    private String historico;                // PHI — NULL on write

    @Column(name = "lognomeprofissional", length = 50)
    private String nomeProfissional;         // NULL on write

    @Column(name = "lognomepaciente", length = 50)
    private String nomePaciente;             // PHI — NULL on write

    @Column(name = "logpacpescod")
    private Long pacienteCodigo;             // PHI — NULL on write

    @Transient
    private transient boolean persisted = false;

    public LogAuditoria() {} // JPA

    /**
     * Always reports {@code true} unless explicitly cleared, so Spring Data issues a pure INSERT.
     * (The composite, time-based PK is generated per-event; there is never an existing row to load.)
     */
    @Override
    public boolean isNew() {
        return !persisted;
    }

    @Override
    public LogAuditoriaId getId() {
        return new LogAuditoriaId(empresaCodigo, dataHora, usuarioCodigo, chaveRegistro);
    }

    // --- getters / setters ---
    public Integer getEmpresaCodigo() { return empresaCodigo; }
    public void setEmpresaCodigo(Integer empresaCodigo) { this.empresaCodigo = empresaCodigo; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public Integer getUsuarioCodigo() { return usuarioCodigo; }
    public void setUsuarioCodigo(Integer usuarioCodigo) { this.usuarioCodigo = usuarioCodigo; }
    public String getChaveRegistro() { return chaveRegistro; }
    public void setChaveRegistro(String chaveRegistro) { this.chaveRegistro = chaveRegistro; }
    public String getOperacao() { return operacao; }
    public void setOperacao(String operacao) { this.operacao = operacao; }
    public String getTabela() { return tabela; }
    public void setTabela(String tabela) { this.tabela = tabela; }
    public Integer getUsuarioCodigoRef() { return usuarioCodigoRef; }
    public void setUsuarioCodigoRef(Integer usuarioCodigoRef) { this.usuarioCodigoRef = usuarioCodigoRef; }
    public Long getProfissionalCodigo() { return profissionalCodigo; }
    public void setProfissionalCodigo(Long profissionalCodigo) { this.profissionalCodigo = profissionalCodigo; }
    public Integer getUnidadeCodigo() { return unidadeCodigo; }
    public void setUnidadeCodigo(Integer unidadeCodigo) { this.unidadeCodigo = unidadeCodigo; }
    public String getSituacao() { return situacao; }
    public void setSituacao(String situacao) { this.situacao = situacao; }
    public String getHistorico() { return historico; }
    public void setHistorico(String historico) { this.historico = historico; }
    public String getNomeProfissional() { return nomeProfissional; }
    public void setNomeProfissional(String nomeProfissional) { this.nomeProfissional = nomeProfissional; }
    public String getNomePaciente() { return nomePaciente; }
    public void setNomePaciente(String nomePaciente) { this.nomePaciente = nomePaciente; }
    public Long getPacienteCodigo() { return pacienteCodigo; }
    public void setPacienteCodigo(Long pacienteCodigo) { this.pacienteCodigo = pacienteCodigo; }
    public boolean isPersisted() { return persisted; }
    public void setPersisted(boolean persisted) { this.persisted = persisted; }

    /** LGPD: deliberately excludes all PHI/free-text columns (no names, no history, no patient id). */
    @Override
    public String toString() {
        return "LogAuditoria{empresaCodigo=" + empresaCodigo
                + ", dataHora=" + dataHora
                + ", usuarioCodigo=" + usuarioCodigo
                + ", operacao=" + operacao
                + ", tabela=" + tabela
                + ", chaveRegistro=" + (chaveRegistro == null ? null : chaveRegistro.trim())
                + ", profissionalCodigo=" + profissionalCodigo
                + ", unidadeCodigo=" + unidadeCodigo + '}';
    }
}

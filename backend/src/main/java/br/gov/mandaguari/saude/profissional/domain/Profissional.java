package br.gov.mandaguari.saude.profissional.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Types;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;

/**
 * Profissional / prescritor — maps the GeneXus physical table {@code SAU_PRO} (16 stored columns).
 * Physical names pinned via {@code @Column}; {@code ddl-auto=validate} never alters the schema.
 *
 * <p><b>PK = SYS_PES person code.</b> {@code ProPesCod} is user-supplied (an existing Pessoa becomes a
 * Profissional) — there is NO {@code @GeneratedValue}. Person fields (name/CPF/phones/address) live in
 * the un-migrated SYS_PES supertype and are read/written via native queries (see repository).
 * {@code conselhoClasseCod} is a raw {@link Short} with a 0/null sentinel (NOT a {@code @ManyToOne}).
 *
 * <p><b>Security / v1 scope (non-negotiable, SLICE-SPEC §Security):</b> this entity still MAPS the
 * three sensitive columns so {@code validate} passes, but the v1 API surface (create/update DTOs and
 * service) neither accepts nor returns them:
 * <ul>
 *   <li>{@code certificadoSenha} — {@code @JsonIgnore} + AES-GCM encrypted at rest via
 *       {@link CertificadoSenhaCryptoConverter}; never logged/serialized (R31 defect NOT ported).</li>
 *   <li>{@code certificado} / {@code assinaturaImagem} — LAZY bytea blobs, excluded from every response
 *       DTO; a future audited/ACL'd sub-feature handles upload/serving (OQ2).</li>
 * </ul>
 * These columns stay null/untouched in v1. PHI/secret fields are excluded from {@link #toString()}.
 */
@Entity
@Table(name = "SAU_PRO")
public class Profissional {

    /** = SYS_PES.PesCod. User-supplied; NOT generated (R1). */
    @Id
    @Column(name = "ProPesCod", nullable = false)
    private Long id;

    /** CHAR(20). CNS — required (R3), 15-digit validated (R4), unique across people (R5). PHI. */
    @Column(name = "ProPesNumCns", length = 20)
    @JdbcTypeCode(Types.CHAR)
    private String numeroCns;

    /** Phonetic key recomputed server-side from SYS_PES.PesNom on every confirm (R15). Read-only. */
    @Column(name = "ProPesNomSoundex", length = 50)
    private String nomeSoundex;

    /** CHAR(20). Council registration nº — no format/required validation in legacy (R14). PHI. */
    @Column(name = "ProNumCr", length = 20)
    @JdbcTypeCode(Types.CHAR)
    private String numeroCr;

    @Column(name = "ProDatIni")
    private LocalDate dataInicio;

    @Column(name = "ProDatFim")
    private LocalDate dataFim;

    @Column(name = "ProScnesId", length = 20)
    private String cnesId;

    /** Real PG boolean. Defaults false on INS (R13). */
    @Column(name = "ProExpeSus")
    private Boolean exportaEsus;

    /** Defaults 0 on INS (R13). Routes After-Trn (R32). */
    @Column(name = "ProExt")
    @JdbcTypeCode(Types.SMALLINT)
    private Short externo;

    /** 1=ATIVO (default on INS, R12) / 2=INATIVO. */
    @Column(name = "ProSit")
    @JdbcTypeCode(Types.SMALLINT)
    private Short situacao;

    /** Signature image bytes. SECURITY: LAZY + excluded from all v1 DTOs (OQ2). */
    @Column(name = "AssinaturaImagem")
    @JdbcTypeCode(Types.VARBINARY)
    @Basic(fetch = FetchType.LAZY)
    @JsonIgnore
    private byte[] assinaturaImagem;

    @Column(name = "AssinaturaImagemTipo", length = 3)
    @JdbcTypeCode(Types.CHAR)
    private String assinaturaImagemTipo;

    /** Raw lookup id; 0/null = none (377 rows have 0). Must exist when !=0 (R10). NOT a @ManyToOne. */
    @Column(name = "ConClaCod")
    @JdbcTypeCode(Types.SMALLINT)
    private Short conselhoClasseCod;

    @Column(name = "ProUfConselho", length = 2)
    private String ufConselho;

    /** ICP-Brasil signing certificate. SECURITY: LAZY + excluded from all v1 DTOs (OQ2). */
    @Column(name = "ProCertificado")
    @JdbcTypeCode(Types.VARBINARY)
    @Basic(fetch = FetchType.LAZY)
    @JsonIgnore
    private byte[] certificado;

    /**
     * ⚠ Cleartext-in-legacy certificate password. SECURITY: {@code @JsonIgnore}; AES-GCM encrypted at
     * rest via {@link CertificadoSenhaCryptoConverter}; NEVER logged or returned (R31 NOT ported).
     */
    @Column(name = "ProCertificadoSenha", length = 255)
    @Convert(converter = CertificadoSenhaCryptoConverter.class)
    @JsonIgnore
    private String certificadoSenha;

    public Profissional() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumeroCns() { return numeroCns; }
    public void setNumeroCns(String numeroCns) { this.numeroCns = numeroCns; }
    public String getNomeSoundex() { return nomeSoundex; }
    public void setNomeSoundex(String nomeSoundex) { this.nomeSoundex = nomeSoundex; }
    public String getNumeroCr() { return numeroCr; }
    public void setNumeroCr(String numeroCr) { this.numeroCr = numeroCr; }
    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }
    public String getCnesId() { return cnesId; }
    public void setCnesId(String cnesId) { this.cnesId = cnesId; }
    public Boolean getExportaEsus() { return exportaEsus; }
    public void setExportaEsus(Boolean exportaEsus) { this.exportaEsus = exportaEsus; }
    public Short getExterno() { return externo; }
    public void setExterno(Short externo) { this.externo = externo; }
    public Short getSituacao() { return situacao; }
    public void setSituacao(Short situacao) { this.situacao = situacao; }
    public byte[] getAssinaturaImagem() { return assinaturaImagem; }
    public void setAssinaturaImagem(byte[] assinaturaImagem) { this.assinaturaImagem = assinaturaImagem; }
    public String getAssinaturaImagemTipo() { return assinaturaImagemTipo; }
    public void setAssinaturaImagemTipo(String assinaturaImagemTipo) { this.assinaturaImagemTipo = assinaturaImagemTipo; }
    public Short getConselhoClasseCod() { return conselhoClasseCod; }
    public void setConselhoClasseCod(Short conselhoClasseCod) { this.conselhoClasseCod = conselhoClasseCod; }
    public String getUfConselho() { return ufConselho; }
    public void setUfConselho(String ufConselho) { this.ufConselho = ufConselho; }
    public byte[] getCertificado() { return certificado; }
    public void setCertificado(byte[] certificado) { this.certificado = certificado; }
    public String getCertificadoSenha() { return certificadoSenha; }
    public void setCertificadoSenha(String certificadoSenha) { this.certificadoSenha = certificadoSenha; }

    /** Excludes PHI (numeroCns, numeroCr, nomeSoundex) and ALL sensitive fields. */
    @Override
    public String toString() {
        return "Profissional{id=" + id + ", conselhoClasseCod=" + conselhoClasseCod
                + ", situacao=" + situacao + ", externo=" + externo + "}";
    }
}

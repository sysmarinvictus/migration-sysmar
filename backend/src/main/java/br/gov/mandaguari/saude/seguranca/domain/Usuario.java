package br.gov.mandaguari.saude.seguranca.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Usuário do Sistema — maps the existing GeneXus table {@code SAU_USU} (110 columns, 1230 rows, PK
 * {@code UsuCod}, ZERO physical FKs). Authentication/authorization keystone (Wave-0).
 *
 * <p><b>Focused mapping (SLICE-SPEC SAU_USU):</b> only the 16 auth-essential columns are mapped here.
 * The ~94 remaining flag columns (usubloq* module bitmap, the 16 LGPD usupesquisa* patient-search
 * permissions, usubio, behaviour toggles) are deliberately UNMAPPED — {@code ddl-auto=validate} only
 * checks mapped columns, so the entity validates fine against the full 110-column table. They will be
 * brought in as {@code @Embeddable} value objects when an authorization slice needs them.
 *
 * <p><b>Secrets ({@code UsuSen}/{@code UsuKey}):</b> {@code @JsonIgnore} and excluded from
 * {@link #toString()}. The legacy scheme is REVERSIBLE encryption ({@code encrypt64(text, usukey)}),
 * NOT a hash — never log or return these. After the offline password-bridge runs, {@code UsuSen}
 * holds a bcrypt hash and {@code UsuKey} is NULL ("migrated"); a non-null {@code UsuKey} means the
 * user has not been migrated and cannot log in via the modern app (must reset password).
 *
 * <p><b>No {@code @ManyToOne}:</b> dependency tables (SAU_PRF/SAU_PRO/SAU_FUN) are not migrated, so
 * FKs are kept as raw scalar ids per the schema-mapper plan.
 *
 * <p>PHI: {@code UsuNom} is PII and is excluded from {@link #toString()}.
 */
@Entity
@Table(name = "SAU_USU")
public class Usuario {

    @Id
    @Column(name = "UsuCod", nullable = false)
    private Integer usuCod;

    /** PII (phi) — excluded from toString(). */
    @Column(name = "UsuNom", length = 50)
    private String nome;

    /** Login key. NOT UNIQUE in the DB (only the non-unique index usau_usu) → uniqueness in service (R13). */
    @Column(name = "UsuLogin", length = 20)
    private String login;

    /** 🔒 Secret. bcrypt hash once bridged (UsuKey NULL); else legacy reversible ciphertext. */
    @JsonIgnore
    @Column(name = "UsuSen", length = 100)
    private String senha;

    /** 🔒 Secret. Per-user symmetric key (legacy). NULL once the password has been bridged to bcrypt. */
    @JsonIgnore
    @Column(name = "UsuKey", length = 100)
    private String chaveSenha;

    @Column(name = "UsuTip")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Short tipo;

    /** 1 = blocked (R5). */
    @Column(name = "UsuBloq")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Short bloqueado;

    /** Perfil/role id — raw id (SAU_PRF not migrated). */
    @Column(name = "UsuPrfCod")
    private Integer perfilId;

    /** ⚠ Superuser bypass flag (R2). */
    @Column(name = "UsuSysmar")
    private Boolean superusuario;

    @Column(name = "UsuProPesCod")
    private Long profissionalId;

    @Column(name = "FunPesCod")
    private Long funcionarioId;

    @Column(name = "UsuTokenSoa", length = 5000)
    private String tokenSoa;

    @Column(name = "UsuTokenExp")
    private Integer tokenExpiracao;

    @Column(name = "UsuTokenData")
    private LocalDateTime tokenData;

    @Column(name = "UsuDataUltimoAcesso")
    private LocalDate ultimoAcesso;

    /** Password-reset stamp (R11). */
    @Column(name = "UsuDataRedefinicao")
    private LocalDate dataRedefinicaoSenha;

    public Usuario() {} // JPA + service instantiation (different package)

    // --- getters / setters ---
    public Integer getUsuCod() { return usuCod; }
    public void setUsuCod(Integer usuCod) { this.usuCod = usuCod; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public String getChaveSenha() { return chaveSenha; }
    public void setChaveSenha(String chaveSenha) { this.chaveSenha = chaveSenha; }
    public Short getTipo() { return tipo; }
    public void setTipo(Short tipo) { this.tipo = tipo; }
    public Short getBloqueado() { return bloqueado; }
    public void setBloqueado(Short bloqueado) { this.bloqueado = bloqueado; }
    public Integer getPerfilId() { return perfilId; }
    public void setPerfilId(Integer perfilId) { this.perfilId = perfilId; }
    public Boolean getSuperusuario() { return superusuario; }
    public void setSuperusuario(Boolean superusuario) { this.superusuario = superusuario; }
    public Long getProfissionalId() { return profissionalId; }
    public void setProfissionalId(Long profissionalId) { this.profissionalId = profissionalId; }
    public Long getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Long funcionarioId) { this.funcionarioId = funcionarioId; }
    public String getTokenSoa() { return tokenSoa; }
    public void setTokenSoa(String tokenSoa) { this.tokenSoa = tokenSoa; }
    public Integer getTokenExpiracao() { return tokenExpiracao; }
    public void setTokenExpiracao(Integer tokenExpiracao) { this.tokenExpiracao = tokenExpiracao; }
    public LocalDateTime getTokenData() { return tokenData; }
    public void setTokenData(LocalDateTime tokenData) { this.tokenData = tokenData; }
    public LocalDate getUltimoAcesso() { return ultimoAcesso; }
    public void setUltimoAcesso(LocalDate ultimoAcesso) { this.ultimoAcesso = ultimoAcesso; }
    public LocalDate getDataRedefinicaoSenha() { return dataRedefinicaoSenha; }
    public void setDataRedefinicaoSenha(LocalDate dataRedefinicaoSenha) { this.dataRedefinicaoSenha = dataRedefinicaoSenha; }

    /** Secrets (UsuSen/UsuKey) and PII (UsuNom) are intentionally excluded. */
    @Override
    public String toString() {
        return "Usuario{usuCod=" + usuCod + ", login='" + login + "', bloqueado=" + bloqueado
                + ", superusuario=" + superusuario + ", perfilId=" + perfilId + "}";
    }
}

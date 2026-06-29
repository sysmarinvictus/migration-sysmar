package br.gov.mandaguari.saude.seguranca.repository;

import br.gov.mandaguari.saude.seguranca.domain.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer>,
        JpaSpecificationExecutor<Usuario> {

    /**
     * Lookup by login. The DB has NO UNIQUE on UsuLogin (only the non-unique index usau_usu), so this
     * could theoretically return more than one row — uniqueness is enforced in the service (R13/OQ10).
     * Spring Data throws {@code IncorrectResultSizeDataAccessException} if duplicates exist, which the
     * service treats as "ambiguous login" → generic auth failure (no user enumeration, R3).
     */
    Optional<Usuario> findByLogin(String login);

    /** Admin list filters — case-insensitive contains on login/nome, optional bloqueado flag. */
    @Query("""
            select u from Usuario u
            where (:login is null or lower(u.login) like lower(concat('%', :login, '%')))
              and (:nome  is null or lower(u.nome)  like lower(concat('%', :nome,  '%')))
              and (:bloqueado is null or u.bloqueado = :bloqueado)
            """)
    Page<Usuario> search(@Param("login") String login,
                         @Param("nome") String nome,
                         @Param("bloqueado") Short bloqueado,
                         Pageable pageable);

    /** Lookup (autocomplete) — slim picker over login/nome. */
    @Query("select u from Usuario u where lower(u.login) like lower(concat('%', :q, '%')) "
            + "or lower(u.nome) like lower(concat('%', :q, '%')) order by u.login")
    java.util.List<Usuario> lookup(@Param("q") String q, Pageable pageable);

    // --- read-only profile lookup from SAU_PRF (OQ8: introspect read-only, do not migrate its CRUD) ---
    // ⚠ SAU_PRF column names (prfnom/prfdes) are inferred from the domain glossary and MUST be confirmed
    // against the live DB before cutover. Queries are defensive: they tolerate the table/columns being
    // absent in test/baseline (caught in the service → null → no admin elevation).

    /** Profile display name → used to decide if the profile is an "admin" profile (configurable match). */
    @Query(value = "select prfnom from sau_prf where prfcod = :prfcod", nativeQuery = true)
    Optional<String> findPerfilNome(@Param("prfcod") Integer prfcod);

    // --- delete guards (R17) — defensive: tolerate the guard tables being absent in baseline/test ---

    @Query(value = "select exists(select 1 from sau_usucon where usucod = :usucod)", nativeQuery = true)
    boolean isReferencedByUsuCon(@Param("usucod") Integer usucod);

    @Query(value = "select exists(select 1 from sau_usuuni where usucod = :usucod)", nativeQuery = true)
    boolean isReferencedByUsuUni(@Param("usucod") Integer usucod);
}

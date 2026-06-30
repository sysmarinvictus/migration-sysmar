package br.gov.mandaguari.saude.autorizacao.service;

import br.gov.mandaguari.saude.autorizacao.domain.Mode;
import br.gov.mandaguari.saude.autorizacao.domain.PerfilPermissaoId;
import br.gov.mandaguari.saude.autorizacao.domain.UsuarioPermissaoId;
import br.gov.mandaguari.saude.autorizacao.repository.PerfilPermissaoRepository;
import br.gov.mandaguari.saude.autorizacao.repository.UsuarioPermissaoRepository;
import br.gov.mandaguari.saude.perfil.repository.PerfilRepository;
import br.gov.mandaguari.saude.seguranca.domain.Usuario;
import br.gov.mandaguari.saude.seguranca.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fine-grained per-program authorization — the GeneXus {@code pisauthorized} precedence engine (SAU_USU
 * R8/R2/R5), realized over the migrated permission matrices. Resolution order for {@code can(user, program, mode)}:
 *
 * <ol>
 *   <li>unknown user → deny;</li>
 *   <li>blocked user ({@code UsuBloq=1}) → deny (R5);</li>
 *   <li>{@code UsuSysmar=true} → grant everything (R2 superuser bypass — the flag, not the magic login);</li>
 *   <li>user has a <b>valid profile</b> ({@code UsuPrfCod}&gt;0 ∈ SAU_PRF) → permission from
 *       <b>SAU_PRFCON</b> (profile tier has <b>precedence</b>, NOT OR-ed with per-user);</li>
 *   <li>otherwise → permission from <b>SAU_USUCON</b> (per-user fallback tier);</li>
 *   <li>no matching permission row → deny.</li>
 * </ol>
 *
 * <p>Registered as bean {@code rbac} so endpoints can guard with
 * {@code @PreAuthorize("@rbac.can(authentication, 'ATEMED', 'ALT')")}. The coarse role model (SAU_USU
 * OQ7) stays in place for already-migrated endpoints; this engine enables per-program wiring as each
 * endpoint is mapped to its GeneXus program code (deferred, endpoint-by-endpoint).
 */
@Component("rbac")
@Transactional(readOnly = true)
public class PermissionResolver {

    private final UsuarioRepository usuarioRepo;
    private final PerfilRepository perfilRepo;
    private final PerfilPermissaoRepository perfilPermRepo;
    private final UsuarioPermissaoRepository usuarioPermRepo;

    public PermissionResolver(UsuarioRepository usuarioRepo, PerfilRepository perfilRepo,
                              PerfilPermissaoRepository perfilPermRepo,
                              UsuarioPermissaoRepository usuarioPermRepo) {
        this.usuarioRepo = usuarioRepo;
        this.perfilRepo = perfilRepo;
        this.perfilPermRepo = perfilPermRepo;
        this.usuarioPermRepo = usuarioPermRepo;
    }

    /** Core resolution (R8/R2/R5). */
    public boolean can(Integer usuCod, String programaId, Mode mode) {
        if (usuCod == null || programaId == null || mode == null) return false;
        Usuario u = usuarioRepo.findById(usuCod).orElse(null);
        if (u == null) return false;
        if (u.getBloqueado() != null && u.getBloqueado() == 1) return false;   // R5
        if (Boolean.TRUE.equals(u.getSuperusuario())) return true;             // R2

        if (hasValidProfile(u.getPerfilId())) {                                // R8 tier 2 (precedence)
            return perfilPermRepo.findById(new PerfilPermissaoId(u.getPerfilId(), programaId))
                    .map(p -> p.granted(mode)).orElse(false);
        }
        return usuarioPermRepo.findById(new UsuarioPermissaoId(usuCod, programaId))   // R8 tier 3 (fallback)
                .map(p -> p.granted(mode)).orElse(false);
    }

    /** Spring Security hook: principal name = UsuCod (JWT sub). Non-numeric principals → deny. */
    public boolean can(Authentication auth, String programaId, String mode) {
        if (auth == null || auth.getName() == null || !auth.getName().matches("\\d+")) return false;
        return can(Integer.valueOf(auth.getName()), programaId, parseMode(mode));
    }

    private boolean hasValidProfile(Integer perfilId) {
        return perfilId != null && perfilId > 0 && perfilRepo.existsById(perfilId);
    }

    private static Mode parseMode(String mode) {
        try {
            return Mode.valueOf(mode.trim().toUpperCase());
        } catch (RuntimeException ex) {
            return null;
        }
    }
}

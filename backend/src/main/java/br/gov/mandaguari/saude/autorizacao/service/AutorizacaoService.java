package br.gov.mandaguari.saude.autorizacao.service;

import br.gov.mandaguari.saude.autorizacao.domain.*;
import br.gov.mandaguari.saude.autorizacao.dto.AutorizacaoDtos.*;
import br.gov.mandaguari.saude.autorizacao.repository.PerfilPermissaoRepository;
import br.gov.mandaguari.saude.autorizacao.repository.UsuarioPermissaoRepository;
import br.gov.mandaguari.saude.common.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read + maintain the per-profile (SAU_PRFCON) and per-user (SAU_USUCON) permission grids, and expose
 * the {@link PermissionResolver} check. Every grid write is audited (common/audit). The actual
 * precedence/grant logic lives in {@link PermissionResolver} (R8).
 */
@Service
@Transactional(readOnly = true)
public class AutorizacaoService {

    private final PerfilPermissaoRepository perfilPermRepo;
    private final UsuarioPermissaoRepository usuarioPermRepo;
    private final PermissionResolver resolver;
    private final AuditService audit;

    public AutorizacaoService(PerfilPermissaoRepository perfilPermRepo,
                              UsuarioPermissaoRepository usuarioPermRepo,
                              PermissionResolver resolver, AuditService audit) {
        this.perfilPermRepo = perfilPermRepo;
        this.usuarioPermRepo = usuarioPermRepo;
        this.resolver = resolver;
        this.audit = audit;
    }

    // --- read grids ---

    public List<PermissaoResponse> getPerfilPermissoes(Integer perfilId) {
        return perfilPermRepo.findByPerfilId(perfilId).stream()
                .map(p -> new PermissaoResponse(p.getProgramaId(),
                        p.granted(Mode.CON), p.granted(Mode.INC), p.granted(Mode.ALT), p.granted(Mode.EXC)))
                .toList();
    }

    public List<PermissaoResponse> getUsuarioPermissoes(Integer usuCod) {
        return usuarioPermRepo.findByUsuarioId(usuCod).stream()
                .map(p -> new PermissaoResponse(p.getProgramaId(),
                        p.granted(Mode.CON), p.granted(Mode.INC), p.granted(Mode.ALT), p.granted(Mode.EXC)))
                .toList();
    }

    // --- maintain grids (upsert) ---

    @Transactional
    public PermissaoResponse setPerfilPermissao(Integer perfilId, String programaId, PermissaoUpsertRequest req) {
        PerfilPermissao p = perfilPermRepo.findById(new PerfilPermissaoId(perfilId, programaId))
                .orElseGet(() -> {
                    PerfilPermissao n = new PerfilPermissao();
                    n.setPerfilId(perfilId);
                    n.setProgramaId(programaId);
                    return n;
                });
        p.setConsultar(flag(req.consultar()));
        p.setIncluir(flag(req.incluir()));
        p.setAlterar(flag(req.alterar()));
        p.setExcluir(flag(req.excluir()));
        perfilPermRepo.save(p);
        audit.record("PERM_SET", "SAU_PRFCON", perfilId + "/" + programaId);
        return new PermissaoResponse(programaId, p.granted(Mode.CON), p.granted(Mode.INC),
                p.granted(Mode.ALT), p.granted(Mode.EXC));
    }

    @Transactional
    public PermissaoResponse setUsuarioPermissao(Integer usuCod, String programaId, PermissaoUpsertRequest req) {
        UsuarioPermissao p = usuarioPermRepo.findById(new UsuarioPermissaoId(usuCod, programaId))
                .orElseGet(() -> {
                    UsuarioPermissao n = new UsuarioPermissao();
                    n.setUsuarioId(usuCod);
                    n.setProgramaId(programaId);
                    return n;
                });
        p.setConsultar(flag(req.consultar()));
        p.setIncluir(flag(req.incluir()));
        p.setAlterar(flag(req.alterar()));
        p.setExcluir(flag(req.excluir()));
        usuarioPermRepo.save(p);
        audit.record("PERM_SET", "SAU_USUCON", usuCod + "/" + programaId);
        return new PermissaoResponse(programaId, p.granted(Mode.CON), p.granted(Mode.INC),
                p.granted(Mode.ALT), p.granted(Mode.EXC));
    }

    // --- check ---

    public CheckResponse check(Integer usuCod, String programaId, Mode mode) {
        boolean granted = resolver.can(usuCod, programaId, mode);
        // Audit the authorization oracle (admin-only endpoint) so misuse is traceable. NOTE: this is the
        // diagnostic /check endpoint only — the per-request resolver.can() used by @PreAuthorize is NOT
        // audited (would be far too noisy once wired into endpoint enforcement, OQ1).
        audit.record("AUTHZ_CHECK", "SAU_USUCON", usuCod + "/" + programaId + ":" + mode.name());
        return new CheckResponse(usuCod, programaId, mode.name(), granted);
    }

    private static short flag(boolean b) { return (short) (b ? 1 : 0); }
}

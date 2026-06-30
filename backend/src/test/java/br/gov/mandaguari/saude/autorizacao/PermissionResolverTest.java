package br.gov.mandaguari.saude.autorizacao;

import br.gov.mandaguari.saude.autorizacao.domain.*;
import br.gov.mandaguari.saude.autorizacao.repository.PerfilPermissaoRepository;
import br.gov.mandaguari.saude.autorizacao.repository.UsuarioPermissaoRepository;
import br.gov.mandaguari.saude.autorizacao.service.PermissionResolver;
import br.gov.mandaguari.saude.perfil.repository.PerfilRepository;
import br.gov.mandaguari.saude.seguranca.domain.Usuario;
import br.gov.mandaguari.saude.seguranca.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the fine-grained authorization precedence engine (SAU_USU R8/R2/R5). The whole point of
 * the RBAC cluster: SYSMAR bypass, profile-tier precedence over per-user, per-user fallback, deny-by-default.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermissionResolverTest {

    static final String PRG = "ATEMED";

    @Mock UsuarioRepository usuarioRepo;
    @Mock PerfilRepository perfilRepo;
    @Mock PerfilPermissaoRepository perfilPermRepo;
    @Mock UsuarioPermissaoRepository usuarioPermRepo;

    PermissionResolver resolver() {
        return new PermissionResolver(usuarioRepo, perfilRepo, perfilPermRepo, usuarioPermRepo);
    }

    private Usuario user(int cod, boolean sysmar, Integer perfilId, int bloqueado) {
        Usuario u = new Usuario();
        u.setUsuCod(cod);
        u.setSuperusuario(sysmar);
        u.setPerfilId(perfilId);
        u.setBloqueado((short) bloqueado);
        return u;
    }

    private PerfilPermissao prfcon(int perfilId, String prg, int con, int inc, int alt, int exc) {
        PerfilPermissao p = new PerfilPermissao();
        p.setPerfilId(perfilId); p.setProgramaId(prg);
        p.setConsultar((short) con); p.setIncluir((short) inc);
        p.setAlterar((short) alt); p.setExcluir((short) exc);
        return p;
    }

    private UsuarioPermissao usucon(int usuCod, String prg, int con, int inc, int alt, int exc) {
        UsuarioPermissao p = new UsuarioPermissao();
        p.setUsuarioId(usuCod); p.setProgramaId(prg);
        p.setConsultar((short) con); p.setIncluir((short) inc);
        p.setAlterar((short) alt); p.setExcluir((short) exc);
        return p;
    }

    // unknown user → deny
    @Test
    void unknownUser_denies() {
        when(usuarioRepo.findById(99)).thenReturn(Optional.empty());
        assertThat(resolver().can(99, PRG, Mode.CON)).isFalse();
    }

    // R5 — blocked user denied (before any permission lookup)
    @Test
    void blockedUser_denies() {
        when(usuarioRepo.findById(1)).thenReturn(Optional.of(user(1, false, 2, 1)));
        assertThat(resolver().can(1, PRG, Mode.CON)).isFalse();
        verify(perfilPermRepo, never()).findById(any());
        verify(usuarioPermRepo, never()).findById(any());
    }

    // R2 — SYSMAR grants everything, no permission lookup
    @Test
    void sysmar_grantsAllModesWithoutLookup() {
        when(usuarioRepo.findById(1)).thenReturn(Optional.of(user(1, true, null, 0)));
        PermissionResolver r = resolver();
        for (Mode m : Mode.values()) assertThat(r.can(1, PRG, m)).isTrue();
        verify(perfilPermRepo, never()).findById(any());
        verify(usuarioPermRepo, never()).findById(any());
    }

    // R8 tier 2 — valid profile → PRFCON grants the requested mode
    @Test
    void validProfile_grantsFromPrfconForMode() {
        when(usuarioRepo.findById(1)).thenReturn(Optional.of(user(1, false, 2, 0)));
        when(perfilRepo.existsById(2)).thenReturn(true);
        when(perfilPermRepo.findById(new PerfilPermissaoId(2, PRG)))
                .thenReturn(Optional.of(prfcon(2, PRG, 1, 0, 1, 0)));   // CON+ALT granted, INC+EXC denied
        PermissionResolver r = resolver();
        assertThat(r.can(1, PRG, Mode.CON)).isTrue();
        assertThat(r.can(1, PRG, Mode.ALT)).isTrue();
        assertThat(r.can(1, PRG, Mode.INC)).isFalse();
        assertThat(r.can(1, PRG, Mode.EXC)).isFalse();
    }

    @Test
    void validProfile_deniesWhenNoPrfconRow() {
        when(usuarioRepo.findById(1)).thenReturn(Optional.of(user(1, false, 2, 0)));
        when(perfilRepo.existsById(2)).thenReturn(true);
        when(perfilPermRepo.findById(any())).thenReturn(Optional.empty());
        assertThat(resolver().can(1, PRG, Mode.CON)).isFalse();
    }

    // R8 — PROFILE PRECEDENCE: a user WITH a valid profile never falls back to per-user, even if USUCON would grant
    @Test
    void profileTakesPrecedenceOverPerUser() {
        when(usuarioRepo.findById(1)).thenReturn(Optional.of(user(1, false, 2, 0)));
        when(perfilRepo.existsById(2)).thenReturn(true);
        when(perfilPermRepo.findById(any())).thenReturn(Optional.empty());      // profile denies
        when(usuarioPermRepo.findById(any())).thenReturn(Optional.of(usucon(1, PRG, 1, 1, 1, 1))); // would grant
        assertThat(resolver().can(1, PRG, Mode.ALT)).isFalse();                 // profile precedence → denied
        verify(usuarioPermRepo, never()).findById(any());                       // per-user tier not consulted
    }

    // R8 tier 3 — no profile → per-user USUCON
    @Test
    void noProfile_grantsFromUsucon() {
        when(usuarioRepo.findById(1)).thenReturn(Optional.of(user(1, false, 0, 0)));   // perfilId 0 = none
        when(usuarioPermRepo.findById(new UsuarioPermissaoId(1, PRG)))
                .thenReturn(Optional.of(usucon(1, PRG, 0, 1, 0, 0)));   // only INC
        PermissionResolver r = resolver();
        assertThat(r.can(1, PRG, Mode.INC)).isTrue();
        assertThat(r.can(1, PRG, Mode.CON)).isFalse();
    }

    @Test
    void noProfile_deniesWhenNoUsuconRow() {
        when(usuarioRepo.findById(1)).thenReturn(Optional.of(user(1, false, null, 0)));
        when(usuarioPermRepo.findById(any())).thenReturn(Optional.empty());
        assertThat(resolver().can(1, PRG, Mode.CON)).isFalse();
    }

    // invalid profile id (not in SAU_PRF) → falls back to per-user tier
    @Test
    void invalidProfileFallsBackToUsucon() {
        when(usuarioRepo.findById(1)).thenReturn(Optional.of(user(1, false, 77, 0)));
        when(perfilRepo.existsById(77)).thenReturn(false);
        when(usuarioPermRepo.findById(new UsuarioPermissaoId(1, PRG)))
                .thenReturn(Optional.of(usucon(1, PRG, 1, 0, 0, 0)));
        assertThat(resolver().can(1, PRG, Mode.CON)).isTrue();
        verify(perfilPermRepo, never()).findById(any());
    }

    @Test
    void nullArguments_deny() {
        PermissionResolver r = resolver();
        assertThat(r.can(null, PRG, Mode.CON)).isFalse();
        assertThat(r.can(1, null, Mode.CON)).isFalse();
        assertThat(r.can(1, PRG, null)).isFalse();
    }

    // Authentication overload: principal = UsuCod (numeric); non-numeric (dev stub) → deny
    @Test
    void authenticationOverload_resolvesNumericPrincipal() {
        when(usuarioRepo.findById(1)).thenReturn(Optional.of(user(1, true, null, 0)));
        var authNumeric = new UsernamePasswordAuthenticationToken("1", "x");
        var authName = new UsernamePasswordAuthenticationToken("admin", "x");
        PermissionResolver r = resolver();
        assertThat(r.can(authNumeric, PRG, "ALT")).isTrue();
        assertThat(r.can(authName, PRG, "ALT")).isFalse();      // non-numeric principal → deny
        assertThat(r.can(authNumeric, PRG, "BOGUS")).isFalse(); // bad mode → deny
    }
}

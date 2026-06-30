package br.gov.mandaguari.saude.seguranca;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.seguranca.domain.Usuario;
import br.gov.mandaguari.saude.seguranca.dto.UsuarioDtos.*;
import br.gov.mandaguari.saude.seguranca.mapper.UsuarioMapper;
import br.gov.mandaguari.saude.seguranca.repository.UsuarioRepository;
import br.gov.mandaguari.saude.seguranca.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the mined SAU_USU user-admin + self-service password rules (R10–R17).
 * Repository + audit are mocked; the real MapStruct mapper and a real {@link BCryptPasswordEncoder}
 * are wired so password hashing/verification is exercised for real. Rule refs match the SLICE-SPEC
 * SAU_USU citations. Synthetic, non-PHI identifiers only; no plaintext password is ever asserted in logs.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioServiceTest {

    @Mock UsuarioRepository repo;
    @Mock AuditService audit;
    final UsuarioMapper mapper = Mappers.getMapper(UsuarioMapper.class);
    final PasswordEncoder encoder = new BCryptPasswordEncoder();

    UsuarioService service() {
        return new UsuarioService(repo, mapper, encoder, audit);
    }

    /** Wires the happy-path create() collaborators: unique login, save echoes back, id allocation. */
    private void stubHappyCreate() {
        when(repo.findByLogin(anyString())).thenReturn(Optional.empty());        // R13 unique
        when(repo.findAll(any(Pageable.class))).thenReturn(Page.empty());        // nextUsuCod → 1
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private UsuarioCreateRequest create(String nome, String login, Boolean bloqueado, Boolean sysmar) {
        return new UsuarioCreateRequest(nome, login, "senhaInicial123", (short) 0,
                null, null, null, bloqueado, sysmar);
    }

    private Usuario existing(int id, String login, String storedHash) {
        Usuario u = new Usuario();
        u.setUsuCod(id);
        u.setLogin(login);
        u.setNome("Usuario Sintetico");
        u.setSenha(storedHash);
        u.setChaveSenha(null);
        u.setBloqueado((short) 0);
        return u;
    }

    // ── R12 — nome + login required ────────────────────────────────────────────
    @Test
    void create_rejectsBlankNome() {
        assertThatThrownBy(() -> service().create(create("  ", "joao", null, null)))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("obrigatórios");
    }

    @Test
    void create_rejectsBlankLogin() {
        assertThatThrownBy(() -> service().create(create("João", "  ", null, null)))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("obrigatórios");
    }

    // ── R13 — login unique (enforced in service; no DB UNIQUE) ─────────────────
    @Test
    void create_rejectsDuplicateLogin() {
        when(repo.findByLogin("joao")).thenReturn(Optional.of(existing(99, "joao", "x")));
        assertThatThrownBy(() -> service().create(create("João", "joao", null, null)))
                .isInstanceOf(Conflict.class)
                .hasMessageContaining("já está sendo usado");
        verify(repo, never()).save(any());
    }

    @Test
    void create_treatsAmbiguousLoginAsDuplicate() {
        when(repo.findByLogin("joao")).thenThrow(new IncorrectResultSizeDataAccessException(1));
        assertThatThrownBy(() -> service().create(create("João", "joao", null, null)))
                .isInstanceOf(Conflict.class);
        verify(repo, never()).save(any());
    }

    // ── R16 — bloqueado defaults to 0 (unblocked) when null on create ──────────
    @Test
    void create_defaultsUnblocked() {
        stubHappyCreate();
        service().create(create("João", "joao", null, null));
        verify(repo).save(argThat(u -> u.getBloqueado() != null && u.getBloqueado() == 0));
    }

    @Test
    void create_honorsExplicitBlocked() {
        stubHappyCreate();
        service().create(create("João", "joao", Boolean.TRUE, null));
        verify(repo).save(argThat(u -> u.getBloqueado() == 1));
    }

    // ── create — password stored as bcrypt, UsuKey NULL (migrated), audit CREATE ─
    @Test
    void create_storesBcryptHashAndNullsKeyAndAudits() {
        stubHappyCreate();
        service().create(create("João", "joao", null, null));
        verify(repo).save(argThat(u ->
                u.getSenha() != null
                        && !u.getSenha().equals("senhaInicial123")          // never stored as plaintext
                        && encoder.matches("senhaInicial123", u.getSenha())  // verifiable bcrypt
                        && u.getChaveSenha() == null));                      // migrated marker
        verify(audit).record(eq("CREATE"), eq("SAU_USU"), eq(1));            // real allocated id, not 0
    }

    @Test
    void create_setsSuperuserFlagWhenRequested() {
        stubHappyCreate();
        service().create(create("Root", "root", null, Boolean.TRUE));
        verify(repo).save(argThat(u -> Boolean.TRUE.equals(u.getSuperusuario())));
    }

    // ── R12 — update requires nome ─────────────────────────────────────────────
    @Test
    void update_rejectsBlankNome() {
        when(repo.findById(5)).thenReturn(Optional.of(existing(5, "joao", "x")));
        var req = new UsuarioUpdateRequest("  ", (short) 0, null, null, null, null, null);
        assertThatThrownBy(() -> service().update(5, req))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("nome é obrigatório");
    }

    // ── R16 — block / unblock via update ───────────────────────────────────────
    @Test
    void update_blocksUser() {
        Usuario u = existing(5, "joao", "x");
        when(repo.findById(5)).thenReturn(Optional.of(u));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var req = new UsuarioUpdateRequest("João", (short) 0, null, null, null, Boolean.TRUE, null);
        service().update(5, req);
        verify(repo).save(argThat(s -> s.getBloqueado() == 1));
        verify(audit).record(eq("UPDATE"), eq("SAU_USU"), eq(5));
    }

    // ── R17 — delete blocked when referenced ───────────────────────────────────
    @Test
    void delete_blockedWhenReferencedByUsuCon() {
        Usuario u = existing(5, "joao", "x");
        when(repo.findById(5)).thenReturn(Optional.of(u));
        when(repo.isReferencedByUsuCon(5)).thenReturn(true);
        assertThatThrownBy(() -> service().delete(5))
                .isInstanceOf(Conflict.class)
                .hasMessageContaining("vínculos");
        verify(repo, never()).delete(any(Usuario.class));
    }

    @Test
    void delete_blockedWhenReferencedByUsuUni() {
        Usuario u = existing(5, "joao", "x");
        when(repo.findById(5)).thenReturn(Optional.of(u));
        when(repo.isReferencedByUsuCon(5)).thenReturn(false);
        when(repo.isReferencedByUsuUni(5)).thenReturn(true);
        assertThatThrownBy(() -> service().delete(5)).isInstanceOf(Conflict.class);
        verify(repo, never()).delete(any(Usuario.class));
    }

    @Test
    void delete_allowedWhenUnreferenced() {
        Usuario u = existing(5, "joao", "x");
        when(repo.findById(5)).thenReturn(Optional.of(u));
        when(repo.isReferencedByUsuCon(5)).thenReturn(false);
        when(repo.isReferencedByUsuUni(5)).thenReturn(false);
        service().delete(5);
        verify(repo).delete(u);
        verify(audit).record(eq("DELETE"), eq("SAU_USU"), eq(5));
    }

    @Test
    void delete_notFound() {
        when(repo.findById(404)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().delete(404)).isInstanceOf(NotFound.class);
    }

    // ── R10 — change password: current verified, new==confirmation ─────────────
    @Test
    void changePassword_rejectsWrongCurrent() {
        Usuario u = existing(7, "joao", encoder.encode("certa"));
        when(repo.findById(7)).thenReturn(Optional.of(u));
        var req = new ChangePasswordRequest("ERRADA", "novaSenha1", "novaSenha1");
        assertThatThrownBy(() -> service().changePassword("7", req))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("atual incorreta");
        verify(repo, never()).save(any());
    }

    @Test
    void changePassword_rejectsConfirmationMismatch() {
        Usuario u = existing(7, "joao", encoder.encode("certa"));
        when(repo.findById(7)).thenReturn(Optional.of(u));
        var req = new ChangePasswordRequest("certa", "novaSenha1", "outraCoisa2");
        assertThatThrownBy(() -> service().changePassword("7", req))
                .isInstanceOf(BusinessRule.class)
                .hasMessageContaining("não conferem");
        verify(repo, never()).save(any());
    }

    // ── R11 — change password: bcrypt new, rotate UsuKey NULL, stamp redefinição ─
    @Test
    void changePassword_storesNewBcryptNullsKeyAndStampsDate() {
        Usuario u = existing(7, "joao", encoder.encode("certa"));
        when(repo.findById(7)).thenReturn(Optional.of(u));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().changePassword("7", new ChangePasswordRequest("certa", "novaSenha1", "novaSenha1"));

        assertThat(encoder.matches("novaSenha1", u.getSenha())).isTrue();   // R11 new stored as bcrypt
        assertThat(u.getSenha()).isNotEqualTo("novaSenha1");                // never plaintext
        assertThat(u.getChaveSenha()).isNull();                            // R11 rotated/migrated marker
        assertThat(u.getDataRedefinicaoSenha()).isEqualTo(LocalDate.now()); // R11 stamp
        verify(audit).record(eq("CHANGE_PASSWORD"), eq("SAU_USU"), eq(7));
    }

    // ── get — not found ────────────────────────────────────────────────────────
    @Test
    void get_notFound() {
        when(repo.findById(404)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().get(404)).isInstanceOf(NotFound.class);
    }
}

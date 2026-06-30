package br.gov.mandaguari.saude.pessoa;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.pessoa.domain.Pessoa;
import br.gov.mandaguari.saude.pessoa.repository.PessoaRepository;
import br.gov.mandaguari.saude.pessoa.service.PessoaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the Pessoa supertype slice: social-name display rule (R2/R3), PHI-read audit, and the
 * centralized CPF/CNS validators. Synthetic identifiers only.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PessoaServiceTest {

    static final String VALID_CPF = "01111111294";       // mod-11 valid (synthetic)
    static final String VALID_CNS = "700000000000021";   // provisional, mod-11 valid (synthetic)

    @Mock PessoaRepository repo;
    @Mock AuditService audit;
    PessoaService service() { return new PessoaService(repo, audit); }

    private Pessoa pessoa(long id, String nome, String social, Boolean usa) {
        Pessoa p = new Pessoa(); p.setId(id); p.setNome(nome); p.setNomeSocial(social); p.setUsaNomeSocial(usa);
        return p;
    }

    // R2/R3 — social name display
    @Test
    void usesSocialNameWhenOptedIn() {
        Pessoa p = pessoa(1, "MARIA REGISTRO", "MARIA SOCIAL", true);
        assertThat(p.getNomeExibicao()).isEqualTo("MARIA SOCIAL");
        assertThat(p.getNomeCompleto()).isEqualTo("MARIA SOCIAL (MARIA REGISTRO)");
    }

    @Test
    void usesRegistryNameWhenNotOptedIn() {
        Pessoa p = pessoa(2, "JOAO REGISTRO", "IGNORADO", false);
        assertThat(p.getNomeExibicao()).isEqualTo("JOAO REGISTRO");
        assertThat(p.getNomeCompleto()).isEqualTo("JOAO REGISTRO");
    }

    // PHI read audited
    @Test
    void getAuditsPhiRead() {
        when(repo.findById(1L)).thenReturn(Optional.of(pessoa(1, "MARIA", null, false)));
        service().get(1L);
        verify(audit).record(eq("READ"), eq("SYS_PES"), eq(1L));
    }

    @Test
    void getNotFound() {
        when(repo.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().get(9L)).isInstanceOf(NotFound.class);
    }

    // centralized validators
    @Test
    void cpfValidoChecksDigits() {
        PessoaService s = service();
        assertThat(s.cpfValido("011.111.112-94")).isTrue();   // formatting stripped
        assertThat(s.cpfValido("12345678900")).isFalse();
        assertThat(s.cpfValido(null)).isTrue();                // blank → not invalid
    }

    @Test
    void cnsValidoChecksDigits() {
        PessoaService s = service();
        assertThat(s.cnsValido(VALID_CNS)).isTrue();
        assertThat(s.cnsValido("700000000000001")).isFalse();
        assertThat(s.cnsValido("")).isTrue();
    }

    // R17 — person-wide CPF uniqueness
    @Test
    void cpfDisponivelWhenNoOtherPersonUsesIt() {
        when(repo.findCpfOwners(eq(VALID_CPF), eq(-1L), any())).thenReturn(List.of());
        assertThat(service().cpfDisponivel("011.111.112-94", null)).isTrue();
    }

    @Test
    void cpfNotDisponivelWhenAnotherPersonUsesIt() {
        when(repo.findCpfOwners(eq(VALID_CPF), eq(7L), any())).thenReturn(List.of(99L));
        assertThat(service().cpfDisponivel(VALID_CPF, 7L)).isFalse();
    }
}

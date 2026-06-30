package br.gov.mandaguari.saude.funcionario;

import br.gov.mandaguari.saude.funcionario.domain.Funcionario;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * R18 — the legacy app logged FunPesNomSoundex / person values; that defect is NOT ported. The entity's
 * {@code toString()} (the most likely accidental log sink) must never expose the PHI-derived soundex.
 */
class FuncionarioSecurityTest {

    @Test
    void toStringOmitsSoundex() {
        Funcionario f = new Funcionario();
        f.setId(100L);
        f.setSituacao((short) 1);
        f.setNomeSoundex("MARISARIVA");          // PHI-derived phonetic key — must not leak
        f.setTelefoneTrabalho("(44) 3232-3232");

        String s = f.toString();

        assertThat(s).doesNotContain("MARISARIVA");
        assertThat(s).contains("id=100");
    }
}

package br.gov.mandaguari.saude.profissional;

import br.gov.mandaguari.saude.profissional.service.SoundexService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verbatim-port parity tests for the Portuguese phonetic key (psau_soundex.java). Each case exercises
 * a distinct branch of the ordered replacement table / trailing-strip / dup-collapse pipeline so the
 * legacy search behaviour (ProPesNomSoundex) is preserved exactly. Names are synthetic (non-PHI).
 */
class SoundexServiceTest {

    final SoundexService soundex = new SoundexService();

    @ParameterizedTest
    @CsvSource({
            // PH -> F (then trailing-letter handling, L -> R)
            "PHILIPE,   FIRIPE",
            // Y -> I
            "YASMIN,     IASMI",
            // Ç path via CE/CI/C -> S/K cascade + dup-collapse
            "CONCEICAO,  KMSIKO",
            // W -> V, trailing R strip, L -> R
            "WALDEMAR,   VARDEMA",
            // trailing-letter strip (A...VA keeps), L -> R: SILVA -> SIRVA
            "SILVA,      SIRVA",
            // trailing 'AO' strip: JOAO -> JO
            "JOAO,       JO",
            // consecutive-duplicate collapse: ANNA (N->M) -> AMMA -> AMA
            "ANNA,       AMA",
            // no replacement applies; trailing vowel kept
            "MARIA,      MARIA",
    })
    void mapsKnownNamesToLegacyPhoneticKey(String name, String expected) {
        assertThat(soundex.compute(name)).isEqualTo(expected);
    }

    @Test
    void nullAndBlankYieldEmptyKey() {
        assertThat(soundex.compute(null)).isEmpty();
        assertThat(soundex.compute("")).isEmpty();
        assertThat(soundex.compute("   ")).isEmpty();
    }

    @Test
    void isCaseInsensitiveOnInput() {
        assertThat(soundex.compute("philipe")).isEqualTo(soundex.compute("PHILIPE"));
    }
}

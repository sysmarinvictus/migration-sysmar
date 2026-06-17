package br.gov.mandaguari.saude.parity;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Golden-master parity stubs for SAU_BAI (Bairro).
 * Run with {@code mvn -Pparity test} against a live GeneXus app + non-production DB copy.
 * See SLICE-SPEC SAU_BAI parity scenarios.
 */
@Tag("parity")
@Disabled("Requires running GeneXus app — enable with -Pparity")
class BairroParityIT {

    @Test void listDefault() {}
    @Test void getByIdExisting() {}
    @Test void getByIdNotFound() {}
    @Test void insertValidNomeOnly() {}
    @Test void insertMissingNome() {}
    @Test void insertDuplicateNome() {}
    @Test void updateNome() {}
    @Test void updateToDuplicateNome() {}
    @Test void deleteUnused() {}
    @Test void deleteReferencedByPessoa() {}
    @Test void deleteReferencedByDistrito() {}
    @Test void lookupByNome() {}
}

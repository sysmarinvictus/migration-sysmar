package br.gov.mandaguari.saude.parametro.web;

import br.gov.mandaguari.saude.parametro.dto.ParametroDtos.*;
import br.gov.mandaguari.saude.parametro.service.ParametroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Parâmetros do sistema (SAU_PAR singleton). A read endpoint plus two focused update views mirroring the
 * legacy transactions: {@code /geral} (sau_par_ger) and {@code /ambulatorial} (sau_par_amb). Config
 * administration → SAUDE_ADMIN. The SIA procedure-code block of sau_par_amb is out of scope (deferred).
 */
@RestController
@RequestMapping("/api/parametros")
@Tag(name = "Parâmetros")
@PreAuthorize("hasRole('SAUDE_ADMIN')")
public class ParametroController {

    private final ParametroService service;

    public ParametroController(ParametroService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Ler os parâmetros do sistema (subset mapeado)")
    public ParametroResponse get() {
        return service.get();
    }

    @PutMapping("/geral")
    @Operation(summary = "Atualizar os parâmetros gerais (validade de receita, dias de usuário/senha, cabeçalho)")
    public ParametroResponse updateGeral(@RequestBody ParametroGeralUpdateRequest req) {
        return service.updateGeral(req);
    }

    @PutMapping("/ambulatorial")
    @Operation(summary = "Atualizar os parâmetros ambulatoriais (flags de política)")
    public ParametroResponse updateAmbulatorial(@RequestBody ParametroAmbulatorialUpdateRequest req) {
        return service.updateAmbulatorial(req);
    }
}

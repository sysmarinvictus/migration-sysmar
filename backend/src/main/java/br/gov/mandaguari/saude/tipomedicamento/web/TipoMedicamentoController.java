package br.gov.mandaguari.saude.tipomedicamento.web;

import br.gov.mandaguari.saude.tipomedicamento.dto.TipoMedicamentoDtos.*;
import br.gov.mandaguari.saude.tipomedicamento.service.TipoMedicamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST surface for Tipo de Medicamento — replaces the GeneXus sau_tiprem transaction + hpromptsau_tiprem.
 *
 * <p>NOTE: the legacy transaction disabled integrated security (sau_tiprem.java:33). Per project
 * convention the new app gates CRUD behind {@code SAUDE_CADASTRO} — confirm the exact permission
 * (SLICE-SPEC open question 1).
 */
@RestController
@RequestMapping("/api/tipos-medicamento")
@Tag(name = "Tipos de Medicamento")
public class TipoMedicamentoController {

    private final TipoMedicamentoService service;

    public TipoMedicamentoController(TipoMedicamentoService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @Operation(summary = "Listar/buscar tipos de medicamento (paginado)")
    public Page<TipoMedicamentoResponse> list(@RequestParam(required = false) String descricao,
                                              @PageableDefault(size = 20, sort = "descricao") Pageable pageable) {
        return service.list(descricao, pageable);
    }

    @GetMapping("/{codigo}")
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @Operation(summary = "Obter tipo de medicamento por código")
    public TipoMedicamentoResponse get(@PathVariable Integer codigo) {
        return service.get(codigo);
    }

    @GetMapping("/lookup")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Autocomplete de tipos de medicamento (FK picker)")
    public List<TipoMedicamentoLookupItem> lookup(@RequestParam(required = false, defaultValue = "") String q,
                                                  @PageableDefault(size = 10) Pageable pageable) {
        return service.lookup(q, pageable);
    }

    @PostMapping
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @Operation(summary = "Criar tipo de medicamento")
    public ResponseEntity<TipoMedicamentoResponse> create(@Valid @RequestBody TipoMedicamentoCreateRequest req,
                                                          UriComponentsBuilder uri) {
        TipoMedicamentoResponse created = service.create(req);
        URI location = uri.path("/api/tipos-medicamento/{codigo}").buildAndExpand(created.codigo()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{codigo}")
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @Operation(summary = "Atualizar tipo de medicamento")
    public TipoMedicamentoResponse update(@PathVariable Integer codigo,
                                          @Valid @RequestBody TipoMedicamentoUpdateRequest req) {
        return service.update(codigo, req);
    }

    @DeleteMapping("/{codigo}")
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir tipo de medicamento")
    public void delete(@PathVariable Integer codigo) {
        service.delete(codigo);
    }
}

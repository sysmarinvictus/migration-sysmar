package br.gov.mandaguari.saude.impedimento.web;

import br.gov.mandaguari.saude.impedimento.dto.ImpedimentoDtos.*;
import br.gov.mandaguari.saude.impedimento.service.ImpedimentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

/** REST surface for Impedimento — replaces the GeneXus psau_imp transaction. */
@RestController
@RequestMapping("/api/impedimentos")
@Tag(name = "Impedimentos")
public class ImpedimentoController {

    private final ImpedimentoService service;

    public ImpedimentoController(ImpedimentoService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @Operation(summary = "Listar impedimentos com filtros opcionais (paginado)")
    public Page<ImpedimentoResponse> list(
            @RequestParam(required = false) String profissionalNome,
            @RequestParam(required = false) Long profissionalId,
            @RequestParam(required = false) Integer especialidadeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicioFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFimAte,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.list(profissionalNome, profissionalId, especialidadeId, dataInicioFrom, dataFimAte, pageable);
    }

    @GetMapping("/{codigo}")
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @Operation(summary = "Obter impedimento por código")
    public ImpedimentoResponse get(@PathVariable Integer codigo) {
        return service.get(codigo);
    }

    @PostMapping
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @Operation(summary = "Registrar impedimento")
    public ResponseEntity<ImpedimentoResponse> create(@Valid @RequestBody ImpedimentoCreateRequest req,
                                                       UriComponentsBuilder uri) {
        ImpedimentoResponse created = service.create(req);
        URI location = uri.path("/api/impedimentos/{codigo}").buildAndExpand(created.codigo()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{codigo}")
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @Operation(summary = "Atualizar impedimento")
    public ImpedimentoResponse update(@PathVariable Integer codigo,
                                       @Valid @RequestBody ImpedimentoUpdateRequest req) {
        return service.update(codigo, req);
    }

    @DeleteMapping("/{codigo}")
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir impedimento")
    public void delete(@PathVariable Integer codigo) {
        service.delete(codigo);
    }
}

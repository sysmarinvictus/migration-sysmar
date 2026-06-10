package br.gov.mandaguari.saude.especialidade.web;

import br.gov.mandaguari.saude.especialidade.dto.EspecialidadeDtos.*;
import br.gov.mandaguari.saude.especialidade.service.EspecialidadeService;
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

/** REST surface for Especialidade — replaces the GeneXus sau_esp transaction + hpromptsau_esp. */
@RestController
@RequestMapping("/api/especialidades")
@Tag(name = "Especialidades")
public class EspecialidadeController {

    private final EspecialidadeService service;

    public EspecialidadeController(EspecialidadeService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @Operation(summary = "Listar/buscar especialidades (paginado)")
    public Page<EspecialidadeResponse> list(@RequestParam(required = false) String nome,
                                            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return service.list(nome, pageable);
    }

    @GetMapping("/{codigo}")
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @Operation(summary = "Obter especialidade por código")
    public EspecialidadeResponse get(@PathVariable Integer codigo) {
        return service.get(codigo);
    }

    @GetMapping("/lookup")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Autocomplete de especialidades (FK picker)")
    public List<EspecialidadeLookupItem> lookup(@RequestParam(required = false, defaultValue = "") String q,
                                                @PageableDefault(size = 10) Pageable pageable) {
        return service.lookup(q, pageable);
    }

    @PostMapping
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @Operation(summary = "Criar especialidade")
    public ResponseEntity<EspecialidadeResponse> create(@Valid @RequestBody EspecialidadeCreateRequest req,
                                                        UriComponentsBuilder uri) {
        EspecialidadeResponse created = service.create(req);
        URI location = uri.path("/api/especialidades/{codigo}").buildAndExpand(created.codigo()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{codigo}")
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @Operation(summary = "Atualizar especialidade")
    public EspecialidadeResponse update(@PathVariable Integer codigo,
                                        @Valid @RequestBody EspecialidadeUpdateRequest req) {
        return service.update(codigo, req);
    }

    @DeleteMapping("/{codigo}")
    @PreAuthorize("hasRole('SAUDE_CADASTRO')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir especialidade")
    public void delete(@PathVariable Integer codigo) {
        service.delete(codigo);
    }
}

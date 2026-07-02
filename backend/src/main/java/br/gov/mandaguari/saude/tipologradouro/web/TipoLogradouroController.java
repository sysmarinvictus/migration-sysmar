package br.gov.mandaguari.saude.tipologradouro.web;

import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.tipologradouro.domain.TipoLogradouro;
import br.gov.mandaguari.saude.tipologradouro.dto.TipoLogradouroDtos.*;
import br.gov.mandaguari.saude.tipologradouro.repository.TipoLogradouroRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST surface for TipoLogradouro — replaces hpromptsau_tiplog.
 * Read-only: SAU_TIPLOG has no GeneXus transaction form.
 */
@RestController
@RequestMapping("/api/tipos-logradouro")
@Tag(name = "TiposLogradouro")
public class TipoLogradouroController {

    private final TipoLogradouroRepository repo;

    public TipoLogradouroController(TipoLogradouroRepository repo) { this.repo = repo; }

    @GetMapping
    @PreAuthorize("@authz.can(authentication, 'SAU_TIPLOG', 'CON', 'SAUDE_CADASTRO')")
    @Operation(summary = "Listar/buscar tipos de logradouro (paginado)")
    public Page<TipoLogradouroResponse> list(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "sigla") Pageable pageable) {
        Page<TipoLogradouro> page = (q == null || q.isBlank()) ? repo.findAll(pageable) : repo.search(q, pageable);
        return page.map(t -> new TipoLogradouroResponse(t.getCodigo(), t.getNome(), t.getSigla()));
    }

    @GetMapping("/{codigo}")
    @PreAuthorize("@authz.can(authentication, 'SAU_TIPLOG', 'CON', 'SAUDE_CADASTRO')")
    @Operation(summary = "Obter tipo de logradouro por código")
    public TipoLogradouroResponse get(@PathVariable Integer codigo) {
        var t = repo.findById(codigo)
                .orElseThrow(() -> new NotFound("Tipo de logradouro " + codigo + " não encontrado"));
        return new TipoLogradouroResponse(t.getCodigo(), t.getNome(), t.getSigla());
    }

    @GetMapping("/lookup")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Autocomplete de tipos de logradouro (FK picker)")
    public List<TipoLogradouroLookupItem> lookup(
            @RequestParam(required = false, defaultValue = "") String q,
            @PageableDefault(size = 10) Pageable pageable) {
        return repo.lookup(q, pageable).stream()
                .map(t -> new TipoLogradouroLookupItem(t.getCodigo(), t.getSigla(), t.getNome()))
                .toList();
    }
}

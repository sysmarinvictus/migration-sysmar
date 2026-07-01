package br.gov.mandaguari.saude.receituarioespecial.web;

import br.gov.mandaguari.saude.receituarioespecial.dto.ReceituarioEspecialDtos.*;
import br.gov.mandaguari.saude.receituarioespecial.service.ReceituarioEspecialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Receituário Controle Especial API (SAU_RECESP + SAU_RECESP1) — controlled-substance prescription,
 * Portaria SVS/MS 344/98. Master+child aggregate; the composite key in the path is {unidadeCodigo}/{numero}.
 * Every read and write is audited (LGPD). SAUDE_CADASTRO.
 *
 * <p><b>Regulatory-gated:</b> DELETE is intentionally blocked (retention, SLICE-SPEC OQ2) rather than
 * porting the legacy hard-delete; print (R32-R36) is deferred. Per-program RBAC (permission key
 * {@code SAU_RECESP}, R3) is wired only as the coarse role until the RBAC engine is enabled.
 */
@RestController
@RequestMapping("/api/receituarios-especiais")
@Tag(name = "Receituários — Controle Especial")
@PreAuthorize("hasRole('SAUDE_CADASTRO')")
public class ReceituarioEspecialController {

    private final ReceituarioEspecialService service;

    public ReceituarioEspecialController(ReceituarioEspecialService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Buscar receituários de controle especial (por nome do paciente, unidade ou paciente)")
    public Page<ReceituarioEspecialListItem> search(@RequestParam(required = false) String nome,
                                                    @RequestParam(required = false) Integer unidade,
                                                    @RequestParam(required = false) Long paciente,
                                                    @PageableDefault(size = 20) Pageable pageable) {
        return service.search(nome, unidade, paciente, pageable);
    }

    @GetMapping("/{unidade}/{numero}")
    @Operation(summary = "Obter um receituário de controle especial (leitura de PHI auditada)")
    public ReceituarioEspecialResponse get(@PathVariable Integer unidade, @PathVariable Long numero) {
        return service.get(unidade, numero);
    }

    @PostMapping
    @Operation(summary = "Emitir um receituário de controle especial (número alocado por unidade)")
    public ResponseEntity<ReceituarioEspecialResponse> create(@RequestBody ReceituarioEspecialWriteRequest req,
                                                              UriComponentsBuilder uri) {
        ReceituarioEspecialResponse created = service.create(req);
        URI location = uri.path("/api/receituarios-especiais/{unidade}/{numero}")
                .buildAndExpand(created.unidadeCodigo(), created.numero()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{unidade}/{numero}")
    @Operation(summary = "Atualizar um receituário de controle especial (cabeçalho + itens)")
    public ReceituarioEspecialResponse update(@PathVariable Integer unidade, @PathVariable Long numero,
                                              @RequestBody ReceituarioEspecialWriteRequest req) {
        return service.update(unidade, numero, req);
    }

    @PostMapping("/{unidade}/{numero}/copia")
    @Operation(summary = "Copiar um receituário (clona com novo número na mesma unidade)")
    public ResponseEntity<ReceituarioEspecialResponse> copy(@PathVariable Integer unidade,
                                                            @PathVariable Long numero, UriComponentsBuilder uri) {
        ReceituarioEspecialResponse created = service.copy(unidade, numero);
        URI location = uri.path("/api/receituarios-especiais/{unidade}/{numero}")
                .buildAndExpand(created.unidadeCodigo(), created.numero()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @DeleteMapping("/{unidade}/{numero}")
    @Operation(summary = "Excluir (BLOQUEADO — retenção Portaria 344/98, pendente de definição regulatória)")
    public void delete(@PathVariable Integer unidade, @PathVariable Long numero) {
        service.delete(unidade, numero);   // always throws Conflict (R29 / OQ2)
    }
}

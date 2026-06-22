package br.gov.mandaguari.saude.medicamento.web;

import br.gov.mandaguari.saude.medicamento.dto.MedicamentoSubDtos.*;
import br.gov.mandaguari.saude.medicamento.service.MedicamentoSubService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicamentos/{remCod}")
@PreAuthorize("hasRole('SAUDE_CADASTRO')")
public class MedicamentoSubController {

    private final MedicamentoSubService service;

    public MedicamentoSubController(MedicamentoSubService service) { this.service = service; }

    // ── SAU_REM1 — unidades ───────────────────────────────────────────────────
    @GetMapping("/unidades")
    public List<UnidadeResponse> listUnidades(@PathVariable Integer remCod) {
        return service.listUnidades(remCod);
    }

    @PostMapping("/unidades")
    public ResponseEntity<UnidadeResponse> addUnidade(@PathVariable Integer remCod,
                                                      @Valid @RequestBody UnidadeCriarRequest req) {
        return ResponseEntity.status(201).body(service.addUnidade(remCod, req));
    }

    @PutMapping("/unidades/{uniCod}")
    public UnidadeResponse updateUnidade(@PathVariable Integer remCod, @PathVariable Integer uniCod,
                                         @Valid @RequestBody UnidadeAtualizarRequest req) {
        return service.updateUnidade(remCod, uniCod, req);
    }

    @DeleteMapping("/unidades/{uniCod}")
    public ResponseEntity<Void> removeUnidade(@PathVariable Integer remCod, @PathVariable Integer uniCod) {
        service.removeUnidade(remCod, uniCod);
        return ResponseEntity.noContent().build();
    }

    // ── SAU_REM2 — códigos de barras ──────────────────────────────────────────
    @GetMapping("/codigos-barras")
    public List<Ean13Response> listEan13(@PathVariable Integer remCod) {
        return service.listEan13(remCod);
    }

    @PostMapping("/codigos-barras")
    public ResponseEntity<Ean13Response> addEan13(@PathVariable Integer remCod,
                                                  @Valid @RequestBody Ean13CriarRequest req) {
        return ResponseEntity.status(201).body(service.addEan13(remCod, req));
    }

    @DeleteMapping("/codigos-barras/{ean13}")
    public ResponseEntity<Void> removeEan13(@PathVariable Integer remCod, @PathVariable Long ean13) {
        service.removeEan13(remCod, ean13);
        return ResponseEntity.noContent().build();
    }

    // ── SAU_REM_UNISETOR — unidade+setor ──────────────────────────────────────
    @GetMapping("/unidade-setores")
    public List<UnidadeSetorResponse> listUnidadeSetores(@PathVariable Integer remCod) {
        return service.listUnidadeSetores(remCod);
    }

    @PostMapping("/unidade-setores")
    public ResponseEntity<UnidadeSetorResponse> addUnidadeSetor(@PathVariable Integer remCod,
                                                                @Valid @RequestBody UnidadeSetorCriarRequest req) {
        return ResponseEntity.status(201).body(service.addUnidadeSetor(remCod, req));
    }

    @PutMapping("/unidade-setores/{seq}/{uniCod}")
    public UnidadeSetorResponse updateUnidadeSetor(@PathVariable Integer remCod, @PathVariable Integer seq,
                                                   @PathVariable Integer uniCod,
                                                   @Valid @RequestBody UnidadeSetorAtualizarRequest req) {
        return service.updateUnidadeSetor(remCod, seq, uniCod, req);
    }

    @DeleteMapping("/unidade-setores/{seq}/{uniCod}")
    public ResponseEntity<Void> removeUnidadeSetor(@PathVariable Integer remCod, @PathVariable Integer seq,
                                                   @PathVariable Integer uniCod) {
        service.removeUnidadeSetor(remCod, seq, uniCod);
        return ResponseEntity.noContent().build();
    }

    // ── SAU_REMPOSO — posologias ──────────────────────────────────────────────
    @GetMapping("/posologias")
    public List<PosologiaResponse> listPosologias(@PathVariable Integer remCod) {
        return service.listPosologias(remCod);
    }

    @PostMapping("/posologias")
    public ResponseEntity<PosologiaResponse> addPosologia(@PathVariable Integer remCod,
                                                          @Valid @RequestBody PosologiaCriarRequest req) {
        return ResponseEntity.status(201).body(service.addPosologia(remCod, req));
    }

    @DeleteMapping("/posologias/{posologiaCodigo}")
    public ResponseEntity<Void> removePosologia(@PathVariable Integer remCod, @PathVariable Integer posologiaCodigo) {
        service.removePosologia(remCod, posologiaCodigo);
        return ResponseEntity.noContent().build();
    }
}

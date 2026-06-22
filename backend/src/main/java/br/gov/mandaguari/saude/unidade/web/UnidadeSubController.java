package br.gov.mandaguari.saude.unidade.web;

import br.gov.mandaguari.saude.unidade.dto.UnidadeSubDtos.*;
import br.gov.mandaguari.saude.unidade.service.UnidadeSubService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/unidades/{uniCod}")
@PreAuthorize("hasRole('SAUDE_CADASTRO')")
public class UnidadeSubController {

    private final UnidadeSubService service;

    public UnidadeSubController(UnidadeSubService service) { this.service = service; }

    // ── Hiperdia ─────────────────────────────────────────────────────────────

    @GetMapping("/hiperdia-profissionais")
    public List<HiperdiaResponse> listHiperdia(@PathVariable Integer uniCod) {
        return service.listHiperdia(uniCod);
    }

    @PostMapping("/hiperdia-profissionais")
    public ResponseEntity<HiperdiaResponse> addHiperdia(@PathVariable Integer uniCod,
                                                         @Valid @RequestBody HiperdiaCriarRequest req) {
        return ResponseEntity.status(201).body(service.addHiperdia(uniCod, req));
    }

    @DeleteMapping("/hiperdia-profissionais/{profId}")
    public ResponseEntity<Void> removeHiperdia(@PathVariable Integer uniCod,
                                                @PathVariable Long profId) {
        service.removeHiperdia(uniCod, profId);
        return ResponseEntity.noContent().build();
    }

    // ── SisPré-Natal ─────────────────────────────────────────────────────────

    @GetMapping("/sisprenatal-profissionais")
    public List<SisPreNatalResponse> listSisPreNatal(@PathVariable Integer uniCod) {
        return service.listSisPreNatal(uniCod);
    }

    @PostMapping("/sisprenatal-profissionais")
    public ResponseEntity<SisPreNatalResponse> addSisPreNatal(@PathVariable Integer uniCod,
                                                               @Valid @RequestBody SisPreNatalCriarRequest req) {
        return ResponseEntity.status(201).body(service.addSisPreNatal(uniCod, req));
    }

    @DeleteMapping("/sisprenatal-profissionais/{profId}/{espId}")
    public ResponseEntity<Void> removeSisPreNatal(@PathVariable Integer uniCod,
                                                   @PathVariable Long profId,
                                                   @PathVariable Integer espId) {
        service.removeSisPreNatal(uniCod, profId, espId);
        return ResponseEntity.noContent().build();
    }

    // ── Nutricionistas ────────────────────────────────────────────────────────

    @GetMapping("/nutricionistas")
    public List<NutricionistaResponse> listNutricionistas(@PathVariable Integer uniCod) {
        return service.listNutricionistas(uniCod);
    }

    @PostMapping("/nutricionistas")
    public ResponseEntity<NutricionistaResponse> addNutricionista(@PathVariable Integer uniCod,
                                                                   @Valid @RequestBody NutricionistaCriarRequest req) {
        return ResponseEntity.status(201).body(service.addNutricionista(uniCod, req));
    }

    @DeleteMapping("/nutricionistas/{profId}/{espId}")
    public ResponseEntity<Void> removeNutricionista(@PathVariable Integer uniCod,
                                                     @PathVariable Long profId,
                                                     @PathVariable Integer espId) {
        service.removeNutricionista(uniCod, profId, espId);
        return ResponseEntity.noContent().build();
    }

    // ── Salas ─────────────────────────────────────────────────────────────────

    @GetMapping("/salas")
    public List<SalaResponse> listSalas(@PathVariable Integer uniCod) {
        return service.listSalas(uniCod);
    }

    @PostMapping("/salas")
    public ResponseEntity<SalaResponse> addSala(@PathVariable Integer uniCod,
                                                 @Valid @RequestBody SalaCriarRequest req) {
        return ResponseEntity.status(201).body(service.addSala(uniCod, req));
    }

    @PutMapping("/salas/{salaCodigo}")
    public SalaResponse updateSala(@PathVariable Integer uniCod,
                                   @PathVariable Short salaCodigo,
                                   @Valid @RequestBody SalaAtualizarRequest req) {
        return service.updateSala(uniCod, salaCodigo, req);
    }

    @DeleteMapping("/salas/{salaCodigo}")
    public ResponseEntity<Void> deleteSala(@PathVariable Integer uniCod,
                                            @PathVariable Short salaCodigo) {
        service.deleteSala(uniCod, salaCodigo);
        return ResponseEntity.noContent().build();
    }
}

package br.gov.mandaguari.saude.medicamento.web;

import br.gov.mandaguari.saude.medicamento.dto.MedicamentoDtos.*;
import br.gov.mandaguari.saude.medicamento.service.MedicamentoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/medicamentos")
@PreAuthorize("hasRole('SAUDE_CADASTRO')")
public class MedicamentoController {

    private final MedicamentoService service;

    public MedicamentoController(MedicamentoService service) { this.service = service; }

    @GetMapping
    public Page<MedicamentoResponse> list(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Integer tipoMedicamentoCodigo,
            @RequestParam(required = false) Short situacao,
            @RequestParam(required = false) Short psicotropico,
            @RequestParam(required = false) Short controleEspecial,
            @RequestParam(required = false) Short etico,
            Pageable pageable) {
        return service.list(nome, tipoMedicamentoCodigo, situacao, psicotropico, controleEspecial, etico, pageable);
    }

    @GetMapping("/lookup")
    public List<MedicamentoLookupItem> lookup(@RequestParam(defaultValue = "") String q, Pageable pageable) {
        return service.lookup(q, pageable);
    }

    @GetMapping("/{id}")
    public MedicamentoResponse get(@PathVariable Integer id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<MedicamentoResponse> create(@Valid @RequestBody MedicamentoCreateRequest req) {
        MedicamentoResponse body = service.create(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(body.id()).toUri();
        return ResponseEntity.created(location).body(body);
    }

    @PutMapping("/{id}")
    public MedicamentoResponse update(@PathVariable Integer id, @Valid @RequestBody MedicamentoUpdateRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

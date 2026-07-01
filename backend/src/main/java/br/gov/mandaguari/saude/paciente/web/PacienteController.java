package br.gov.mandaguari.saude.paciente.web;

import br.gov.mandaguari.saude.paciente.dto.PacienteDtos.*;
import br.gov.mandaguari.saude.paciente.service.PacienteService;
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
import java.util.List;

/**
 * Paciente API (SAU_PAC) — the most PHI-dense surface. Patient CRUD over the SYS_PES person supertype +
 * SAU_PAC subtype. Every read and write is audited in the service (LGPD). SAUDE_CADASTRO. Delete is
 * blocked when the patient has a controlled-substance prescription (Portaria 344/98).
 */
@RestController
@RequestMapping("/api/pacientes")
@Tag(name = "Pacientes")
@PreAuthorize("hasRole('SAUDE_CADASTRO')")
public class PacienteController {

    private final PacienteService service;

    public PacienteController(PacienteService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Buscar pacientes por nome, nome da mãe, prontuário, CPF ou CNS (paginado)")
    public Page<PacienteListItem> search(@RequestParam(required = false) String nome,
                                         @RequestParam(required = false) String nomeMae,
                                         @RequestParam(required = false) String prontuario,
                                         @RequestParam(required = false) String cpf,
                                         @RequestParam(required = false) String cns,
                                         @PageableDefault(size = 20) Pageable pageable) {
        return service.search(nome, nomeMae, prontuario, cpf, cns, pageable);
    }

    @GetMapping("/lookup")
    @Operation(summary = "Autocomplete de pacientes (por nome/CNS) para telas de prescrição")
    public List<PacienteListItem> lookup(@RequestParam(required = false, defaultValue = "") String q,
                                         @PageableDefault(size = 10) Pageable pageable) {
        return service.lookup(q, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter o cadastro completo de um paciente (leitura de PHI auditada)")
    public PacienteResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar paciente (cria a pessoa SYS_PES + o SAU_PAC)")
    public ResponseEntity<PacienteResponse> create(@RequestBody PacienteWriteRequest req,
                                                   UriComponentsBuilder uri) {
        PacienteResponse created = service.create(req);
        URI location = uri.path("/api/pacientes/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar o cadastro de um paciente (grava SAU_PAC + write-back SYS_PES)")
    public PacienteResponse update(@PathVariable Long id, @RequestBody PacienteWriteRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir paciente (bloqueado se há Receituário de Controle Especial — Portaria 344/98)")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

package br.gov.mandaguari.saude.programa.service;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.programa.domain.Programa;
import br.gov.mandaguari.saude.programa.dto.ProgramaDtos.*;
import br.gov.mandaguari.saude.programa.mapper.ProgramaMapper;
import br.gov.mandaguari.saude.programa.repository.GrupoProgramaRepository;
import br.gov.mandaguari.saude.programa.repository.ProgramaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Program catalog (SAU_PRG) + groups (SAU_PRGGRP). PrgCod is a client-supplied VARCHAR key (program
 * name). Programs are referenced by the permission matrices, so delete is guarded.
 */
@Service
@Transactional(readOnly = true)
public class ProgramaService {

    private final ProgramaRepository repo;
    private final GrupoProgramaRepository grupoRepo;
    private final ProgramaMapper mapper;
    private final AuditService audit;

    public ProgramaService(ProgramaRepository repo, GrupoProgramaRepository grupoRepo,
                           ProgramaMapper mapper, AuditService audit) {
        this.repo = repo;
        this.grupoRepo = grupoRepo;
        this.mapper = mapper;
        this.audit = audit;
    }

    public List<GrupoProgramaResponse> listGrupos() {
        return grupoRepo.findAll(Sort.by("nome")).stream().map(mapper::toGrupoResponse).toList();
    }

    public Page<ProgramaResponse> list(String q, Integer grupoId, Pageable pageable) {
        return repo.search(blank(q), grupoId, pageable).map(mapper::toResponse);
    }

    public ProgramaResponse get(String id) {
        return mapper.toResponse(find(id));
    }

    public List<ProgramaLookupItem> lookup(String q, Pageable pageable) {
        return repo.lookup(q == null ? "" : q, pageable).stream().map(mapper::toLookupItem).toList();
    }

    @Transactional
    public ProgramaResponse create(ProgramaCreateRequest req) {
        String id = req.id().trim();
        if (repo.existsById(id)) {
            throw new Conflict("Já existe programa com código " + id);
        }
        Programa p = new Programa();
        p.setId(id);
        apply(p, req.nome(), req.grupoId(), req.admin(), req.medico(), req.acessoPublico());
        Programa saved = repo.save(p);
        audit.record("CREATE", "SAU_PRG", saved.getId());
        return mapper.toResponse(saved);
    }

    @Transactional
    public ProgramaResponse update(String id, ProgramaUpdateRequest req) {
        Programa p = find(id);
        apply(p, req.nome(), req.grupoId(), req.admin(), req.medico(), req.acessoPublico());
        Programa saved = repo.save(p);
        audit.record("UPDATE", "SAU_PRG", saved.getId());
        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(String id) {
        Programa p = find(id);
        if (repo.isReferencedByPermission(id)) {
            throw new Conflict("Programa está vinculado a permissões (perfil/usuário) e não pode ser excluído");
        }
        repo.delete(p);
        audit.record("DELETE", "SAU_PRG", id);
    }

    // --- helpers ---

    private Programa find(String id) {
        return repo.findById(id).orElseThrow(() -> new NotFound("Programa " + id + " não encontrado"));
    }

    private static void apply(Programa p, String nome, Integer grupoId,
                              Boolean admin, Boolean medico, Boolean acessoPublico) {
        p.setNome(nome);
        p.setGrupoId(grupoId);
        p.setAdmin((short) (Boolean.TRUE.equals(admin) ? 1 : 0));
        p.setMedico((short) (Boolean.TRUE.equals(medico) ? 1 : 0));
        p.setAcessoPublico(Boolean.TRUE.equals(acessoPublico));
    }

    private static String blank(String s) { return (s == null || s.isBlank()) ? null : s; }
}

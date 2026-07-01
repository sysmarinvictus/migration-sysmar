package br.gov.mandaguari.saude.pessoa.service;

import br.gov.mandaguari.saude.common.audit.AuditService;
import br.gov.mandaguari.saude.common.error.DomainExceptions.BusinessRule;
import br.gov.mandaguari.saude.common.error.DomainExceptions.Conflict;
import br.gov.mandaguari.saude.common.error.DomainExceptions.NotFound;
import br.gov.mandaguari.saude.common.validation.CnsValidator;
import br.gov.mandaguari.saude.common.validation.CpfValidator;
import br.gov.mandaguari.saude.pessoa.domain.Pessoa;
import br.gov.mandaguari.saude.pessoa.dto.PessoaDtos.*;
import br.gov.mandaguari.saude.pessoa.repository.PessoaRepository;
import br.gov.mandaguari.saude.profissional.service.SoundexService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.regex.Pattern;

/**
 * SAU_PESF (Pessoa Física) — the WRITE path over the SYS_PES person supertype. Rules mined from
 * {@code sau_pesf_impl.java} (see SLICE-SPEC SAU_PESF), cited {@code // R<n>}. Scope is pure person
 * cadastro (OQ1 resolved): subtype auto-provisioning (R56–R58) is intentionally out of scope. Every
 * create/update/delete is audited (LGPD, common/audit).
 */
@Service
@Transactional(readOnly = true)
public class PessoaCadastroService {

    /** Nacionalidade: 1=brasileiro, 2=estrangeiro, 3=naturalizado. */
    private static final int NAC_BRASILEIRO = 1, NAC_ESTRANGEIRO = 2, NAC_NATURALIZADO = 3;
    /** R28/R29: país "Brasil" == código 10 (magic constant — confirm against SAU_PAIS seed, OQ). */
    private static final int BRASIL_PAIS_COD = 10;
    /** R24: cor/raça 5 == indígena → etnia required. */
    private static final int COR_INDIGENA = 5;
    private static final int MAX_IDADE = 130;                                  // R21
    private static final Pattern TELEFONE = Pattern.compile("\\(\\d{2}\\) 9?\\d{4}-\\d{4}"); // R17/R18

    private final PessoaRepository repo;
    private final SoundexService soundex;
    private final AuditService audit;

    public PessoaCadastroService(PessoaRepository repo, SoundexService soundex, AuditService audit) {
        this.repo = repo;
        this.soundex = soundex;
        this.audit = audit;
    }

    @Transactional
    public PessoaCadastroResponse create(PessoaCadastroRequest req) {
        Pessoa p = new Pessoa();
        long nextId = repo.findMaxPesCod().map(max -> max + 1L).orElse(1L);    // R49-PK (psau_inc_pes: MAX+1)
        p.setId(nextId);
        p.setTipoPessoa(2);                                                    // R48: pessoa física
        p.setDataCadastro(LocalDate.now());                                   // R49: data de cadastro
        if (req.situacaoFamiliarCod() == null) p.setSituacaoFamiliarCod(0);   // R50
        apply(p, req);
        validateAndDerive(p, req, null);
        Pessoa saved = repo.save(p);
        audit.record("CREATE", "SYS_PES", saved.getId());                     // LGPD
        return toResponse(saved);
    }

    @Transactional
    public PessoaCadastroResponse update(Long id, PessoaCadastroRequest req) {
        Pessoa p = repo.findById(id).orElseThrow(() -> new NotFound("Pessoa " + id + " não encontrada"));
        apply(p, req);
        if (p.getSituacaoFamiliarCod() == null) p.setSituacaoFamiliarCod(0);  // R50
        validateAndDerive(p, req, id);
        Pessoa saved = repo.save(p);
        audit.record("UPDATE", "SYS_PES", saved.getId());                     // LGPD
        return toResponse(saved);
    }

    /** Full cadastro read-back for the edit form (audited PHI read). */
    public PessoaCadastroResponse getCadastro(Long id) {
        Pessoa p = repo.findById(id).orElseThrow(() -> new NotFound("Pessoa " + id + " não encontrada"));
        audit.record("READ", "SYS_PES", id);                                  // LGPD
        return toResponse(p);
    }

    @Transactional
    public void delete(Long id) {
        Pessoa p = repo.findById(id).orElseThrow(() -> new NotFound("Pessoa " + id + " não encontrada"));
        if (repo.referencedByProfissional(id)) {                              // R53
            throw new Conflict("Pessoa não pode ser excluída: possui cadastro de Profissional");
        }
        if (repo.referencedByFuncionario(id)) {                               // R54
            throw new Conflict("Pessoa não pode ser excluída: possui cadastro de Funcionário");
        }
        if (repo.referencedByPaciente(id)) {                                  // R55
            throw new Conflict("Pessoa não pode ser excluída: possui cadastro de Paciente");
        }
        repo.delete(p);
        audit.record("DELETE", "SYS_PES", id);                                // LGPD
    }

    // --- validation + derivation (rules cited) ---

    private void validateAndDerive(Pessoa p, PessoaCadastroRequest req, Long selfId) {
        // Names (R1-R5, R39-R41)
        PessoaNomeValidator.validateRequired(p.getNome());
        PessoaNomeValidator.validateOptional(p.getNomePai(), "pes.nomePai", "Nome do Pai");
        PessoaNomeValidator.validateOptional(p.getNomeMae(), "pes.nomeMae", "Nome da Mãe");
        PessoaNomeValidator.validateSocial(p.getNomeSocial());

        // Documents — CNS required + valid (R42/R43)
        String cns = digits(p.getCns());
        if (cns == null) throw new BusinessRule("pes.cns.required", "Informe o número do CNS");
        if (!CnsValidator.isValidCns(cns)) throw new BusinessRule("pes.cns.invalid", "CNS inválido! Tente novamente.");
        // CPF optional but valid + unique when present (R44/R45)
        String cpf = digits(p.getCpfCnpj());
        if (cpf != null) {
            if (!CpfValidator.isValidCpf(cpf)) throw new BusinessRule("pes.cpf.invalid", "CPF inválido! Tente novamente.");
            var owners = repo.findCpfOwners(cpf, selfId == null ? -1L : selfId, PageRequest.of(0, 1)); // R45
            if (!owners.isEmpty()) {
                throw new Conflict("Este número de CPF está sendo utilizado pelo cadastro " + owners.get(0) + "! Por favor verifique.");
            }
        }
        if (p.getOrgaoEmissorCod() != null && !repo.orgaoEmissorExists(p.getOrgaoEmissorCod())) { // R47
            throw new BusinessRule("pes.orgaoEmissor.notfound", "Não existe Orgão Emissor");
        }

        // Birth / age (R19-R21)
        if (p.getDataNascimento() == null) throw new BusinessRule("pes.dataNascimento.required", "Informe a Data de Nascimento!");
        if (p.getDataNascimento().isAfter(LocalDate.now())) {
            throw new BusinessRule("pes.dataNascimento.future", "Data de Nascimento não pode ser maior que a data atual!");
        }
        if (Period.between(p.getDataNascimento(), LocalDate.now()).getYears() > MAX_IDADE) {
            throw new BusinessRule("pes.idade.max", "Idade não pode ser superior a 130 anos!");
        }

        // Sexo / cor / etnia (R22-R25)
        if (isBlank(p.getSexo())) throw new BusinessRule("pes.sexo.required", "Informe o Sexo da Pessoa!");
        if (p.getCorCod() == null) throw new BusinessRule("pes.cor.required", "Informe a Cor/Raça da Pessoa!");
        if (p.getCorCod() == COR_INDIGENA && p.getEtniaCod() == null) {
            throw new BusinessRule("pes.etnia.required", "Informe o Código da Etnia!");
        }
        if (p.getEtniaCod() != null && !repo.etniaExists(p.getEtniaCod())) {   // R25
            throw new BusinessRule("pes.etnia.notfound", "Não existe Etnia");
        }

        // Nationality cross-field (R26-R38)
        validateNacionalidade(p);

        // Address (R10-R16)
        // R11 DEFERRED (see SLICE-SPEC OQ8): CEP-must-be-valid-for-município (legacy psau_valcep) is not
        // enforced here — it needs the CEP↔município reference logic; format/required (R10) is enforced.
        if (isBlank(p.getCep())) throw new BusinessRule("pes.cep.required", "Informe o CEP!");
        if (p.getTipoLogradouroCod() == null) throw new BusinessRule("pes.tipoLogradouro.required", "Informe o código do logradouro");
        if (!repo.tipoLogradouroExists(p.getTipoLogradouroCod())) {
            throw new BusinessRule("pes.tipoLogradouro.notfound", "Não existe Tipo Logradouro");
        }
        if (isBlank(p.getEndereco())) throw new BusinessRule("pes.endereco.required", "Informe o logradouro!");
        if (isBlank(p.getEnderecoNumero())) throw new BusinessRule("pes.numero.required", "Informe o número!");
        if (!isNumeroValido(p.getEnderecoNumero())) throw new BusinessRule("pes.numero.invalid", "Número inválido");
        if (p.getBairroCod() == null) throw new BusinessRule("pes.bairro.required", "Informe o bairro!");
        if (!repo.bairroExists(p.getBairroCod())) throw new BusinessRule("pes.bairro.notfound", "Não existe Bairro");
        if (p.getMunicipioCod() == null) throw new BusinessRule("pes.municipio.required", "Informe o Município da Pessoa!");
        if (!repo.municipioExists(p.getMunicipioCod())) throw new BusinessRule("pes.municipio.notfound", "Não existe Município");

        // Contact formats (R17/R18)
        if (!isBlank(p.getTelefone()) && !TELEFONE.matcher(p.getTelefone().trim()).matches()) {
            throw new BusinessRule("pes.telefone.invalid", "Telefone inválido!");
        }
        if (!isBlank(p.getCelular()) && !TELEFONE.matcher(p.getCelular().trim()).matches()) {
            throw new BusinessRule("pes.celular.invalid", "Celular inválido!");
        }

        // Ocupação CBO (R46)
        if (!isBlank(p.getCboCod()) && !repo.cborExists(p.getCboCod())) {
            throw new BusinessRule("pes.cbo.notfound", "Não existe CBOR");
        }

        // Derivations (R51/R52)
        p.setNomeSoundex(soundex.compute(p.getNome()));
        p.setNomeMaeSoundex(soundex.compute(p.getNomeMae()));
        p.setNomeSocialSoundex(soundex.compute(p.getNomeSocial()));
    }

    private void validateNacionalidade(Pessoa p) {
        Integer nac = p.getNacionalidadeTipo();
        if (nac == null) throw new BusinessRule("pes.nacionalidade.required", "Informe a Nacionalidade!"); // R26

        if (nac != NAC_NATURALIZADO && p.getPaisCod() == null) {                                           // R27
            throw new BusinessRule("pes.pais.required", "Informe o Código do Pais de Origem!");
        }
        if (p.getPaisCod() != null) {
            if (nac == NAC_ESTRANGEIRO && p.getPaisCod() == BRASIL_PAIS_COD) {                             // R28
                throw new BusinessRule("pes.pais.notBrasil", "Pais de origem deve ser diferente do Brasil!");
            }
            if (nac == NAC_BRASILEIRO && p.getPaisCod() != BRASIL_PAIS_COD) {                              // R29
                throw new BusinessRule("pes.pais.brasil", "Pais de origem deve ser o Brasil!");
            }
            if (!repo.paisExists(p.getPaisCod())) throw new BusinessRule("pes.pais.notfound", "Não existe Pais"); // R34
        }
        if (nac == NAC_ESTRANGEIRO && p.getDataEntradaPais() == null) {                                    // R30
            throw new BusinessRule("pes.dataEntrada.required", "Informe a Data de Entrada no Brasil");
        }
        if (nac == NAC_NATURALIZADO) {
            if (p.getDataNaturalizacao() == null) throw new BusinessRule("pes.dataNaturalizacao.required", "Informe a Data de Naturalização"); // R31
            if (isBlank(p.getNumeroPortaria())) throw new BusinessRule("pes.portaria.required", "Informe a Portaria de Naturalização");       // R32
        }
        if (nac == NAC_BRASILEIRO) {                                                                       // R33
            if (p.getMunicipioNascCod() == null) throw new BusinessRule("pes.munNascimento.required", "Informe o município de nascimento!");
            if (!repo.municipioExists(p.getMunicipioNascCod())) throw new BusinessRule("pes.munNascimento.notfound", "Não existe Município de Nascimento");
        }
        LocalDate nasc = p.getDataNascimento();
        if (p.getDataNaturalizacao() != null) {
            if (nasc != null && p.getDataNaturalizacao().isBefore(nasc)) {                                 // R35
                throw new BusinessRule("pes.dataNaturalizacao.beforeBirth", "Data de naturalização não pode ser anterior a data de nascimento!");
            }
            if (p.getDataNaturalizacao().isAfter(LocalDate.now())) {                                       // R37
                throw new BusinessRule("pes.dataNaturalizacao.future", "Data de naturalização não pode ser maior que a data atual!");
            }
        }
        if (p.getDataEntradaPais() != null) {
            if (nasc != null && p.getDataEntradaPais().isBefore(nasc)) {                                   // R36
                throw new BusinessRule("pes.dataEntrada.beforeBirth", "Data de entrada não pode ser anterior a data de nascimento!");
            }
            if (p.getDataEntradaPais().isAfter(LocalDate.now())) {                                         // R38
                throw new BusinessRule("pes.dataEntrada.future", "Data de entrada não pode ser maior que a data atual!");
            }
        }
    }

    /** Copy all editable fields from the request onto the entity (soundex/id/tip/cadDat handled separately). */
    private void apply(Pessoa p, PessoaCadastroRequest r) {
        p.setNome(trim(r.nome()));
        p.setNomeSocial(trim(r.nomeSocial()));
        p.setUsaNomeSocial(r.usaNomeSocial());
        p.setNomePai(trim(r.nomePai()));
        p.setNomeMae(trim(r.nomeMae()));
        p.setNomeConjuge(trim(r.nomeConjuge()));
        p.setCpfCnpj(r.cpfCnpj());
        p.setCns(r.cns());
        p.setRgIe(r.rgIe());
        p.setOrgaoEmissorCod(r.orgaoEmissorCod());
        p.setRgUf(r.rgUf());
        p.setRgDataEmissao(r.rgDataEmissao());
        p.setDataNascimento(r.dataNascimento());
        p.setSexo(r.sexo());
        p.setCorCod(r.corCod());
        p.setEstadoCivilCod(r.estadoCivilCod());
        if (r.situacaoFamiliarCod() != null) p.setSituacaoFamiliarCod(r.situacaoFamiliarCod());
        p.setEtniaCod(r.etniaCod());
        p.setTipoSanguineo(r.tipoSanguineo());
        p.setNacionalidadeTipo(r.nacionalidadeTipo());
        p.setPaisCod(r.paisCod());
        p.setMunicipioNascCod(r.municipioNascCod());
        p.setDataNaturalizacao(r.dataNaturalizacao());
        p.setNumeroPortaria(r.numeroPortaria());
        p.setDataEntradaPais(r.dataEntradaPais());
        p.setCep(r.cep());
        p.setTipoLogradouroCod(r.tipoLogradouroCod());
        p.setEndereco(trim(r.endereco()));
        p.setEnderecoNumero(trim(r.enderecoNumero()));
        p.setEnderecoComplemento(trim(r.enderecoComplemento()));
        p.setBairroCod(r.bairroCod());
        p.setMunicipioCod(r.municipioCod());
        p.setTelefone(r.telefone());
        p.setCelular(r.celular());
        p.setFax(r.fax());
        p.setEmail(r.email());
        p.setHomePage(r.homePage());
        p.setCertidaoCivilTipo(r.certidaoCivilTipo());
        p.setCertidaoNumero(r.certidaoNumero());
        p.setCertidaoLivro(r.certidaoLivro());
        p.setCertidaoFolha(r.certidaoFolha());
        p.setCertidaoData(r.certidaoData());
        p.setCertidaoCartorio(r.certidaoCartorio());
        p.setCtpsSerie(r.ctpsSerie());
        p.setCtpsNumero(r.ctpsNumero());
        p.setCtpsUf(r.ctpsUf());
        p.setCtpsData(r.ctpsData());
        p.setPisPasep(r.pisPasep());
        p.setTituloEleitorNumero(r.tituloEleitorNumero());
        p.setTituloEleitorZona(r.tituloEleitorZona());
        p.setTituloEleitorSecao(r.tituloEleitorSecao());
        p.setNis(r.nis());
        p.setFrequentaEscola(r.frequentaEscola());
        p.setGrauEscolaridade(r.grauEscolaridade());
        p.setEscolaridade(r.escolaridade());
        p.setCboCod(r.cboCod());
        p.setObservacao(r.observacao());
        p.setGerarBpa(r.gerarBpa());
    }

    // --- helpers ---

    /** R14: número must be digits or 'SN' (sem número). */
    static boolean isNumeroValido(String n) {
        if (n == null) return false;
        String s = n.trim();
        return s.matches("\\d+") || s.toUpperCase().startsWith("SN");
    }

    private static PessoaCadastroResponse toResponse(Pessoa p) {
        return new PessoaCadastroResponse(
                p.getId(), p.getTipoPessoa(), p.getNome(), p.getNomeSocial(),
                Boolean.TRUE.equals(p.getUsaNomeSocial()), p.getNomeExibicao(),
                p.getNomePai(), p.getNomeMae(), p.getNomeConjuge(), p.getCpfCnpj(), p.getCns(),
                p.getRgIe(), p.getOrgaoEmissorCod(), p.getRgUf(), p.getRgDataEmissao(),
                p.getDataNascimento(), p.getSexo(), p.getCorCod(), p.getEstadoCivilCod(),
                p.getSituacaoFamiliarCod(), p.getEtniaCod(), p.getTipoSanguineo(),
                p.getNacionalidadeTipo(), p.getPaisCod(), p.getMunicipioNascCod(),
                p.getDataNaturalizacao(), p.getNumeroPortaria(), p.getDataEntradaPais(),
                p.getCep(), p.getTipoLogradouroCod(), p.getEndereco(), p.getEnderecoNumero(),
                p.getEnderecoComplemento(), p.getBairroCod(), p.getMunicipioCod(),
                p.getTelefone(), p.getCelular(), p.getFax(), p.getEmail(), p.getHomePage(),
                p.getCertidaoCivilTipo(), p.getCertidaoNumero(), p.getCertidaoLivro(),
                p.getCertidaoFolha(), p.getCertidaoData(), p.getCertidaoCartorio(),
                p.getCtpsSerie(), p.getCtpsNumero(), p.getCtpsUf(), p.getCtpsData(), p.getPisPasep(),
                p.getTituloEleitorNumero(), p.getTituloEleitorZona(), p.getTituloEleitorSecao(),
                p.getNis(), p.getFrequentaEscola(), p.getGrauEscolaridade(), p.getEscolaridade(),
                p.getCboCod(), p.getDataCadastro(), p.getObservacao(), p.getGerarBpa());
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String trim(String s) { return s == null ? null : s.trim(); }
    private static String digits(String s) { return (s == null || s.isBlank()) ? null : s.replaceAll("\\D", ""); }
}

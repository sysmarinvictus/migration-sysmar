package br.gov.mandaguari.saude.especialidade.mapper;

import br.gov.mandaguari.saude.especialidade.domain.Especialidade;
import br.gov.mandaguari.saude.especialidade.dto.EspecialidadeDtos.AgendaParametros;
import br.gov.mandaguari.saude.especialidade.dto.EspecialidadeDtos.EspecialidadeLookupItem;
import br.gov.mandaguari.saude.especialidade.dto.EspecialidadeDtos.EspecialidadeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapping between the SAU_ESP entity and DTOs. The flat agenda* columns on the entity are
 * grouped into the {@link AgendaParametros} record on the DTO side.
 */
@Mapper(componentModel = "spring")
public interface EspecialidadeMapper {

    @Mapping(target = "cborDescricao", ignore = true) // set by the service (R3 lookup)
    @Mapping(target = "agenda", source = "e")
    EspecialidadeResponse toResponse(Especialidade e);

    EspecialidadeLookupItem toLookupItem(Especialidade e);

    @Mapping(target = "estagnadoMuitoUrgente", source = "agendaEstagnadoMuitoUrgente")
    @Mapping(target = "estagnadoUrgente",      source = "agendaEstagnadoUrgente")
    @Mapping(target = "estagnadoPrioritario",  source = "agendaEstagnadoPrioritario")
    @Mapping(target = "estagnadoNormal",       source = "agendaEstagnadoNormal")
    @Mapping(target = "tempoMaxMuitoUrgente",  source = "agendaTempoMaxMuitoUrgente")
    @Mapping(target = "tempoMaxUrgente",       source = "agendaTempoMaxUrgente")
    @Mapping(target = "tempoMaxPrioritario",   source = "agendaTempoMaxPrioritario")
    @Mapping(target = "tempoMaxNormal",        source = "agendaTempoMaxNormal")
    @Mapping(target = "vagaMuitoUrgenteMin",   source = "agendaVagaMuitoUrgenteMin")
    @Mapping(target = "vagaMuitoUrgenteMax",   source = "agendaVagaMuitoUrgenteMax")
    @Mapping(target = "vagaUrgenteMin",        source = "agendaVagaUrgenteMin")
    @Mapping(target = "vagaUrgenteMax",        source = "agendaVagaUrgenteMax")
    @Mapping(target = "vagaPrioritarioMin",    source = "agendaVagaPrioritarioMin")
    @Mapping(target = "vagaPrioritarioMax",    source = "agendaVagaPrioritarioMax")
    @Mapping(target = "vagaNormalMin",         source = "agendaVagaNormalMin")
    @Mapping(target = "vagaNormalMax",         source = "agendaVagaNormalMax")
    AgendaParametros toAgenda(Especialidade e);

    /** Apply agenda parameters from a DTO record onto the entity (used on create/update). */
    default void applyAgenda(Especialidade e, AgendaParametros a) {
        if (a == null) return;
        e.setAgendaEstagnadoMuitoUrgente(a.estagnadoMuitoUrgente());
        e.setAgendaEstagnadoUrgente(a.estagnadoUrgente());
        e.setAgendaEstagnadoPrioritario(a.estagnadoPrioritario());
        e.setAgendaEstagnadoNormal(a.estagnadoNormal());
        e.setAgendaTempoMaxMuitoUrgente(a.tempoMaxMuitoUrgente());
        e.setAgendaTempoMaxUrgente(a.tempoMaxUrgente());
        e.setAgendaTempoMaxPrioritario(a.tempoMaxPrioritario());
        e.setAgendaTempoMaxNormal(a.tempoMaxNormal());
        e.setAgendaVagaMuitoUrgenteMin(a.vagaMuitoUrgenteMin());
        e.setAgendaVagaMuitoUrgenteMax(a.vagaMuitoUrgenteMax());
        e.setAgendaVagaUrgenteMin(a.vagaUrgenteMin());
        e.setAgendaVagaUrgenteMax(a.vagaUrgenteMax());
        e.setAgendaVagaPrioritarioMin(a.vagaPrioritarioMin());
        e.setAgendaVagaPrioritarioMax(a.vagaPrioritarioMax());
        e.setAgendaVagaNormalMin(a.vagaNormalMin());
        e.setAgendaVagaNormalMax(a.vagaNormalMax());
    }
}

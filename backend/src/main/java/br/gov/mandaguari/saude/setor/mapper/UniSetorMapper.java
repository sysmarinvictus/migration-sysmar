package br.gov.mandaguari.saude.setor.mapper;

import br.gov.mandaguari.saude.setor.domain.UniSetor;
import br.gov.mandaguari.saude.setor.dto.UniSetorDtos.UniSetorLookupItem;
import br.gov.mandaguari.saude.setor.dto.UniSetorDtos.UniSetorResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** MapStruct mapping between SAU_UNISETOR entity and DTOs. */
@Mapper(componentModel = "spring")
public interface UniSetorMapper {

    @Mapping(target = "unidadeNome",     ignore = true)
    @Mapping(target = "unidadeCnes",     ignore = true)
    @Mapping(target = "unidadeSituacao", ignore = true)
    UniSetorResponse toResponse(UniSetor s);

    UniSetorLookupItem toLookupItem(UniSetor s);
}

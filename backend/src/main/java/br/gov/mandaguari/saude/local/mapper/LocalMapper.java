package br.gov.mandaguari.saude.local.mapper;

import br.gov.mandaguari.saude.local.domain.Local;
import br.gov.mandaguari.saude.local.dto.LocalDtos.LocalLookupItem;
import br.gov.mandaguari.saude.local.dto.LocalDtos.LocalResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** MapStruct mapping between the SAU_LOC entity and DTOs. */
@Mapper(componentModel = "spring")
public interface LocalMapper {

    @Mapping(target = "municipioNome", ignore = true)  // set by the service (R4 lookup)
    @Mapping(target = "municipioUf", ignore = true)
    @Mapping(target = "municipioIbge", ignore = true)
    LocalResponse toResponse(Local l);

    LocalLookupItem toLookupItem(Local l);
}

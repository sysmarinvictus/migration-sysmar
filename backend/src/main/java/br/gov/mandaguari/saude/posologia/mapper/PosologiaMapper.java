package br.gov.mandaguari.saude.posologia.mapper;

import br.gov.mandaguari.saude.posologia.domain.Posologia;
import br.gov.mandaguari.saude.posologia.dto.PosologiaDtos.PosologiaLookupItem;
import br.gov.mandaguari.saude.posologia.dto.PosologiaDtos.PosologiaResponse;
import org.mapstruct.Mapper;

/** MapStruct mapping between SAU_REMOBS entity and DTOs. */
@Mapper(componentModel = "spring")
public interface PosologiaMapper {

    PosologiaResponse toResponse(Posologia p);

    PosologiaLookupItem toLookupItem(Posologia p);
}

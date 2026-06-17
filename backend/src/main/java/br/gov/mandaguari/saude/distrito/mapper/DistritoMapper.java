package br.gov.mandaguari.saude.distrito.mapper;

import br.gov.mandaguari.saude.distrito.domain.Distrito;
import br.gov.mandaguari.saude.distrito.dto.DistritoDtos.DistritoLookupItem;
import br.gov.mandaguari.saude.distrito.dto.DistritoDtos.DistritoResponse;
import org.mapstruct.Mapper;

/** MapStruct mapping between SAU_DIS entity and DTOs. */
@Mapper(componentModel = "spring")
public interface DistritoMapper {

    DistritoResponse toResponse(Distrito d);

    DistritoLookupItem toLookupItem(Distrito d);
}

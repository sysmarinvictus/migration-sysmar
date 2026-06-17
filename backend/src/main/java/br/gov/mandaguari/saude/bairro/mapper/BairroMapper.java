package br.gov.mandaguari.saude.bairro.mapper;

import br.gov.mandaguari.saude.bairro.domain.Bairro;
import br.gov.mandaguari.saude.bairro.dto.BairroDtos.BairroLookupItem;
import br.gov.mandaguari.saude.bairro.dto.BairroDtos.BairroResponse;
import org.mapstruct.Mapper;

/** MapStruct mapping between SAU_BAI entity and DTOs. */
@Mapper(componentModel = "spring")
public interface BairroMapper {

    BairroResponse toResponse(Bairro b);

    BairroLookupItem toLookupItem(Bairro b);
}

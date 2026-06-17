package br.gov.mandaguari.saude.unidade.mapper;

import br.gov.mandaguari.saude.unidade.domain.Unidade;
import br.gov.mandaguari.saude.unidade.dto.UnidadeDtos.*;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UnidadeMapper {
    UnidadeResponse toResponse(Unidade u);
    UnidadeLookupItem toLookupItem(Unidade u);
}

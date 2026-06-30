package br.gov.mandaguari.saude.perfil.mapper;

import br.gov.mandaguari.saude.perfil.domain.Perfil;
import br.gov.mandaguari.saude.perfil.dto.PerfilDtos.PerfilLookupItem;
import br.gov.mandaguari.saude.perfil.dto.PerfilDtos.PerfilResponse;
import org.mapstruct.Mapper;

/** Entity ↔ DTO mapping for Perfil. */
@Mapper(componentModel = "spring")
public interface PerfilMapper {

    PerfilResponse toResponse(Perfil p);

    PerfilLookupItem toLookupItem(Perfil p);
}

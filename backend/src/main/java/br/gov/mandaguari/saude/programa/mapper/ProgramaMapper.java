package br.gov.mandaguari.saude.programa.mapper;

import br.gov.mandaguari.saude.programa.domain.GrupoPrograma;
import br.gov.mandaguari.saude.programa.domain.Programa;
import br.gov.mandaguari.saude.programa.dto.ProgramaDtos.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Entity ↔ DTO mapping for the program catalog. smallint flags → boolean (==1). */
@Mapper(componentModel = "spring")
public interface ProgramaMapper {

    GrupoProgramaResponse toGrupoResponse(GrupoPrograma g);

    @Mapping(target = "admin", expression = "java(p.getAdmin() != null && p.getAdmin() == 1)")
    @Mapping(target = "medico", expression = "java(p.getMedico() != null && p.getMedico() == 1)")
    @Mapping(target = "acessoPublico", expression = "java(Boolean.TRUE.equals(p.getAcessoPublico()))")
    ProgramaResponse toResponse(Programa p);

    ProgramaLookupItem toLookupItem(Programa p);
}

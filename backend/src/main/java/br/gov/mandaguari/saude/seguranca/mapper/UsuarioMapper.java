package br.gov.mandaguari.saude.seguranca.mapper;

import br.gov.mandaguari.saude.seguranca.domain.Usuario;
import br.gov.mandaguari.saude.seguranca.dto.UsuarioDtos.UsuarioLookupItem;
import br.gov.mandaguari.saude.seguranca.dto.UsuarioDtos.UsuarioResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Entity ↔ DTO mapping for Usuario. Secrets (senha/chaveSenha) are NEVER mapped to any DTO. */
@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(target = "bloqueado", expression = "java(u.getBloqueado() != null && u.getBloqueado() == 1)")
    @Mapping(target = "superusuario", expression = "java(Boolean.TRUE.equals(u.getSuperusuario()))")
    UsuarioResponse toResponse(Usuario u);

    UsuarioLookupItem toLookupItem(Usuario u);
}

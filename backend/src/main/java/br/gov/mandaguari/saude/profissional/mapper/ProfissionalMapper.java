package br.gov.mandaguari.saude.profissional.mapper;

import br.gov.mandaguari.saude.profissional.domain.Profissional;
import br.gov.mandaguari.saude.profissional.dto.ProfissionalDtos.ProfissionalResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity → DTO mapping. Person (SYS_PES) and conselho (SAU_CONCLA) display fields are not on the
 * entity — they are joined natively and filled by the service, so they are ignored here. Secret/blob
 * fields are intentionally absent from {@link ProfissionalResponse}.
 */
@Mapper(componentModel = "spring")
public interface ProfissionalMapper {

    @Mapping(target = "conselhoClasseNome", ignore = true)
    @Mapping(target = "conselhoClasseSigla", ignore = true)
    @Mapping(target = "nome", ignore = true)
    @Mapping(target = "cpfCnpj", ignore = true)
    @Mapping(target = "rgIe", ignore = true)
    @Mapping(target = "sexo", ignore = true)
    @Mapping(target = "dataNascimento", ignore = true)
    @Mapping(target = "endereco", ignore = true)
    @Mapping(target = "telefone", ignore = true)
    @Mapping(target = "celular", ignore = true)
    ProfissionalResponse toResponse(Profissional p);
}

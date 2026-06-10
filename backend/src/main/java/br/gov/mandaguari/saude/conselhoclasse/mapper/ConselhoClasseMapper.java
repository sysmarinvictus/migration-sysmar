package br.gov.mandaguari.saude.conselhoclasse.mapper;

import br.gov.mandaguari.saude.conselhoclasse.domain.ConselhoClasse;
import br.gov.mandaguari.saude.conselhoclasse.dto.ConselhoClasseDtos.ConselhoClasseLookupItem;
import br.gov.mandaguari.saude.conselhoclasse.dto.ConselhoClasseDtos.ConselhoClasseResponse;
import org.mapstruct.Mapper;

/** MapStruct mapping between the SAU_CONCLA entity and DTOs (flat — no nested records). */
@Mapper(componentModel = "spring")
public interface ConselhoClasseMapper {

    ConselhoClasseResponse toResponse(ConselhoClasse c);

    ConselhoClasseLookupItem toLookupItem(ConselhoClasse c);
}

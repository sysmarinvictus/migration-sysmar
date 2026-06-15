package br.gov.mandaguari.saude.tipomedicamento.mapper;

import br.gov.mandaguari.saude.tipomedicamento.domain.TipoMedicamento;
import br.gov.mandaguari.saude.tipomedicamento.dto.TipoMedicamentoDtos.TipoMedicamentoLookupItem;
import br.gov.mandaguari.saude.tipomedicamento.dto.TipoMedicamentoDtos.TipoMedicamentoResponse;
import org.mapstruct.Mapper;

/** MapStruct mapping between the SAU_TIPREM entity and DTOs (flat). */
@Mapper(componentModel = "spring")
public interface TipoMedicamentoMapper {

    TipoMedicamentoResponse toResponse(TipoMedicamento t);

    TipoMedicamentoLookupItem toLookupItem(TipoMedicamento t);
}

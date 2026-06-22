package br.gov.mandaguari.saude.medicamento.mapper;

import br.gov.mandaguari.saude.medicamento.domain.Medicamento;
import br.gov.mandaguari.saude.medicamento.dto.MedicamentoDtos.MedicamentoLookupItem;
import br.gov.mandaguari.saude.medicamento.dto.MedicamentoDtos.MedicamentoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MedicamentoMapper {

    @Mapping(target = "renameDescricao", source = "renameDescricao")
    @Mapping(target = "posologiaCount", source = "posologiaCount")
    MedicamentoResponse toResponse(Medicamento m, String renameDescricao, long posologiaCount);

    MedicamentoLookupItem toLookupItem(Medicamento m);
}

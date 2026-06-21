package br.gov.mandaguari.saude.impedimento.mapper;

import br.gov.mandaguari.saude.impedimento.domain.Impedimento;
import br.gov.mandaguari.saude.impedimento.dto.ImpedimentoDtos.ImpedimentoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ImpedimentoMapper {

    @Mapping(target = "especialidadeCodigo", source = "especialidade.codigo")
    @Mapping(target = "especialidadeNome", source = "especialidade.nome")
    @Mapping(target = "profissionalNome", ignore = true)
    @Mapping(target = "profissionalSituacao", ignore = true)
    @Mapping(target = "cboCode", ignore = true)
    @Mapping(target = "cboDescricao", ignore = true)
    ImpedimentoResponse toResponse(Impedimento impedimento);
}

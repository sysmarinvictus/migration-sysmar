package br.gov.mandaguari.saude.formaapresentacao.mapper;

import br.gov.mandaguari.saude.formaapresentacao.domain.FormaApresentacao;
import br.gov.mandaguari.saude.formaapresentacao.dto.FormaApresentacaoDtos.FormaApresentacaoLookupItem;
import br.gov.mandaguari.saude.formaapresentacao.dto.FormaApresentacaoDtos.FormaApresentacaoResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FormaApresentacaoMapper {

    FormaApresentacaoResponse toResponse(FormaApresentacao f);

    FormaApresentacaoLookupItem toLookupItem(FormaApresentacao f);
}

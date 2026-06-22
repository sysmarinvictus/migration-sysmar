package br.gov.mandaguari.saude.especialidade.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** EspSit is SMALLINT (1/2) in the live DB; the entity/DTO expose a 1-char String ("1"/"2"). */
@Converter
public class SituacaoToShortConverter implements AttributeConverter<String, Short> {
    @Override public Short convertToDatabaseColumn(String v) {
        if (v == null || v.isBlank()) return null;
        return Short.parseShort(v.trim());
    }
    @Override public String convertToEntityAttribute(Short v) {
        return v == null ? null : String.valueOf(v);
    }
}

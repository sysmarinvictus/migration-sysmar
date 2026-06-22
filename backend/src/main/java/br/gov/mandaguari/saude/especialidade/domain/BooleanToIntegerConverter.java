package br.gov.mandaguari.saude.especialidade.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** EspAux is INTEGER (0/1) in the live DB; the entity exposes a Boolean. */
@Converter
public class BooleanToIntegerConverter implements AttributeConverter<Boolean, Integer> {
    @Override public Integer convertToDatabaseColumn(Boolean v) {
        return v == null ? null : (v ? 1 : 0);
    }
    @Override public Boolean convertToEntityAttribute(Integer v) {
        return v == null ? null : v != 0;
    }
}

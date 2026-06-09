package com.bytmasoft.dm.enums;


import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DomainTypeConverter implements AttributeConverter<DomainType, Integer> {

  @Override
  public Integer convertToDatabaseColumn(DomainType attribute) {
    return attribute != null ? attribute.getValue() : null;
  }

  @Override
  public DomainType convertToEntityAttribute(Integer dbData) {
    return dbData != null ? DomainType.forValue(dbData) : null;
  }
}

package com.bytmasoft.dm.enums;


import com.bytmasoft.dm.exception.StorageFileNotFoundException;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;

public enum DomainType {
  ITEM(1, "Use for Found and Lost Items in Dalilak App"),
  STUDENT(2, "Use for School documents"),
  VEHICLE(3, "Use for Vehicle photos"),

  OTHER(90, "Use for other");

  private final Integer value;
  private final String description;

  DomainType(int value, String description) {
    this.value = value;
    this.description = description;
  }

  public int getValue() {
    return value;
  }

  public String getDescription() {
    return description;
  }


  @JsonCreator
  public static DomainType forValue(int value) {
    return Arrays.stream(values())
        .filter(type -> type.value == value)
        .findFirst()
        .orElseThrow(() -> new StorageFileNotFoundException("" + value));
  }

  public static DomainType forDescription(String description) {
    return Arrays.stream(values())
        .filter(type -> type.description.equalsIgnoreCase(description))
        .findFirst()
        .orElseThrow(() -> new StorageFileNotFoundException(description));

  }

}

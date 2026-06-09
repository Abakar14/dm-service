package com.bytmasoft.dm.exception;

import com.bytmasoft.dm.exception.enums.DmErrorCode;
import com.bytmasoft.starter.exception.base.BusinessException;

public class InvalidFileTypeException extends BusinessException {

  public InvalidFileTypeException(String message) {
    super(DmErrorCode.INVALID_FILE_TYPE, message);
  }
}
package com.bytmasoft.dm.exception;

import com.bytmasoft.dm.exception.enums.DmErrorCode;
import com.bytmasoft.starter.exception.api.ErrorCode;
import com.bytmasoft.starter.exception.base.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidFileException extends BusinessException {

  public InvalidFileException(ErrorCode errorCode) {
    super(errorCode);
  }

  public InvalidFileException(String message) {
    super(DmErrorCode.INVALID_FILE, message);
  }

  public InvalidFileException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }

  public InvalidFileException(ErrorCode errorCode, String message, HttpStatus status) {
    super(errorCode, message, status);
  }
}

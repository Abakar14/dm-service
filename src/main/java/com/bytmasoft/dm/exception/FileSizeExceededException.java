package com.bytmasoft.dm.exception;

import com.bytmasoft.dm.exception.enums.DmErrorCode;
import com.bytmasoft.starter.exception.base.BusinessException;

public class FileSizeExceededException extends BusinessException {

  public FileSizeExceededException(String message) {
    super(DmErrorCode.PAYLOAD_TOO_LARGE, message);
  }
}

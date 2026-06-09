package com.bytmasoft.dm.exception;

import com.bytmasoft.dm.exception.enums.DmErrorCode;
import com.bytmasoft.starter.exception.base.BusinessException;

/**
 * Base exception for DM storage errors
 */
public class StorageException extends BusinessException {

  public StorageException(String message) {
    super(DmErrorCode.STORAGE_ERROR, message);
  }

  public StorageException(String message, Throwable cause) {
    super(DmErrorCode.STORAGE_ERROR, message);
    initCause(cause);
  }
}
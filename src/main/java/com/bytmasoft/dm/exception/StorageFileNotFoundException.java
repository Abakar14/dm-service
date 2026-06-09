package com.bytmasoft.dm.exception;

import com.bytmasoft.dm.exception.enums.DmErrorCode;
import com.bytmasoft.starter.exception.base.NotFoundException;

public class StorageFileNotFoundException extends NotFoundException {

  public StorageFileNotFoundException(String message) {
    super(DmErrorCode.FILE_NOT_FOUND, message);
  }
}
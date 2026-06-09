package com.bytmasoft.dm.exception;

import com.bytmasoft.starter.exception.api.ErrorCode;
import com.bytmasoft.starter.exception.base.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * InvalidRequestException
 *
 * @author Mahamat Abakar
 * @since 14.02.26
 */
public class InvalidRequestException extends BusinessException {

  public InvalidRequestException(ErrorCode errorCode) {
    super(errorCode);
  }

  public InvalidRequestException(ErrorCode errorCode, String message, HttpStatus status) {
    super(errorCode, message, status);
  }

  public InvalidRequestException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
package com.bytmasoft.dm.exception.enums;

import com.bytmasoft.starter.exception.api.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * DmErrorCode
 *
 * @author Mahamat Abakar
 * @since 11.04.26
 */
public enum DmErrorCode implements ErrorCode {

  PAYLOAD_TOO_LARGE(
      "ERRORS.DM.PAYLOAD_TOO_LARGE",
      "Uploaded payload is too large",
      HttpStatus.PAYLOAD_TOO_LARGE
  ),

  INVALID_FILE(
      "ERRORS.DM.INVALID_FILE",
      "Invalid file",
      HttpStatus.BAD_REQUEST
  ),

  INVALID_FILE_TYPE(
      "ERRORS.DM.INVALID_FILE_TYPE",
      "Unsupported file type",
      HttpStatus.UNSUPPORTED_MEDIA_TYPE
  ),

  INVALID_REQUEST(
      "ERRORS.DM.INVALID_REQUEST",
      "Invalid request",
      HttpStatus.BAD_REQUEST
  ),

  INVALID_MULTIPART_CLIENT_IDS(
      "ERRORS.DM.INVALID_MULTIPART_CLIENT_IDS",
      "ClientIds must match files count",
      HttpStatus.BAD_REQUEST
  ),

  EMPTY_FILE(
      "ERRORS.DM.EMPTY_FILE",
      "File is empty",
      HttpStatus.BAD_REQUEST
  ),

  STORAGE_ERROR(
      "ERRORS.DM.STORAGE_ERROR",
      "Storage operation failed",
      HttpStatus.INTERNAL_SERVER_ERROR
  ),

  FILE_NOT_FOUND(
      "ERRORS.DM.FILE_NOT_FOUND",
      "File not found",
      HttpStatus.NOT_FOUND
  ),

  MISSING_FILE_PART(
      "ERRORS.DM.MISSING_FILE_PART",
      "Required file part is missing",
      HttpStatus.BAD_REQUEST
  ),

  VALIDATION_ERROR(
      "ERRORS.DM.VALIDATION_ERROR",
      "Validation failed",
      HttpStatus.BAD_REQUEST
  );

  private final String messageKey;
  private final String defaultMessage;
  private final HttpStatus httpStatus;

  DmErrorCode(String messageKey, String defaultMessage, HttpStatus httpStatus) {
    this.messageKey = messageKey;
    this.defaultMessage = defaultMessage;
    this.httpStatus = httpStatus;
  }

  @Override
  public String getMessageKey() {
    return messageKey;
  }

  @Override
  public String getDefaultMessage() {
    return defaultMessage;
  }

  @Override
  public HttpStatus getHttpStatus() {
    return httpStatus;
  }
}
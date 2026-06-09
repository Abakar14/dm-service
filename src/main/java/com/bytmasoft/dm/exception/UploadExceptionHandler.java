package com.bytmasoft.dm.exception;

import com.bytmasoft.dm.exception.enums.DmErrorCode;
import com.bytmasoft.starter.exception.api.ApiError;
import com.bytmasoft.starter.exception.api.CommonErrorCode;

import com.bytmasoft.starter.exception.api.ServletCorrelationIdResolver;
import com.bytmasoft.starter.exception.base.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
@RequiredArgsConstructor
public class UploadExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiError> handleBusinessException(
      BusinessException ex,
      HttpServletRequest request
  ) {
    return ResponseEntity.status(ex.getStatus())
        .body(ApiError.simple(
            ex.getMessageKey(),
            ex.getMessage(),
            request.getRequestURI(),
            ServletCorrelationIdResolver.from(request)
        ));
  }


  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpServletRequest request
  ) {
    Map<String, Object> errors = new LinkedHashMap<>();

    for (FieldError err : ex.getBindingResult().getFieldErrors()) {
      errors.put(err.getField(), err.getDefaultMessage());
    }

    return ResponseEntity.badRequest().body(
        ApiError.withDetails(
            CommonErrorCode.VALIDATION_ERROR.getMessageKey(),
            CommonErrorCode.VALIDATION_ERROR.getDefaultMessage(),
            request.getRequestURI(),
            ServletCorrelationIdResolver.from(request),
            errors
        )
    );
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiError> handleConstraintViolation(
      ConstraintViolationException ex,
      HttpServletRequest request
  ) {
    Map<String, Object> errors = new LinkedHashMap<>();

    for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
      String field = violation.getPropertyPath() != null
          ? violation.getPropertyPath().toString()
          : "request";
      errors.put(field, violation.getMessage());
    }

    return ResponseEntity.badRequest().body(
        ApiError.withDetails(
            CommonErrorCode.VALIDATION_ERROR.getMessageKey(),
            CommonErrorCode.VALIDATION_ERROR.getDefaultMessage(),
            request.getRequestURI(),
            ServletCorrelationIdResolver.from(request),
            errors
        )
    );
  }

  @ExceptionHandler(MissingServletRequestPartException.class)
  public ResponseEntity<ApiError> handleMissingPart(
      MissingServletRequestPartException ex,
      HttpServletRequest request
  ) {
    return ResponseEntity.badRequest().body(
        ApiError.simple(
            DmErrorCode.MISSING_FILE_PART.getMessageKey(),
            DmErrorCode.MISSING_FILE_PART.getDefaultMessage(),
            request.getRequestURI(),
            ServletCorrelationIdResolver.from(request)
        )
    );
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ApiError> handleMissingParam(
      MissingServletRequestParameterException ex,
      HttpServletRequest request
  ) {
    return ResponseEntity.badRequest().body(
        ApiError.simple(
            CommonErrorCode.INVALID_REQUEST.getMessageKey(),
            "Missing required request parameter: " + ex.getParameterName(),
            request.getRequestURI(),
            ServletCorrelationIdResolver.from(request)
        )
    );
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiError> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex,
      HttpServletRequest request
  ) {
    return ResponseEntity.badRequest().body(
        ApiError.simple(
            CommonErrorCode.INVALID_REQUEST.getMessageKey(),
            "Invalid parameter: " + ex.getName(),
            request.getRequestURI(),
            ServletCorrelationIdResolver.from(request)
        )
    );
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleNotReadable(
      HttpMessageNotReadableException ex,
      HttpServletRequest request
  ) {
    return ResponseEntity.badRequest().body(
        ApiError.simple(
            CommonErrorCode.INVALID_REQUEST.getMessageKey(),
            CommonErrorCode.INVALID_REQUEST.getDefaultMessage(),
            request.getRequestURI(),
            ServletCorrelationIdResolver.from(request)
        )
    );
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiError> handleMaxUploadSizeExceededException(
      MaxUploadSizeExceededException ex,
      HttpServletRequest request
  ) {
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
        .body(ApiError.withDetails(
            DmErrorCode.PAYLOAD_TOO_LARGE.getMessageKey(),
            DmErrorCode.PAYLOAD_TOO_LARGE.getDefaultMessage(),
            request.getRequestURI(),
            ServletCorrelationIdResolver.from(request),
            uploadLimitsDetails()
        ));
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ApiError> handleHttpMediaTypeNotSupportedException(
      HttpMediaTypeNotSupportedException ex,
      HttpServletRequest request
  ) {
    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        .body(ApiError.simple(
            DmErrorCode.INVALID_FILE_TYPE.getMessageKey(),
            DmErrorCode.INVALID_FILE_TYPE.getDefaultMessage(),
            request.getRequestURI(),
            ServletCorrelationIdResolver.from(request)
        ));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> handleIllegalArgument(
      IllegalArgumentException ex,
      HttpServletRequest request
  ) {
    log.warn("[{}] Validation error for path {}: {}",
        ServletCorrelationIdResolver.from(request), request.getRequestURI(), ex.getMessage());

    return ResponseEntity.badRequest().body(
        ApiError.simple(
            CommonErrorCode.VALIDATION_ERROR.getMessageKey(),
            ex.getMessage(),
            request.getRequestURI(),
            ServletCorrelationIdResolver.from(request)
        )
    );
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleGeneric(
      Exception ex,
      HttpServletRequest request
  ) {
    log.error("[{}] Unhandled exception for path {}",
        ServletCorrelationIdResolver.from(request), request.getRequestURI(), ex);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiError.simple(
            CommonErrorCode.INTERNAL_SERVER_ERROR.getMessageKey(),
            CommonErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage(),
            request.getRequestURI(),
            ServletCorrelationIdResolver.from(request)
        ));
  }


  private Map<String, Object> uploadLimitsDetails() {
    Map<String, Object> details = new HashMap<>();
    details.put("maxTotalMb", 20);
    details.put("maxFileMb", 5);
    details.put("maxFiles", 5);
    return details;
  }


}
package com.bytmasoft.dm.security;

import com.bytmasoft.starter.exception.api.ApiError;
import com.bytmasoft.starter.exception.api.CommonErrorCode;

import com.bytmasoft.starter.exception.api.ServletCorrelationIdResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * ApiAccessDeniedHandler
 *
 * @author Mahamat Abakar
 * @since 21.04.26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException
  ) throws IOException {

    String correlationId = ServletCorrelationIdResolver.from(request);

    log.warn("[{}] Access denied to {}: {}",
        correlationId, request.getRequestURI(), accessDeniedException.getMessage());

    ApiError error = new ApiError(
        CommonErrorCode.ACCESS_DENIED.getMessageKey(),
        CommonErrorCode.ACCESS_DENIED.getDefaultMessage(),
        request.getRequestURI(),
        Instant.now(),
        null,
        correlationId,
        null
    );

    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    objectMapper.writeValue(response.getWriter(), error);
  }
}
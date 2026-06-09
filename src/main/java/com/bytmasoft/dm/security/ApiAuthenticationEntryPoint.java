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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * ApiAuthenticationEntryPoint
 *
 * @author Mahamat Abakar
 * @since 21.04.26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {


  private final ObjectMapper objectMapper;

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException
  ) throws IOException {

    String correlationId = ServletCorrelationIdResolver.from(request);

    log.warn("[{}] Unauthorized access to {}: {}",
        correlationId, request.getRequestURI(), authException.getMessage());

    ApiError error = new ApiError(
        CommonErrorCode.UNAUTHORIZED.getMessageKey(),
        CommonErrorCode.UNAUTHORIZED.getDefaultMessage(),
        request.getRequestURI(),
        Instant.now(),
        null,
        correlationId,
        null
    );

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    objectMapper.writeValue(response.getWriter(), error);
  }
}
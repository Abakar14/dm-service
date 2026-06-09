package com.bytmasoft.dm.security.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * CorrelationIdFilter
 *
 * @author Mahamat Abakar
 * @since 21.02.26
 */

@Component
public class CorrelationIdFilter implements Filter {

  private static final String HEADER = "X-Correlation-Id";
  private static final String MDC_KEY = "correlationId";

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest req = (HttpServletRequest) request;
    String cid = Optional.ofNullable(req.getHeader(HEADER)).filter(s -> !s.isBlank())
        .orElse(UUID.randomUUID().toString());

    MDC.put(MDC_KEY, cid);
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }
}

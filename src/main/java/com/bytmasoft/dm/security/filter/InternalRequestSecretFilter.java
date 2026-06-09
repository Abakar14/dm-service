package com.bytmasoft.dm.security.filter;

import com.bytmasoft.dm.config.DmSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Defense-in-depth: if a shared secret is configured, require it for internal/admin requests. This
 * protects the service if it is accidentally exposed without Gateway/BFF in front.
 * <p>
 * Model B: UI -> BFF only, so /api/v1/public/** is denied anyway.
 */
@Component
@RequiredArgsConstructor
public class InternalRequestSecretFilter extends OncePerRequestFilter {

  private final DmSecurityProperties securityProperties;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String expected = securityProperties.getInternalSharedSecret();

    // If no secret configured -> don't enforce (dev/local)
    if (!StringUtils.hasText(expected)) {
      return true;
    }

    String path = request.getRequestURI();

    // Allow docs/actuator without internal secret (actuator is still secured by SecurityConfig)
    if (path.startsWith("/v3/api-docs")
        || path.startsWith("/swagger-ui")
        || path.startsWith("/webjars")
        || path.startsWith("/actuator")) {
      return true;
    }

    // Enforce secret for internal/admin (and legacy if ever enabled)
    return !(path.startsWith("/api/v1/internal/dm")
        || path.startsWith("/api/v1/admin/dm")
        || path.startsWith("/api/v1/documents"));
  }


  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String expected = securityProperties.getInternalSharedSecret();

    // shouldNotFilter already ensures expected is present, but keep defense-in-depth
    if (!StringUtils.hasText(expected)) {
      filterChain.doFilter(request, response);
      return;
    }

    String headerName = securityProperties.getInternalSharedSecretHeader();
    String provided = request.getHeader(headerName);

    if (!StringUtils.hasText(provided) || !expected.equals(provided)) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      response.setContentType("application/json");
      response.getWriter().write(
          "{\"message\":\"Forbidden\",\"details\":\"Missing or invalid internal secret header\"}"
      );
      return;
    }

    filterChain.doFilter(request, response);
  }
}

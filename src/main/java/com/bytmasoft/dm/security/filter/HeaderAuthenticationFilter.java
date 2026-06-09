package com.bytmasoft.dm.security.filter;


import com.bytmasoft.dm.context.RequestContext;
import com.bytmasoft.dm.context.RequestContextData;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Trust boundary: DM accepts identity ONLY from the Gateway (forwarded headers). Model B: DM is
 * internal-only; all /api/v1/internal/** and /api/v1/admin/** must be gateway-trusted.
 */
@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

  private static final String GATEWAY_AUTH_HEADER = "X-Gateway-Auth";
  private static final String GATEWAY_AUTH_TRUSTED = "trusted";

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String path = request.getRequestURI();

    boolean isInternalOrAdmin =
        path.startsWith("/api/v1/internal") || path.startsWith("/api/v1/admin")
            || path.startsWith("/api/v1/documents");

    boolean isDocsOrActuator =
        path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")
            || path.startsWith("/webjars") || path.startsWith("/actuator");

    // Skip docs/actuator
    if (isDocsOrActuator) {
      filterChain.doFilter(request, response);
      return;
    }

    String gatewayAuth = request.getHeader(GATEWAY_AUTH_HEADER);

    // For internal/admin endpoints, gateway trust is mandatory
    if (isInternalOrAdmin && !GATEWAY_AUTH_TRUSTED.equalsIgnoreCase(gatewayAuth)) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      response.setContentType("application/json");
      response.getWriter().write(
          "{\"message\":\"Forbidden\",\"details\":\"Untrusted gateway\"}"
      );
      return;
    }

    String userIdHeader = request.getHeader("X-User-Id");
    String username = request.getHeader("X-User-Name");
    String rolesHeader = request.getHeader("X-User-Roles");
    String permissionsHeader = request.getHeader("X-User-Permissions");
    String clientIp = request.getHeader("X-Forwarded-For");

    Long userId = null;
    if (StringUtils.hasText(userIdHeader)) {
      try {
        userId = Long.valueOf(userIdHeader);
      } catch (NumberFormatException ex) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        response.getWriter().write(
            "{\"message\":\"Bad Request\",\"details\":\"Invalid X-User-Id\"}"
        );
        return;
      }
    }

    // For internal/admin calls, user id should always exist (strong internal contract)
    if (isInternalOrAdmin && userId == null) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json");
      response.getWriter().write(
          "{\"message\":\"Unauthorized\",\"details\":\"Missing X-User-Id\"}"
      );
      return;
    }

    // Build authorities from roles (ROLE_ prefix only)
    List<GrantedAuthority> authorities = new ArrayList<>();
    if (StringUtils.hasText(rolesHeader)) {
      Arrays.stream(rolesHeader.split(","))
          .map(String::trim)
          .filter(s -> !s.isBlank())
          .forEach(role -> {
            String normalized = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            authorities.add(new SimpleGrantedAuthority(normalized));
          });
    }

    if (userId != null) {
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(username, null, authorities);
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // RequestContext (for services)
    RequestContext.set(
        new RequestContextData(
            userId,
            username,
            split(rolesHeader),
            split(permissionsHeader),
            clientIp,
            true
        )
    );

    filterChain.doFilter(request, response);
  }

  private Set<String> split(String header) {
    if (!StringUtils.hasText(header)) {
      return Set.of();
    }
    return Arrays.stream(header.split(","))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .collect(Collectors.toUnmodifiableSet());
  }
}

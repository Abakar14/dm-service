package com.bytmasoft.dm.security.aspect;

import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class AccessLogAspect {

  @Before("within(@org.springframework.web.bind.annotation.RestController *) && execution(* *(..))")
  public void logControllerAccess(JoinPoint jp) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null) {
      log.info("Access by {} (Roles: {}) to {}",
          auth.getName(),
          auth.getAuthorities().stream()
              .map(GrantedAuthority::getAuthority)
              .collect(Collectors.joining(",")),
          jp.getSignature().toShortString());
    }
  }
}

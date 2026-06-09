package com.bytmasoft.dm.security;

import com.bytmasoft.dm.security.filter.HeaderAuthenticationFilter;
import com.bytmasoft.dm.security.filter.InternalRequestSecretFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

  private final InternalRequestSecretFilter internalRequestSecretFilter;
  private final HeaderAuthenticationFilter headerAuthenticationFilter;
  private final ApiAuthenticationEntryPoint authenticationEntryPoint;
  private final ApiAccessDeniedHandler accessDeniedHandler;


  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(authenticationEntryPoint)
            .accessDeniedHandler(accessDeniedHandler)); // JWT for other endpoints

    http.authorizeHttpRequests(req -> {

      // Swagger / docs
      req.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/webjars/**")
          .permitAll();

      // actuator
      req.requestMatchers("/actuator/health/**", "/actuator/info").permitAll();
      req.requestMatchers("/actuator/**").hasRole("ADMIN");

      // public endpoints: not used in model B
      req.requestMatchers("/public/**").permitAll();

      // admin endpoints
      req.requestMatchers("/admin/**").hasRole("ADMIN");

      // internal
      req.requestMatchers("/internal/**").authenticated();

      // legacy (if still present): deny, or keep internal-only temporarily
      req.requestMatchers("/documents/**").denyAll();

      // default deny
      req.anyRequest().denyAll();
    });

    // Filter order: secret first, then header auth
    http.addFilterBefore(internalRequestSecretFilter, UsernamePasswordAuthenticationFilter.class);
    http.addFilterAfter(headerAuthenticationFilter, InternalRequestSecretFilter.class);

    return http.build();
  }


  @Bean
  public WebSecurityCustomizer webSecurityCustomizer() {
    return web -> web.ignoring().requestMatchers("/error");
  }

}

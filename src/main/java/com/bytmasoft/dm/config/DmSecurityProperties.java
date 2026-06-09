package com.bytmasoft.dm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Security-related settings for dm-service.
 * <p>
 * NOTE: This service is intended to be called via Gateway/BFF. These properties provide
 * defense-in-depth in case dm-service is accidentally exposed.
 */
@Data
@ConfigurationProperties(prefix = "dm.security")
public class DmSecurityProperties {

  /**
   * If set (non-blank), write operations (POST/PUT/PATCH/DELETE) require a matching header. This is
   * a lightweight guard until HMAC/mTLS is introduced.
   */
  private String internalSharedSecret = "";

  /**
   * Header name used to carry the internal shared secret.
   */
  private String internalSharedSecretHeader = "X-Internal-Secret";

  /**
   * Enable anonymous public download endpoints under /api/v1/public/**. Keep disabled by default;
   * enable only for truly public media (e.g., item images).
   */
  private boolean publicDownloadsEnabled = false;

  /**
   * Cache max-age (seconds) for public image responses.
   */
  private long publicImageCacheMaxAgeSeconds = 60L * 60L * 24L * 7L; // 7 days

  /**
   * Ownership guard for OwnerType.USER: only the same user (X-User-Id) or ADMIN may write.
   */
  private boolean enforceOwnerMatchForOwnerTypeUser = true;
}

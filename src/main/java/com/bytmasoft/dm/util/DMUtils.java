package com.bytmasoft.dm.util;

import com.bytmasoft.dm.entity.UploadEntity;
import com.bytmasoft.dm.enums.UploadType;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class DMUtils {

  public UploadType mapToThumbType(UploadType uploadType) {
    switch (uploadType) {
      case FOUND_ITEM_IMAGE:
        return UploadType.FOUND_ITEM_THUMBNAIL;
      case LOST_ITEM_IMAGE:
        return UploadType.LOST_ITEM_THUMBNAIL;
      case USER_PROFILE_IMAGE:
        return UploadType.USER_PROFILE_THUMBNAIL;
      case VEHICLE_PHOTO:
        return UploadType.VEHICLE_PHOTO_THUMBNAIL;
      default:
        return uploadType;
    }
  }

  public Dimension readImageSize(byte[] data) throws IOException {
    try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
      BufferedImage img = ImageIO.read(in);
      if (img == null) {
        throw new IOException("Not a readable image");
      }
      return new Dimension(img.getWidth(), img.getHeight());
    }
  }


  public int getVersion(List<UploadEntity> existing) {
    return existing.stream()
        .map(UploadEntity::getVersion)
        .filter(Objects::nonNull)
        .max(Integer::compareTo)
        .orElse(0) + 1;

  }

  public String getUsername() {
    if (SecurityContextHolder.getContext() != null) {
      if (SecurityContextHolder.getContext().getAuthentication() != null) {
        if (SecurityContextHolder.getContext().getAuthentication().getName() != null) {
          return SecurityContextHolder.getContext().getAuthentication().getName();
        }
      }
    }
    return "system";
  }

  public String shortId() {
    UUID uuid = UUID.randomUUID();
    BigInteger bigInteger = new BigInteger(1, uuid.toString().getBytes());
    return bigInteger.toString(36).substring(0, 8); // Base36 (0-9 + a-z)
  }

  public String normalizeContentType(String contentType, String filename) {
    if (contentType == null || contentType.isBlank() || "application/octet-stream".equalsIgnoreCase(
        contentType)) {
      return guessMimeTypeFromFilename(filename).orElse("application/octet-stream");
    }
    return contentType;
  }

  private Optional<String> guessMimeTypeFromFilename(String filename) {
    if (filename == null || filename.isBlank()) {
      return Optional.empty();
    }

    String name = filename.trim().toLowerCase(Locale.ROOT);

    // Remove any path prefix
    int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
    if (slash >= 0 && slash + 1 < name.length()) {
      name = name.substring(slash + 1);
    }

    int dot = name.lastIndexOf('.');
    if (dot < 0 || dot == name.length() - 1) {
      return Optional.empty();
    }

    String ext = name.substring(dot + 1);

    return switch (ext) {
      case "jpg", "jpeg" -> Optional.of("image/jpeg");
      case "png" -> Optional.of("image/png");
      case "webp" -> Optional.of("image/webp");
      case "gif" -> Optional.of("image/gif");
      case "bmp" -> Optional.of("image/bmp");
      case "heic" -> Optional.of("image/heic");
      case "pdf" -> Optional.of("application/pdf");
      case "txt" -> Optional.of("text/plain");
      case "csv" -> Optional.of("text/csv");
      case "json" -> Optional.of("application/json");
      default -> Optional.empty();
    };
  }


  private String ensureExtension(String originalName, String contentType) {
    // Fallback name if missing
    String name = (originalName == null || originalName.isBlank()) ? "file" : originalName.trim();

    // Strip fake path / directories
    int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
    if (slash >= 0 && slash + 1 < name.length()) {
      name = name.substring(slash + 1);
    }

    // Determine extension for content-type
    String wantedExt = extensionForContentType(contentType).orElse(null);
    if (wantedExt == null) {
      // No idea → keep original filename as-is
      return sanitizeFilename(name);
    }

    // If name already has an extension, keep it if it matches; otherwise replace
    int dot = name.lastIndexOf('.');
    if (dot > 0 && dot < name.length() - 1) {
      String currentExt = name.substring(dot + 1).toLowerCase(Locale.ROOT);
      if (currentExt.equals(wantedExt)) {
        return sanitizeFilename(name);
      }
      // replace extension
      name = name.substring(0, dot);
    }

    return sanitizeFilename(name + "." + wantedExt);
  }


  private Optional<String> extensionForContentType(String contentType) {
    if (contentType == null) {
      return Optional.empty();
    }
    String ct = contentType.toLowerCase(Locale.ROOT);

    return switch (ct) {
      case "image/jpeg" -> Optional.of("jpg");
      case "image/png" -> Optional.of("png");
      case "image/webp" -> Optional.of("webp");
      case "image/gif" -> Optional.of("gif");
      case "application/pdf" -> Optional.of("pdf");
      case "text/plain" -> Optional.of("txt");
      case "text/csv" -> Optional.of("csv");
      case "application/json" -> Optional.of("json");
      default -> Optional.empty();
    };
  }

  private String sanitizeFilename(String name) {
    // Keep it conservative: letters, digits, dot, dash, underscore
    String cleaned = name.replaceAll("[^a-zA-Z0-9._-]+", "_");
    // Avoid empty
    if (cleaned.isBlank()) {
      return "file";
    }
    // Avoid hidden files like ".jpg"
    if (cleaned.startsWith(".")) {
      cleaned = "file" + cleaned;
    }
    // Optional: limit length
    if (cleaned.length() > 120) {
      cleaned = cleaned.substring(0, 120);
    }
    return cleaned;
  }


  public String safeFilename(String originalName, String contentType) {
    return ensureExtension(originalName, contentType);
  }


}

package com.bytmasoft.dm.enums;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Simple magic-byte sniffer for common upload types.
 *
 * <p>Not a full file-type detector, but enough to block common spoofing.
 */
public enum FileSignature {
  PNG,
  JPEG,
  GIF,
  WEBP,
  PDF,
  UNKNOWN;

  public static FileSignature detect(byte[] header) {
    if (header == null || header.length == 0) {
      return UNKNOWN;
    }

    // PNG: 89 50 4E 47 0D 0A 1A 0A
    if (header.length >= 8
        && (header[0] & 0xFF) == 0x89
        && header[1] == 0x50
        && header[2] == 0x4E
        && header[3] == 0x47
        && header[4] == 0x0D
        && header[5] == 0x0A
        && header[6] == 0x1A
        && header[7] == 0x0A) {
      return PNG;
    }

    // JPEG: FF D8 FF
    if (header.length >= 3
        && (header[0] & 0xFF) == 0xFF
        && (header[1] & 0xFF) == 0xD8
        && (header[2] & 0xFF) == 0xFF) {
      return JPEG;
    }

    // GIF: "GIF87a" or "GIF89a"
    if (header.length >= 6) {
      String gif = new String(header, 0, 6, StandardCharsets.US_ASCII);
      if ("GIF87a".equals(gif) || "GIF89a".equals(gif)) {
        return GIF;
      }
    }

    // WEBP: RIFF .... WEBP
    if (header.length >= 12) {
      String riff = new String(header, 0, 4, StandardCharsets.US_ASCII);
      String webp = new String(header, 8, 4, StandardCharsets.US_ASCII);
      if ("RIFF".equals(riff) && "WEBP".equals(webp)) {
        return WEBP;
      }
    }

    // PDF: %PDF-
    if (header.length >= 5) {
      String pdf = new String(header, 0, 5, StandardCharsets.US_ASCII);
      if ("%PDF-".equals(pdf)) {
        return PDF;
      }
    }

    return UNKNOWN;
  }

  public boolean matchesExtension(String ext) {
    if (ext == null) {
      return false;
    }
    ext = ext.toLowerCase(Locale.ROOT);
    return switch (this) {
      case PNG -> ext.equals("png");
      case JPEG -> ext.equals("jpg") || ext.equals("jpeg");
      case GIF -> ext.equals("gif");
      case WEBP -> ext.equals("webp");
      case PDF -> ext.equals("pdf");
      case UNKNOWN -> true;
    };
  }

  public boolean matchesMime(String mime) {
    if (mime == null) {
      return false;
    }
    mime = mime.toLowerCase(Locale.ROOT);
    return switch (this) {
      case PNG -> mime.equals("image/png");
      case JPEG -> mime.equals("image/jpeg");
      case GIF -> mime.equals("image/gif");
      case WEBP -> mime.equals("image/webp");
      case PDF -> mime.equals("application/pdf");
      case UNKNOWN -> true;
    };
  }
}

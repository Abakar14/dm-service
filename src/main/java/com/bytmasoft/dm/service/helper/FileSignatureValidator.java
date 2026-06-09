package com.bytmasoft.dm.service.helper;

/**
 * FileSignatureValidator
 *
 * @author Mahamat Abakar
 * @since 20.02.26
 */

import java.util.Arrays;

public final class FileSignatureValidator {

  public enum DetectedType {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp"),
    GIF("image/gif", "gif");

    private final String mime;
    private final String ext;

    DetectedType(String mime, String ext) {
      this.mime = mime;
      this.ext = ext;
    }

    public String mime() {
      return mime;
    }

    public String ext() {
      return ext;
    }
  }

  private FileSignatureValidator() {
  }

  public static DetectedType detect(byte[] bytes) {
    if (bytes == null || bytes.length < 12) {
      return null;
    }

    if (isJpeg(bytes)) {
      return DetectedType.JPEG;
    }
    if (isPng(bytes)) {
      return DetectedType.PNG;
    }
    if (isWebp(bytes)) {
      return DetectedType.WEBP;
    }
    if (isGif(bytes)) {
      return DetectedType.GIF;
    }

    return null;
  }

  private static boolean isJpeg(byte[] b) {
    // FF D8 FF
    return b.length >= 3
        && (b[0] & 0xFF) == 0xFF
        && (b[1] & 0xFF) == 0xD8
        && (b[2] & 0xFF) == 0xFF;
  }

  private static boolean isPng(byte[] b) {
    // 89 50 4E 47 0D 0A 1A 0A
    byte[] sig = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    return b.length >= sig.length && Arrays.equals(Arrays.copyOf(b, sig.length), sig);
  }

  private static boolean isGif(byte[] b) {
    // "GIF87a" or "GIF89a"
    return b.length >= 6
        && b[0] == 'G' && b[1] == 'I' && b[2] == 'F'
        && b[3] == '8'
        && (b[4] == '7' || b[4] == '9')
        && b[5] == 'a';
  }

  private static boolean isWebp(byte[] b) {
    // RIFF....WEBP
    return b.length >= 12
        && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
        && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
  }
}

package com.bytmasoft.dm.service;

import com.bytmasoft.dm.dto.response.OptimizedImageResponse;
import com.bytmasoft.dm.exception.InvalidFileException;
import com.bytmasoft.dm.exception.enums.DmErrorCode;
import jakarta.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Optional;
import javax.imageio.ImageIO;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

/**
 * ThumbnailService
 *
 * @author Mahamat Abakar
 * @since 01.02.26
 */
@Service
public class ThumbnailService {

  private static final int MAX_W = 480;
  private static final int MAX_H = 480;

  public boolean isImage(@Nullable String mimeType) {
    return mimeType != null && mimeType.toLowerCase(Locale.ROOT).startsWith("image/");
  }

  public byte[] generateJpeg(InputStream original) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Thumbnails.of(original)
        .size(MAX_W, MAX_H)
        .outputFormat("jpg")
        .outputQuality(0.82)
        .toOutputStream(out);
    return out.toByteArray();
  }


  public OptimizedImageResponse optimizeOriginal(
      ByteArrayInputStream in,
      String contentType,
      String originalName
  ) {
    try {
      BufferedImage img = ImageIO.read(in);
      if (img == null) {
        // Not a readable image (corrupt or unsupported)
        throw new InvalidFileException(DmErrorCode.INVALID_FILE);
      }

      // --- Config (tune for Dalilak) ---
      int maxDim = 1600;         // max width/height
      float quality = 0.85f;     // jpeg quality

      // Decide output type
      // Default: JPEG
      String outType = "image/jpeg";
      String outExt = "jpg";

      // Optional: preserve PNG if transparency exists
      // (uncomment if you want true transparency preserved)
    /*
    boolean hasAlpha = img.getColorModel().hasAlpha();
    if (hasAlpha) {
      outType = "image/png";
      outExt = "png";
    }
    */

      // Resize while preserving aspect ratio
      BufferedImage resized = Thumbnails.of(img)
          .size(maxDim, maxDim)
          .outputQuality(quality) // only applies to JPEG, harmless for PNG
          .asBufferedImage();

      // Write to bytes
      ByteArrayOutputStream baos = new ByteArrayOutputStream(256 * 1024);
      boolean written = ImageIO.write(resized, outExt, baos);
      if (!written) {
        // fallback to jpg if writer missing
        baos.reset();
        outType = "image/jpeg";
        outExt = "jpg";
        ImageIO.write(resized, outExt, baos);
      }

      byte[] outBytes = baos.toByteArray();

      // Name: keep base, replace extension to match output
      String base = baseName(originalName).orElse("image");
      String outName = base + "." + outExt;

      return new OptimizedImageResponse(outBytes, outType, outName);

    } catch (InvalidFileException e) {
      throw e;
    } catch (Exception e) {
      // If anything goes wrong, fail explicitly so client sees a clear message
      throw new InvalidFileException(DmErrorCode.INVALID_FILE);
    }
  }

  /**
   * Extract base name without extension; safe
   */
  private Optional<String> baseName(String originalName) {
    if (originalName == null || originalName.isBlank()) {
      return Optional.empty();
    }
    String name = originalName.trim();

    // Remove any path segments (some browsers send "C:\fakepath\...")
    int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
    if (slash >= 0 && slash + 1 < name.length()) {
      name = name.substring(slash + 1);
    }

    // Remove extension
    int dot = name.lastIndexOf('.');
    if (dot > 0) {
      name = name.substring(0, dot);
    }

    // sanitize minimal (optional)
    name = name.replaceAll("[^a-zA-Z0-9-_]+", "_");

    if (name.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(name);
  }


}

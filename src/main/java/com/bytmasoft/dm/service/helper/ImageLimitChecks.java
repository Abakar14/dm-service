package com.bytmasoft.dm.service.helper;

/**
 * ImageLimitChecks
 *
 * @author Mahamat Abakar
 * @since 21.02.26
 */

import java.awt.image.BufferedImage;

public final class ImageLimitChecks {

  private ImageLimitChecks() {
  }

  public static void enforce(BufferedImage img) {
    int w = img.getWidth();
    int h = img.getHeight();
    long pixels = (long) w * (long) h;

    if (w <= 0 || h <= 0) {
      throw new IllegalArgumentException("Invalid image dimensions");
    }
    if (w > ImageLimits.MAX_WIDTH || h > ImageLimits.MAX_HEIGHT) {
      throw new IllegalArgumentException("Image dimensions exceed limit");
    }
    if (pixels > ImageLimits.MAX_PIXELS) {
      throw new IllegalArgumentException("Image pixel count exceeds limit");
    }
  }

  public static void enforceAspectRatio(int w, int h) {
    double ratio = (double) Math.max(w, h) / (double) Math.min(w, h);
    if (ratio > 40.0) { // extremely long strip images
      throw new IllegalArgumentException("Suspicious aspect ratio");
    }
  }

}


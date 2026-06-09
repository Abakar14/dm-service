package com.bytmasoft.dm.service.helper;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * ImageValidators
 *
 * @author Mahamat Abakar
 * @since 21.02.26 Signature check proves “looks like JPEG/PNG…”, but we also want to ensure the
 * file is actually decodable.
 */
public final class ImageValidators {

  private ImageValidators() {
  }

  /**
   * Decodes the image; throws if unreadable/corrupt.
   */
  public static BufferedImage decodeOrThrow(byte[] bytes) throws IOException {
    try (var in = new ByteArrayInputStream(bytes)) {
      BufferedImage img = ImageIO.read(in);
      if (img == null) {
        throw new IOException("ImageIO could not decode image");
      }
      return img;
    }
  }
}

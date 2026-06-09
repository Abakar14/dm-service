package com.bytmasoft.dm.service.storage;

import java.io.IOException;
import java.nio.file.Path;

/**
 * ThumbnailService
 *
 * @author Mahamat Abakar
 * @since 01.02.26
 */
public interface ThumbnailService {

  ThumbnailResult generate(Path originalFile, String originalMimeType) throws IOException;

  record ThumbnailResult(byte[] bytes, String contentType, int width, int height) {

  }
}

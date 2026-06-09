package com.bytmasoft.dm.service.helper;

import com.bytmasoft.dm.config.StorageProperties;
import com.bytmasoft.dm.enums.UploadType;
import com.bytmasoft.dm.exception.InvalidFileException;
import com.bytmasoft.dm.exception.enums.DmErrorCode;
import com.bytmasoft.dm.util.DMUtils;
import java.awt.image.BufferedImage;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

/**
 * Central upload validation.
 *
 * <p>Production goals:
 * <ul>
 *   <li>Enforce max upload size (defense-in-depth beyond servlet multipart limits)</li>
 *   <li>Validate extension against allowed list</li>
 *   <li>Validate content-type (if configured)</li>
 *   <li>Sniff magic bytes for common types (png/jpg/gif/webp/pdf)
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileValidationService {


  private final DMUtils dmUtils;
  private final StorageProperties storageProperties;

  public ValidatedUpload validateAndRead(MultipartFile file, UploadType uploadType, Long ownerId)
      throws IOException {
    String originalName = file != null ? file.getOriginalFilename() : null;
    DataSize max =
        storageProperties.getUpload() != null ? storageProperties.getUpload().getMaxFileSize()
            : null;

    if (file == null) {
      auditReject("INVALID_FILE", "File is null", uploadType, ownerId, originalName,
          file.getSize());
      throw new InvalidFileException(DmErrorCode.INVALID_FILE);
    }
    if (file.isEmpty()) {
      auditReject("EMPTY_FILE", "File is empty", uploadType, ownerId, originalName, file.getSize());
      throw new InvalidFileException(DmErrorCode.EMPTY_FILE);
    }

    if (max != null && file.getSize() > max.toBytes()) {
      auditReject("FILE_TOO_LARGE", "File size exceeds maximum allowed size: " + max, uploadType,
          ownerId, originalName, file.getSize());
      throw new InvalidFileException(DmErrorCode.PAYLOAD_TOO_LARGE,
          "File size exceeds maximum allowed size: " + max, HttpStatus.BAD_REQUEST);
    }

    byte[] bytes = file.getBytes();

    var detected = FileSignatureValidator.detect(bytes);
    if (detected == null) {
      auditReject("UNSUPPORTED_MEDIA_TYPE", "Unsupported file type", uploadType, ownerId,
          originalName, file.getSize());

      throw new InvalidFileException(
          DmErrorCode.INVALID_FILE_TYPE,
          "Unsupported file type",
          HttpStatus.UNSUPPORTED_MEDIA_TYPE
      );
    }

    BufferedImage img;
    try {
      img = ImageValidators.decodeOrThrow(bytes);
    } catch (Exception e) {
      auditReject("INVALID_IMAGE", "Unreadable/corrupt image", uploadType, ownerId, originalName,
          file.getSize());
      throw new InvalidFileException(DmErrorCode.INVALID_FILE
      );
    }

    try {
      ImageLimitChecks.enforce(img);
      // Optional:
      // ImageLimitChecks.enforceAspectRatio(img.getWidth(), img.getHeight());
    } catch (Exception e) {
      auditReject("IMAGE_TOO_LARGE", e.getMessage(), uploadType, ownerId, originalName,
          file.getSize());
      throw new InvalidFileException(DmErrorCode.PAYLOAD_TOO_LARGE);
    }

    String safeName = sanitizeBaseName(file.getOriginalFilename()) + "." + detected.ext();

    return new ValidatedUpload(bytes, detected.mime(), safeName, file.getOriginalFilename());
  }


  private void auditReject(String code, String reason, UploadType uploadType, Long ownerId,
      String name, long size) {
    String username = "unknown";
    try {
      username = dmUtils.getUsername();
    } catch (Exception ignored) {
    }
    log.warn(
        "Upload rejected: code={}, reason={}, user={}, ownerId={}, uploadType={}, file={}, size={}",
        code, reason, username, ownerId, uploadType, name, size);
  }


  private String sanitizeBaseName(String name) {
    if (name == null || name.isBlank()) {
      return "image";
    }
    String n = name.trim();
    int slash = Math.max(n.lastIndexOf('/'), n.lastIndexOf('\\'));
    if (slash >= 0 && slash + 1 < n.length()) {
      n = n.substring(slash + 1);
    }
    int dot = n.lastIndexOf('.');
    if (dot > 0) {
      n = n.substring(0, dot);
    }
    n = n.replaceAll("[^a-zA-Z0-9-_]+", "_");
    if (n.isBlank()) {
      return "image";
    }
    if (n.length() > 80) {
      n = n.substring(0, 80);
    }
    return n;
  }

  public record ValidatedUpload(
      byte[] bytes,
      String detectedMimeType,
      String safeFileName,
      String originalFileName
  ) {

  }


}

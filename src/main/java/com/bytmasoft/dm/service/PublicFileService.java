package com.bytmasoft.dm.service;

import com.bytmasoft.dm.config.StorageProperties;
import com.bytmasoft.dm.dto.response.StoredFileResourceResponse;
import com.bytmasoft.dm.entity.UploadEntity;
import com.bytmasoft.dm.enums.DomainType;
import com.bytmasoft.dm.enums.RenditionType;
import com.bytmasoft.dm.enums.UploadType;
import com.bytmasoft.dm.exception.StorageException;
import com.bytmasoft.dm.exception.StorageFileNotFoundException;
import com.bytmasoft.dm.repository.UploadEntityRepository;
import com.bytmasoft.dm.service.storage.StorageService;
import java.io.IOException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class PublicFileService {

  private final UploadEntityRepository repository;
  private final StorageService storageService;
  private final StorageProperties storageProperties;

  /**
   * Load the original file (binary) by its upload id.
   */
  public StoredFileResourceResponse load(String fileId) throws IOException {
    UploadEntity entity = repository.findById(fileId).orElseThrow(
        () -> new StorageFileNotFoundException("File with id: " + fileId + " not found"));

    assertValidFileMeta(entity, fileId);
    Resource resource = storageService.load(entity.getFilePath(), entity.getFileName());

    String contentType = resolveContentType(entity, "application/octet-stream");
    String etag = buildEtag(entity, "orig");

    return new StoredFileResourceResponse(contentType, etag, resource);

  }

  /**
   * Load a thumbnail for an upload id.
   * <p>
   * Preferred mode: load persisted thumbnail entity (parent_upload_id + rendition_type =
   * THUMBNAIL). Optional migration fallback: if DB thumbnail doesn't exist, fallback to
   * storageService.loadThumbnail(fileId).
   */
  public StoredFileResourceResponse loadThumbnail(String fileId) throws IOException {
    UploadEntity original = repository.findById(fileId)
        .orElseThrow(() -> new StorageFileNotFoundException("File not found: " + fileId));
    UploadEntity thumb = null;
    if (original.getRenditionType().equals(RenditionType.THUMBNAIL)) {
      thumb = original;
    } else {
      thumb = repository
          .findFirstByParentUploadIdAndRenditionType(fileId, RenditionType.THUMBNAIL)
          .orElse(null);
    }

    assertValidFileMeta(original, fileId);
    // 1) Preferred: DB-linked rendition

    if (thumb != null) {
      assertValidFileMeta(thumb, fileId + " (thumb)");
      Resource res = storageService.load(thumb.getFilePath(), thumb.getFileName());

      String ct = resolveContentType(thumb, "image/jpeg");
      // thumb etag should vary when thumb changes; include thumb id + version if you store it
      String etag = buildEtagForThumb(original, thumb);

      return new StoredFileResourceResponse(ct, etag, res);
    }

    // 2) Optional fallback: lazy on-demand thumbnail generation in StorageService
    // If you do NOT want this fallback, replace with throwing StorageFileNotFoundException.
    try {
      Resource res = storageService.loadThumbnail(fileId);
      String etag = buildEtag(original, "thumb-fallback");
      return new StoredFileResourceResponse("image/jpeg", etag, res);
    } catch (StorageFileNotFoundException e) {
      throw e;
    } catch (Exception e) {
      // keep error transparent and consistent
      throw new StorageException("Failed to load thumbnail for id " + fileId, e);
    }

  }

  public Resource downloadDocument(DomainType domainType, UploadType uploadType, Long ownerId,
      Integer version) throws IOException {
    UploadEntity uploadEntity = findDocumentBy(domainType, uploadType, ownerId, version);
    return storageService.load(uploadEntity.getFilePath(), uploadEntity.getFileName());
  }

  private UploadEntity findDocumentBy(DomainType domainType, UploadType uploadType, Long ownerId,
      Integer version) throws StorageException {
    UploadEntity uploadEntity = repository.findByDomainTypeAndUploadTypeAndItemIdAndVersion(
            domainType,
            uploadType, ownerId, version)
        .orElseThrow(() -> new StorageException(
            "Upload with ownerId: " + ownerId + " and version nr: " + version + " + not found"));
    return uploadEntity;
  }


  public String buildOriginalUrl(Long uploadId) {
    return storageProperties.getDownload().getDownloadUrl() + "/" + uploadId;
  }

  public String buildThumbUrl(Long uploadId) {
    return storageProperties.getDownload().getThumbUrl() + "/" + uploadId + "/thumb";
  }

  private void assertValidFileMeta(UploadEntity e, String debugId) {
    if (e.getFilePath() == null || e.getFilePath().isBlank()) {
      throw new StorageException("Corrupt upload metadata (filePath missing) for " + debugId);
    }
    if (e.getFileName() == null || e.getFileName().isBlank()) {
      throw new StorageException("Corrupt upload metadata (fileName missing) for " + debugId);
    }
  }

  private String resolveContentType(UploadEntity e, String fallback) {
    String mt = e.getMimeType();
    if (mt == null || mt.isBlank()) {
      mt = e.getContentType();
    }
    if (mt == null || mt.isBlank()) {
      return fallback;
    }
    return mt.toLowerCase(Locale.ROOT);
  }

  /**
   * Strong ETag: changes when the underlying content likely changes. Uses: id + version + size +
   * suffix.
   */
  private String buildEtag(UploadEntity e, String suffix) {
    String id = e.getId() == null ? "noid" : e.getId();
    String v = e.getVersion() == null ? "nov" : e.getVersion().toString();
    String s = e.getFileSize() == null ? "nos" : e.getFileSize().toString();
    return "\"" + id + "-" + v + "-" + s + "-" + suffix + "\"";
  }

  /**
   * ETag for thumbnail: use thumb id + thumb size/version if available, but also include original
   * version to ensure invalidation when original is replaced.
   */
  private String buildEtagForThumb(UploadEntity original, UploadEntity thumb) {
    String origV = original.getVersion() == null ? "nov" : original.getVersion().toString();
    String thumbId = thumb.getId() == null ? "noid" : thumb.getId();
    String thumbV = thumb.getVersion() == null ? "nov" : thumb.getVersion().toString();
    String thumbS = thumb.getFileSize() == null ? "nos" : thumb.getFileSize().toString();
    return "\"" + thumbId + "-" + thumbV + "-" + thumbS + "-origv-" + origV + "-thumb\"";
  }


}

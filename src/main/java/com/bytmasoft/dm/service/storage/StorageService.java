package com.bytmasoft.dm.service.storage;

import com.bytmasoft.dm.dto.request.StorageCommandRequest;
import com.bytmasoft.dm.dto.response.StoredObjectResponse;
import com.bytmasoft.dm.entity.UploadEntity;
import com.bytmasoft.dm.enums.DomainType;
import com.bytmasoft.dm.enums.UploadType;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * Storage backend abstraction.
 *
 * <p>Controllers/business services should NOT deal with filesystem paths, nor with MultipartFile.
 * They should pass an intent (StorageCommand) and raw bytes (InputStream).
 *
 * <p>This allows swapping Local FS with S3/MinIO later without changing controller code.
 */
public interface StorageService {

  StoredObjectResponse store(InputStream data, StorageCommandRequest command) throws IOException;

  Resource load(String directory, String filename) throws IOException;

  // NEW: load/store by key, not “thumbnail by fileId”
  Resource loadByKey(String storageKey) throws IOException;

  Resource loadThumbnail(String fileId) throws IOException;

  boolean delete(String directory, String filename) throws IOException;

  void moveFileToTrash(String directory, String filename, String trashKey) throws IOException;

  boolean exists(String directory, String filename);

  int purgeTrash(DomainType domainType, int olderThanDays) throws IOException;

  ByteArrayResource generateZip(DomainType domainType, UploadType uploadType, Long ownerId,
      Integer version, List<UploadEntity> uploadEntities) throws IOException;

  boolean deleteThumbnail(String directory, String filename) throws IOException;

  void generateThumbnail(String filePath, String fileName, String contentType) throws IOException;
}

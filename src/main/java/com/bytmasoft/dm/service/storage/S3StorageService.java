package com.bytmasoft.dm.service.storage;

import com.bytmasoft.dm.dto.request.StorageCommandRequest;
import com.bytmasoft.dm.dto.response.StoredObjectResponse;
import com.bytmasoft.dm.entity.UploadEntity;
import com.bytmasoft.dm.enums.DomainType;
import com.bytmasoft.dm.enums.UploadType;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;


@Service
@Profile("aws")
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

  @Override
  public StoredObjectResponse store(InputStream data, StorageCommandRequest command)
      throws IOException {
    throw new UnsupportedOperationException("S3 storage not implemented yet");
    //return null;
  }

  @Override
  public Resource load(String directory, String filename) throws IOException {
    throw new UnsupportedOperationException("S3 storage not implemented yet");
    //return null;
  }

  @Override
  public Resource loadByKey(String storageKey) throws IOException {
    return null;
  }

  @Override
  public Resource loadThumbnail(String fileId) throws IOException {
    return null;
  }

  @Override
  public boolean delete(String directory, String filename) throws IOException {
    throw new UnsupportedOperationException("S3 storage not implemented yet");
  }

  @Override
  public void moveFileToTrash(String directory, String filename, String trashKey)
      throws IOException {

  }

  @Override
  public boolean exists(String directory, String filename) {
    return false;
  }

  @Override
  public int purgeTrash(DomainType domainType, int olderThanDays) throws IOException {
    return 0;
  }

  @Override
  public ByteArrayResource generateZip(DomainType domainType, UploadType uploadType, Long ownerId,
      Integer version, List<UploadEntity> uploadEntities) throws IOException {
    throw new UnsupportedOperationException("S3 storage not implemented yet");
  }

  @Override
  public boolean deleteThumbnail(String directory, String filename) throws IOException {
    return false;
  }

  @Override
  public void generateThumbnail(String filePath, String fileName, String contentType)
      throws IOException {

  }


}
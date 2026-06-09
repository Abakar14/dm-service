package com.bytmasoft.dm.service.storage;

import com.bytmasoft.dm.entity.UploadEntity;
import com.bytmasoft.dm.exception.StorageException;
import com.bytmasoft.dm.exception.StorageFileNotFoundException;
import com.bytmasoft.dm.repository.UploadEntityRepository;
import org.springframework.stereotype.Service;

/**
 * FileMetaResolver
 *
 * @author Mahamat Abakar
 * @since 01.02.26
 */
@Service
public class FileMetaResolverImpl implements FileMetaResolver {

  private final UploadEntityRepository repository;

  public FileMetaResolverImpl(UploadEntityRepository repository) {
    this.repository = repository;
  }

  @Override
  public FileMeta resolve(String fileId) {

    UploadEntity e = repository.findById(fileId).orElseThrow(
        () -> new StorageFileNotFoundException("File with id " + fileId + " not found"));

    String dir = e.getFilePath();
    String name = e.getFileName();
    if (dir == null || dir.isBlank() || name == null || name.isBlank()) {
      throw new StorageException("Corrupt upload metadata for id " + fileId);
    }
    String mt = (e.getMimeType() != null && !e.getMimeType().isBlank())
        ? e.getMimeType()
        : e.getContentType();

    return new FileMeta(dir, name, mt);

  }
}

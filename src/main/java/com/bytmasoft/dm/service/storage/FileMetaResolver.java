package com.bytmasoft.dm.service.storage;

/**
 * FileMetaResolver
 *
 * @author Mahamat Abakar
 * @since 01.02.26
 */
public interface FileMetaResolver {

  FileMeta resolve(String fileId);

  record FileMeta(String directory, String filename, String mimeType) {

  }
}

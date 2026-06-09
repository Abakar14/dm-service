package com.bytmasoft.dm.service.storage;

import com.bytmasoft.dm.config.StorageProperties;
import com.bytmasoft.dm.dto.request.StorageCommandRequest;
import com.bytmasoft.dm.dto.response.StoredObjectResponse;
import com.bytmasoft.dm.entity.UploadEntity;
import com.bytmasoft.dm.enums.DomainType;
import com.bytmasoft.dm.enums.UploadType;
import com.bytmasoft.dm.exception.StorageException;
import com.bytmasoft.dm.exception.StorageFileNotFoundException;
import com.bytmasoft.dm.service.storage.FileMetaResolver.FileMeta;
import com.bytmasoft.dm.util.DMUtils;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

/**
 * Default Local Filesystem implementation for production (single instance / mounted volume).
 *
 * <p>Stores files under:
 * {root}/{domain}/{ownerType}/{ownerId}/{uploadType}/
 */
@Service
@Profile("!aws") // default profile (aws profile can override with S3 implementation later)
@RequiredArgsConstructor
public class LocalFsStorageService implements StorageService {

  private static final Logger log = LoggerFactory.getLogger(LocalFsStorageService.class);

  private final FileMetaResolver metaResolver;
  private final StorageProperties storageProperties;
  private final DMUtils dmUtils;
  // thumb params (tune later)
  private static final int THUMB_MAX_W = 480;
  private static final int THUMB_MAX_H = 480;
  private static final String THUMB_DIR_NAME = ".thumbs";

  private Path root;

  @PostConstruct
  public void init() {
    String location = storageProperties.getUpload().getLocation();

    if (location == null || location.isBlank()) {
      throw new StorageException("file.storage.upload.location is not configured");
    }
    root = Paths.get(location).toAbsolutePath().normalize();
    try {
      Files.createDirectories(root);
    } catch (IOException e) {
      throw new StorageException("Failed to create storage root: " + root, e);
    }
    if (!Files.isWritable(root)) {
      throw new StorageException("Upload directory is not writable: " + root);
    }
    log.info("Local FS storage root: {}", root);
  }

  @Override
  public StoredObjectResponse store(InputStream data, StorageCommandRequest cmd)
      throws IOException {
    if (data == null) {
      throw new StorageException("InputStream is null");
    }
    Path uploadRoot = root;

    Path directory = Paths.get(
        cmd.domainType().name().toLowerCase(),
        cmd.uploadType().name().toLowerCase(),
        String.valueOf(cmd.ownerId())

    ).normalize();

    // ensure directory is relative (defense-in-depth)
    if (directory.isAbsolute() || directory.startsWith("..")) {
      throw new StorageException("Invalid storage directory");
    }

    Path destinationDir = uploadRoot.resolve(directory).normalize();
    if (!destinationDir.startsWith(uploadRoot)) {
      throw new StorageException("Cannot store file outside upload directory");
    }
    Files.createDirectories(destinationDir);

    String filename = generateFilename(cmd);
    Path target = destinationDir.resolve(filename).normalize();

    if (!target.getParent().equals(destinationDir)) {
      throw new StorageException("Cannot store file outside target directory");
    }

    long size;
    try {
      Files.copy(data, target, StandardCopyOption.REPLACE_EXISTING);
      size = Files.size(target);

    } catch (IOException e) {
      throw new StorageException("Failed to store file: " + filename, e);
    }

    return new StoredObjectResponse(directory.toString(), filename, cmd.originalFilename(),
        cmd.contentType(), size);
  }

  @Override
  public Resource load(String directory, String filename) throws IOException {
    Path dir = Paths.get(directory).normalize();
    if (dir.isAbsolute() || dir.startsWith("..")) {
      throw new StorageException("Invalid directory");
    }

    Path base = root.resolve(dir).normalize();
    if (!base.startsWith(root)) {
      throw new StorageException("Invalid directory");
    }

    Path file = base.resolve(filename).normalize();
    if (!file.getParent().equals(base)) {
      throw new StorageException("Invalid filename");
    }
    if (!Files.exists(file)) {
      throw new StorageFileNotFoundException("File not found: " + directory + "/" + filename);
    }
    return new UrlResource(file.toUri());
  }


  @Override
  public Resource loadByKey(String storageKey) throws IOException {
    return null;
  }


  @Override
  public Resource loadThumbnail(String fileId) throws IOException {
    if (fileId == null || fileId.isBlank()) {
      throw new StorageException("FileId is blank");
    }
    // 1) resolve metadata
    FileMeta meta = metaResolver.resolve(fileId);

    //2) basic image check (optional but recommended)
    String ct = meta.mimeType() == null ? "" : meta.mimeType().toLowerCase(Locale.ROOT);
    if (!ct.startsWith("image/")) {
      // Either:
      // throw new StorageFileNotFoundException("Thumbnail not available for non-image: " + fileId);
      // OR return the original file:
      return load(meta.directory(), meta.filename());
    }

    // 3) locate original file path safely (reuse your existing validation logic)
    Path dir = Paths.get(meta.directory()).normalize();
    if (dir.isAbsolute() || dir.startsWith("..")) {
      throw new StorageException("Invalid directory");
    }

    Path base = root.resolve(dir).normalize();
    if (!base.startsWith(root)) {
      throw new StorageException("Invalid directory");
    }

    Path original = base.resolve(meta.filename()).normalize();
    if (!original.getParent().equals(base)) {
      throw new StorageException("Invalid filename");
    }
    if (!Files.exists(original)) {
      throw new StorageFileNotFoundException(
          "File not found: " + meta.directory() + "/" + meta.filename());
    }

    // 4) thumbnail path (cache)
    Path thumbDir = base.resolve(THUMB_DIR_NAME).normalize();
    if (!thumbDir.startsWith(root)) {
      throw new StorageException("Invalid thumb directory");
    }
    Files.createDirectories(thumbDir);

    // produce stable thumb filename derived from original filename
    String thumbName = thumbName(meta.filename());
    Path thumb = thumbDir.resolve(thumbName).normalize();
    if (!thumb.getParent().equals(thumbDir)) {
      throw new StorageException("Invalid thumb filename");
    }

    // 5) return cached thumbnail if it exists & is newer than original
    if (Files.exists(thumb)) {
      try {
        if (Files.getLastModifiedTime(thumb).compareTo(Files.getLastModifiedTime(original)) >= 0) {
          return new UrlResource(thumb.toUri());
        }
      } catch (IOException ignore) {
        // if we fail to read mtime, we'll regenerate
      }
    }
    // 6) generate thumbnail atomically
    Path tmp = thumbDir.resolve(thumbName + ".tmp").normalize();

    try {
      // Create thumbnail (JPEG). For PNG input you can still output JPG; or keep png.
      Thumbnails.of(original.toFile())
          .size(THUMB_MAX_W, THUMB_MAX_H)
          .outputFormat("jpg")
          .outputQuality(0.82)
          .toFile(tmp.toFile());

      try {
        Files.move(tmp, thumb, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(tmp, thumb, StandardCopyOption.REPLACE_EXISTING);
      }

      return new UrlResource(thumb.toUri());
    } finally {
      // Cleanup temp if something failed
      try {
        Files.deleteIfExists(tmp);
      } catch (IOException ignore) {
      }
    }

  }


  @Override
  public boolean delete(String directory, String filename) throws IOException {
    Path dir = Paths.get(directory).normalize();
    if (dir.isAbsolute() || dir.startsWith("..")) {
      throw new StorageException("Invalid directory");
    }
    Path base = root.resolve(dir).normalize();
    if (!base.startsWith(root)) {
      throw new StorageException("Invalid directory");
    }
    Path file = base.resolve(filename).normalize();
    if (!file.getParent().equals(base)) {
      throw new StorageException("Invalid filename");
    }
    return Files.deleteIfExists(file);
  }

  @Override
  public void moveFileToTrash(String directory, String filename, String trashKey)
      throws IOException {
    //TOTO i need to validate directory and filename
    Path sourceDir = root.resolve(directory).normalize();
    Path sourceFile = sourceDir.resolve(filename).normalize();

    Path trashDir = root.resolve(".trash").resolve(trashKey).normalize();
    Files.createDirectories(trashDir);

    Path target = trashDir.resolve(filename);

    Files.move(sourceFile, target, StandardCopyOption.REPLACE_EXISTING);
  }

  private int purgeTrash(Duration olderThan) throws IOException {
    Path trashRoot = root.resolve(".trash").normalize();

    if (!Files.exists(trashRoot)) {
      return 0;
    }

    if (!trashRoot.startsWith(root)) {
      log.error("Refusing to delete non-trash path: {}", root);
      return 0;
    }

    try (Stream<Path> stream = Files.list(trashRoot)) {
      return stream
          .filter(p -> isOlderThan(p, olderThan))
          .mapToInt(this::deleteRecursively)
          .sum();
    }
  }


  @Override
  public boolean exists(String directory, String filename) {
    try {
      Path dir = Paths.get(directory).normalize();
      if (dir.isAbsolute() || dir.startsWith("..")) {
        return false;
      }
      Path base = root.resolve(dir).normalize();
      if (!base.startsWith(root)) {
        return false;
      }
      Path file = base.resolve(filename).normalize();
      if (!file.getParent().equals(base)) {
        return false;
      }
      return Files.exists(file);
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public int purgeTrash(DomainType domainType, int olderThanDays) throws IOException {
    return purgeTrash(Duration.ofDays(olderThanDays));
  }


  @Override
  public ByteArrayResource generateZip(DomainType domainType, UploadType uploadType, Long ownerId,
      Integer version, List<UploadEntity> uploadEntities) throws IOException {

    //Create zip file
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream zipOut = new ZipOutputStream(baos);
    for (UploadEntity entity : uploadEntities) {
      try {

        Resource resource = load(entity.getFilePath(), entity.getFileName());
        //Create zip entry with original filename
        ZipEntry zipEntry = new ZipEntry(
            getSafeFileName(entity.getOriginalFileName(), entity.getFileName()));
        zipOut.putNextEntry(zipEntry);
        try (InputStream inputStream = resource.getInputStream()) {
          byte[] buffer = new byte[8192];
          int bytesRead;
          while ((bytesRead = inputStream.read(buffer)) != -1) {
            zipOut.write(buffer, 0, bytesRead);
          }
        }
        zipOut.closeEntry();
      } catch (IOException e) {
        log.warn("Failed to add file {} to zip: {}", entity.getFileName(), e.getMessage());
        throw new RuntimeException(e);
      }


    }
    zipOut.finish();
    zipOut.close();

    ByteArrayResource zipResource = new ByteArrayResource(baos.toByteArray()) {
      @Override
      public @Nullable String getFilename() {
        return generateZipFileName(domainType, uploadType, ownerId);
      }
    };

    return zipResource;

  }

  @Override
  public boolean deleteThumbnail(String directory, String filename) throws IOException {
    Path dir = Paths.get(directory).normalize();
    if (dir.isAbsolute() || dir.startsWith("..")) {
      throw new StorageException("Invalid directory");
    }

    Path base = root.resolve(dir).normalize();
    if (!base.startsWith(root)) {
      throw new StorageException("Invalid directory");
    }

    Path thumbDir = base.resolve(THUMB_DIR_NAME).normalize();
    if (!thumbDir.startsWith(root)) {
      throw new StorageException("Invalid thumb directory");
    }

    // must match your thumb naming
    String thumbName = thumbName(filename); // same helper used in loadThumbnail()
    Path thumb = thumbDir.resolve(thumbName).normalize();
    if (!thumb.getParent().equals(thumbDir)) {
      throw new StorageException("Invalid thumb filename");
    }

    return Files.deleteIfExists(thumb);
  }


  @Override
  public void generateThumbnail(String filePath, String fileName, String contentType)
      throws IOException {
    // 1) validate inputs
    if (filePath == null || filePath.isBlank()) {
      throw new StorageException("filePath is blank");
    }
    if (fileName == null || fileName.isBlank()) {
      throw new StorageException("fileName is blank");
    }

    String ct = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);

    // 2) only images
    if (!ct.startsWith("image/")) {
      // choose policy:
      // - do nothing (recommended)
      return;
    }

    // 3) normalize + safe resolve directory
    Path dir = Paths.get(filePath).normalize();
    if (dir.isAbsolute() || dir.startsWith("..")) {
      throw new StorageException("Invalid directory");
    }

    Path base = root.resolve(dir).normalize();
    if (!base.startsWith(root)) {
      throw new StorageException("Invalid directory");
    }

    // 4) locate original file safely
    Path original = base.resolve(fileName).normalize();
    if (!original.getParent().equals(base)) {
      throw new StorageException("Invalid filename");
    }
    if (!Files.exists(original)) {
      throw new StorageFileNotFoundException("File not found: " + filePath + "/" + fileName);
    }

    // 5) thumb directory
    Path thumbDir = base.resolve(THUMB_DIR_NAME).normalize();
    if (!thumbDir.startsWith(root)) {
      throw new StorageException("Invalid thumb directory");
    }
    Files.createDirectories(thumbDir);

    // 6) thumb file path
    String thumbName = thumbName(fileName); // deterministic
    Path thumb = thumbDir.resolve(thumbName).normalize();
    if (!thumb.getParent().equals(thumbDir)) {
      throw new StorageException("Invalid thumb filename");
    }

    // 7) generate to temp then atomic move (avoid partial files)
    //Path tmp = thumbDir.resolve(thumbName + ".tmp.jpg").normalize();
    //File tmp = File.createTempFile(thumbName, ".tmp.jpg", thumbDir.toFile());

    Path tmp = Files.createTempFile(thumbDir, thumbName, ".tmp.jpg");
    if (!tmp.getParent().equals(thumbDir)) {
      throw new StorageException("Invalid temp thumb filename");
    }
    try {

      Thumbnails.of(original.toFile())
          .size(THUMB_MAX_W, THUMB_MAX_H)
          .outputFormat("jpg")
          .outputQuality(0.82)
          .toFile(tmp.toFile());

      try {
        Files.move(tmp, thumb,
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(tmp, thumb,
            StandardCopyOption.REPLACE_EXISTING);
      }

    } finally {
      Files.deleteIfExists(tmp);
    }
  }

  /************************** Private Section ********************************/


  private boolean isOlderThan(Path path, Duration olderThan) {
    try {
      FileTime lastModifiedTime = Files.getLastModifiedTime(path);
      Instant cutoff = Instant.now().minus(olderThan);
      return lastModifiedTime.toInstant().isBefore(cutoff);
    } catch (IOException e) {
      log.warn("Failed to read lastModifiedTime for {} – skipping", path, e);
      return false;
    }
  }

  private int deleteRecursively(Path rootPath) {
    if (!Files.exists(rootPath)) {
      return 0;
    }

    try (Stream<Path> walk = Files.walk(rootPath)) {
      List<Path> paths = walk
          .sorted(Comparator.reverseOrder()) // delete children first
          .toList();

      for (Path p : paths) {
        Files.deleteIfExists(p);
      }

      log.info("Deleted trash entry: {}", rootPath);
      return 1;

    } catch (IOException e) {
      log.error("Failed to delete trash entry {}", rootPath, e);
      return 0;
    }
  }

  private String generateZipFileName(DomainType domainType, UploadType uploadType, Long ownerId) {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

    StringBuilder name = new StringBuilder("download_");

    if (domainType != null) {
      name.append(domainType.name().toLowerCase()).append("_");
    }

    if (uploadType != null) {
      name.append(uploadType.name().toLowerCase()).append("_");
    }

    if (ownerId != null) {
      name.append("owner_").append(ownerId).append("_");
    }

    name.append(timestamp).append(".zip");

    return name.toString();
  }

  private String generateFilename(StorageCommandRequest cmd) {
    String extension = safeExtension(cmd.originalFilename());
    String baseName = String.format(
        "%s_%d_%sv%d",
        cmd.uploadType().name().toLowerCase(),
        cmd.ownerId(),
        dmUtils.shortId(),
        cmd.version()
    );
    return baseName + "." + extension;
  }

  private String safeExtension(String originalFilename) {
    if (originalFilename == null) {
      return "bin";
    }
    int idx = originalFilename.lastIndexOf('.');
    if (idx < 0 || idx == originalFilename.length() - 1) {
      return "bin";
    }
    String ext = originalFilename.substring(idx + 1).toLowerCase();
    // basic sanitization: keep alnum only
    ext = ext.replaceAll("[^a-z0-9]", "");
    if (ext.isBlank()) {
      return "bin";
    }
    return ext;
  }

  private String thumbName(String originalFilename) {
    // Make a safe, deterministic name. You can also hash the filename if you want.
    String base = originalFilename;
    int dot = base.lastIndexOf('.');
    if (dot > 0) {
      base = base.substring(0, dot);
    }
    base = base.replaceAll("[^a-zA-Z0-9._-]", "_");
    return base + "_thumb.jpg";
  }

  private String getSafeFileName(String originalName, String fileName) {
    if (originalName != null && !originalName.isEmpty()) {
      // Sanitize filename
      return originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
    return fileName != null ? fileName : "file";
  }

  private boolean validateFilename(String filename) {

/*    Path file = base.resolve(filename).normalize();
    if (!file.getParent().equals(base)) {
      throw new StorageException("Invalid filename");
    }
    if (!Files.exists(file)) {
      throw new StorageFileNotFoundException("File not found: " + directory + "/" + filename);
    }*/

    return true;

  }

  private boolean validateDirectory(Path dir) {

    Path base = root.resolve(dir).normalize();

    if (dir.isAbsolute() || dir.startsWith("..")) {
      throw new StorageException("Invalid directory");
    }
    if (!base.startsWith(root)) {
      throw new StorageException("Invalid directory");
    }

    return true;

  }
}

package com.bytmasoft.dm.ops;

import com.bytmasoft.dm.config.StorageProperties;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that purges files from the DM trash folder after a retention period.
 *
 * <p>Enabled via {@code file.storage.cleanup.enabled=true}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrashCleanupJob {

  private final StorageProperties storageProperties;

  @Scheduled(cron = "#{@storageProperties.cleanup.cron}")
  public void purgeTrash() {
    if (storageProperties.getCleanup() == null || !storageProperties.getCleanup().isEnabled()) {
      return;
    }

    Path trash = Paths.get(storageProperties.getUpload().getLocation(), "trash")
        .toAbsolutePath().normalize();

    if (!Files.exists(trash)) {
      return;
    }

    int retentionDays = Math.max(1, storageProperties.getCleanup().getTrashRetentionDays());
    Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

    try {
      Files.walkFileTree(trash, new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          Instant lastModified = attrs.lastModifiedTime().toInstant();
          if (lastModified.isBefore(cutoff)) {
            try {
              Files.deleteIfExists(file);
              log.info("Purged trashed file: {}", file);
            } catch (IOException ex) {
              log.warn("Failed to purge trashed file: {}", file, ex);
            }
          }
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
          // Remove empty directories after purging
          try {
            if (!dir.equals(trash) && Files.isDirectory(dir) && Files.list(dir).findAny()
                .isEmpty()) {
              Files.deleteIfExists(dir);
            }
          } catch (Exception ignore) {
            // best-effort
          }
          return FileVisitResult.CONTINUE;
        }
      });

    } catch (IOException e) {
      log.warn("Trash cleanup failed", e);
    }
  }
}

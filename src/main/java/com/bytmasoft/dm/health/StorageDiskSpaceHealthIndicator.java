package com.bytmasoft.dm.health;

import com.bytmasoft.dm.config.StorageProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator that checks disk space on the configured DM upload storage root.
 *
 * <p>Spring's default DiskSpaceHealthIndicator checks the JVM temp directory.
 * For a media service, we must check the actual storage mount.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StorageDiskSpaceHealthIndicator implements HealthIndicator {


  private final StorageProperties storageProperties;

  @Override
  public Health health() {
    Path root = Paths.get(storageProperties.getUpload().getLocation()).toAbsolutePath().normalize();
    try {
      if (!Files.exists(root)) {
        return Health.down()
            .withDetail("path", root.toString())
            .withDetail("reason", "storage_root_missing")
            .build();
      }

      long usable = Files.getFileStore(root).getUsableSpace();
      long total = Files.getFileStore(root).getTotalSpace();

      long min = storageProperties.getUpload().getMinFreeSpace().toBytes();
      long warn = storageProperties.getUpload().getWarnFreeSpace().toBytes();

      Health.Builder builder = usable < min ? Health.down() : Health.up();

      builder
          .withDetail("path", root.toString())
          .withDetail("usableBytes", usable)
          .withDetail("totalBytes", total)
          .withDetail("minFreeBytes", min)
          .withDetail("warnFreeBytes", warn);

      if (usable < warn) {
        builder.withDetail("warning", "low_disk_space");
      }

      return builder.build();
    } catch (IOException e) {
      log.error("Disk health check failed for {}", root, e);
      return Health.down(e)
          .withDetail("path", root.toString())
          .withDetail("reason", "disk_check_failed")
          .build();
    }
  }
}

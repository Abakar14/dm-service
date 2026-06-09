package com.bytmasoft.dm.config;


import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@Getter
@Setter
//@Configuration
@ConfigurationProperties(prefix = "file.storage")
public class StorageProperties {

  private Upload upload;
  private Document document;
  private Cleanup cleanup;
  private Limits limits;
  private Download download;


  @Getter
  @Setter
  public static class Upload {

    /**
     * Maximum allowed upload size for a single file.
     *
     * <p>Use Spring's {@link DataSize} format in configuration, e.g. "5MB".
     */
    private DataSize maxFileSize;
    private DataSize maxTotalSize;
    private String location;
    private int maxFiles;


    /**
     * Minimum free space required on the storage volume for the service to be considered healthy.
     *
     * <p>Example: "1GB".
     */
    private DataSize minFreeSpace = DataSize.ofGigabytes(1);

    /**
     * Warning threshold (reported in health details) when free space falls below this value.
     */
    private DataSize warnFreeSpace = DataSize.ofGigabytes(5);

  }

  @Getter
  @Setter
  public static class Document {

    private List<String> supportedFileTypes;
    /**
     * Allowed MIME types (optional but recommended). If empty, MIME validation will be lenient.
     */
    private List<String> allowedMimeTypes;

    /**
     * Whether SVG uploads are allowed. Default false (recommended).
     */
    private boolean allowSvg = false;

  }

  @Getter
  @Setter
  public static class Cleanup {

    /**
     * Enable scheduled cleanup tasks (trash purge).
     */
    private boolean enabled = false;

    /**
     * Trash retention in days before physical deletion.
     */
    private int trashRetentionDays = 14;

    /**
     * Cron for cleanup job. Default: daily at 03:15.
     */
    private String cron = "0 15 3 * * *";
  }


  // =========================================
  // NEW SECURITY LIMITS BLOCK
  // =========================================
  @Getter
  @Setter
  public static class Limits {

    /**
     * Maximum allowed decoded image width.
     */
    private int maxImageWidth = 4096;

    /**
     * Maximum allowed decoded image height.
     */
    private int maxImageHeight = 4096;

    /**
     * Maximum allowed pixel count (width × height).
     */
    private long maxImagePixels = 16_000_000L;

    /**
     * Maximum allowed aspect ratio (e.g. 40 = 1:40).
     */
    private double maxAspectRatio = 40.0;

    /**
     * Enable structural ImageIO validation.
     */
    private boolean enableStructuralValidation = true;

    /**
     * Enable magic number validation.
     */
    private boolean enableSignatureValidation = true;

    /**
     * Enable upload rate limiting.
     */
    private boolean enableRateLimiting = true;
  }

  @Getter
  @Setter
  public static class Download {

    private String thumbUrl;
    private String downloadUrl;

  }


}

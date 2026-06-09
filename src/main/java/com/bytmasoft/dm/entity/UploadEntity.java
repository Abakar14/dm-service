package com.bytmasoft.dm.entity;

import com.bytmasoft.dm.enums.DomainType;
import com.bytmasoft.dm.enums.RenditionType;
import com.bytmasoft.dm.enums.UploadType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

/**
 * {domain}-{owner_id}-{uuid}.{ext} Example: student-12345-6789abc1.pdf /uploads/ ├── domain/ #
 * e.g., user, student, found-item │   ├── owner_type │   │   │   │── {owner_id}/  # Owner
 * identifier │   │   │   ├── │── document_type │   │   │   │   │   │── documents/ │   │   │   ├── │
 * │── images/ │   │   └── │   │   │── temp/    # For temporary uploads └── shared/ ──  ──  ──
 * <p>
 * uploads/domainType/ownerType/ownerId/uploadType/filename example:
 * uploads/dalilak/user/121212/pdfs/filename1 uploads/school/student/1212343/pdfs/filename5
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "uploads")
public class UploadEntity implements Serializable {

  @Id
  @GeneratedValue(generator = "uuid")
  @GenericGenerator(name = "uuid", strategy = "uuid2")
  private String id;

  @Column(name = "file_name", nullable = false)
  private String fileName;

  @Column(name = "original_file_name")
  private String originalFileName;

  @Column(name = "item_id", nullable = false)
  private Long itemId;

  @Column(name = "file_size")
  private Long fileSize;

  @Enumerated(EnumType.STRING)
  @Column(name = "upload_type", nullable = false)
  private UploadType uploadType;

  @Builder.Default
  @Column(nullable = false)
  @JsonProperty("active")
  private Boolean isActive = true;

  @Column(nullable = false)
  private String addedBy;

  private String modifiedBy;

  @Column(nullable = false, updatable = false)
  private Instant addedOn;

  @Column(nullable = false)
  private Instant modifiedOn;

  private String filePath;

  @Column(name = "content_type")
  private String contentType;

  @Column(columnDefinition = "int4")
  private Integer version;


  @Enumerated(EnumType.STRING)
  @Column(name = "domain_type", nullable = false)
  private DomainType domainType;

  @Column
  private String contentDisposition; // inline/attachment

  @Column
  private String mimeType;

  @Builder.Default
  private boolean deleted = false;

  @Builder.Default
  @JsonProperty("archived")
  private boolean isArchived = false;

  @Builder.Default
  @Column(name = "rendition_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private RenditionType renditionType = RenditionType.ORIGINAL;

  @Column(name = "parent_upload_id")
  private String parentUploadId; // original upload id if this is THUMBNAIL

  private Integer width;
  private Integer height;

  @PreUpdate
  void preUpdate() {
    setModifiedOn(Instant.now());
  }

  @PrePersist
  void prePersist() {
    if (getAddedOn() == null) {
      setAddedOn(Instant.now());
    }
    setModifiedOn(getAddedOn());
  }

}
package com.bytmasoft.dm.dto.response;


import com.bytmasoft.dm.enums.DomainType;
import com.bytmasoft.dm.enums.UploadType;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class UploadEntityResponse {

  String id;

  String fileName;

  String originalFileName;

  Long ownerId;

  Long fileSize;

  String filePath;

  Integer version;

  UploadType uploadType;

  Boolean isActive;

  String addedBy;

  String modifiedBy;

  Instant addedOn;

  Instant modifiedOn;

  String contentType;

  DomainType domainType;

  boolean deleted;

  boolean isArchived;

  String mimeType;

/*  private String publicUrl;
  private String thumbnailUrl;
  private String previewUrl;
  private List<UploadRenditionResponse> renditions;*/

}

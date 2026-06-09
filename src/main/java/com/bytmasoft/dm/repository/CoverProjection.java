package com.bytmasoft.dm.repository;

public interface CoverProjection {

  Long getOwnerId();

  String getOriginalId();      // UUID as String

  String getThumbnailId();     //

  Long getImageCount();
}

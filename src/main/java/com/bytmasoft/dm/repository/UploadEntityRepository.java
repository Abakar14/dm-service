package com.bytmasoft.dm.repository;

import com.bytmasoft.dm.entity.UploadEntity;
import com.bytmasoft.dm.enums.DomainType;
import com.bytmasoft.dm.enums.RenditionType;
import com.bytmasoft.dm.enums.UploadType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UploadEntityRepository extends JpaRepository<UploadEntity, String>,
    JpaSpecificationExecutor<UploadEntity> {

  List<UploadEntity> findByDomainTypeAndUploadTypeAndItemId(DomainType domainType,
      UploadType uploadType, Long ownerId);

  Optional<UploadEntity> findByDomainTypeAndUploadTypeAndItemIdAndVersion(DomainType domainType,
      UploadType uploadType, Long ownerId, Integer version);

  Optional<UploadEntity> findFirstByParentUploadIdAndRenditionType(String parentUploadId,
      RenditionType renditionType);

  List<UploadEntity> findByParentUploadId(String parentUploadId);

  Optional<UploadEntity> findFirstByIdAndRenditionType(String id,
      RenditionType renditionType); // optional

  void deleteByParentUploadId(String parentUploadId);

  List<UploadEntity> findByDomainTypeAndUploadTypeAndItemIdOrderByVersionAsc(
      DomainType domainType, UploadType uploadType, Long itemId);

  @Query(value = """
      WITH latest_originals AS (
          SELECT DISTINCT ON (item_id) 
              item_id,
              id AS original_id,
              added_on
          FROM uploads
          WHERE rendition_type = 'ORIGINAL'
            AND upload_type = :uploadType
            AND item_id IN (:ownerIds)
          ORDER BY item_id, added_on DESC
      )
      SELECT 
          lo.item_id AS ownerId,
          lo.original_id AS originalId,
          t.id AS thumbnailId,
          (SELECT COUNT(*) FROM uploads u2 
           WHERE u2.item_id = lo.item_id 
             AND u2.rendition_type = 'ORIGINAL'
             AND u2.upload_type = :uploadType) AS imageCount
      FROM latest_originals lo
      LEFT JOIN uploads t 
          ON t.parent_upload_id = lo.original_id 
          AND t.rendition_type = 'THUMBNAIL'
      """, nativeQuery = true)
  List<CoverProjection> findCoversAndThumbnails(
      @Param("ownerIds") List<Long> ownerIds,
      @Param("uploadType") String uploadType
  );


  @Query(value = """
      WITH latest_originals AS (
          SELECT DISTINCT ON (item_id) 
              item_id,
              id AS original_id,
              added_on
          FROM uploads
          WHERE rendition_type = 'ORIGINAL'           
            AND item_id IN (:ownerIds)
          ORDER BY item_id, added_on DESC
      )
      SELECT 
          lo.item_id AS ownerId,
          lo.original_id AS originalId,
          t.id AS thumbnailId,
          (SELECT COUNT(*) FROM uploads u2 
           WHERE u2.item_id = lo.item_id 
             AND u2.rendition_type = 'ORIGINAL') AS imageCount
      FROM latest_originals lo
      LEFT JOIN uploads t 
          ON t.parent_upload_id = lo.original_id 
          AND t.rendition_type = 'THUMBNAIL'
      """, nativeQuery = true)
  List<CoverProjection> findCoversAndThumbnails(@Param("ownerIds") List<Long> ownerIds);

  @Query("""
          select coalesce(max(u.version), 0)
          from UploadEntity u
          where u.domainType = :domainType
            and u.uploadType = :uploadType
            and u.itemId = :ownerId
      """)
  int findMaxVersion(
      @Param("domainType") DomainType domainType,
      @Param("uploadType") UploadType uploadType,
      @Param("ownerId") Long ownerId
  );

  @Query("""
      select u
      from UploadEntity u
      where u.itemId in :itemIds
        and u.deleted = false
        and u.isActive = true
        and u.isArchived = false
        and u.domainType = :domainType
        and u.contentType like 'image/%'
      order by u.itemId asc, u.addedOn asc
      """)
  List<UploadEntity> findActiveItemImages(
      @Param("itemIds") List<Long> itemIds,
      @Param("domainType") DomainType domainType
  );
}



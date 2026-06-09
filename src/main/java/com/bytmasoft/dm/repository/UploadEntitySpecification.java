package com.bytmasoft.dm.repository;

import com.bytmasoft.dm.entity.UploadEntity;
import com.bytmasoft.dm.enums.DomainType;
import com.bytmasoft.dm.enums.UploadType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;


@Component
public class UploadEntitySpecification {

  public Specification<UploadEntity> getSpecificationByDomainTypeByUploadTypeOwnerIdAndVersion(
      DomainType domainType,
      UploadType uploadType, Long itemId, Integer version) {

    return (root, query, criteriaBuilder) -> {

      List<Predicate> predicates = new ArrayList<>();
      if (domainType != null) {
        predicates.add(criteriaBuilder.equal(root.get("domainType"), domainType));
      }

      if (uploadType != null) {
        predicates.add(criteriaBuilder.equal(root.get("uploadType"), uploadType));
      }

      if (itemId != null && itemId > 0) {
        predicates.add(criteriaBuilder.equal(root.get("itemId"), itemId));
      }
      if (version != null && version > 0) {
        predicates.add(criteriaBuilder.equal(root.get("version"), version));
      }

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }

  public Specification<UploadEntity> getDocumentsOwnerListByUploadTypeAndVersion(
      DomainType domainType, UploadType uploadType, List<Long> itemIds,
      Integer version) {

    return (root, query, criteriaBuilder) -> {

      List<Predicate> predicates = new ArrayList<>();

      if (itemIds != null && itemIds.size() > 0) {
        predicates.add(criteriaBuilder.and(root.get("itemId").in(itemIds)));
      }

      if (domainType != null) {
        predicates.add(criteriaBuilder.equal(root.get("domainType"), domainType));
      }
      if (uploadType != null) {
        predicates.add(criteriaBuilder.equal(root.get("uploadType"), uploadType));
      }

      if (version != null && version > 0) {
        predicates.add(criteriaBuilder.equal(root.get("version"), version));
      }

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }

}

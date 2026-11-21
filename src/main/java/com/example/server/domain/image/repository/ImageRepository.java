package com.example.server.domain.image.repository;

import com.example.server.domain.image.entity.Image;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImageRepository extends JpaRepository<Image, Long> {

  Optional<Image> findByPublicIdAndDeletedAtIsNull(String publicId);

  boolean existsByPublicId(String publicId);

  /** 본인이 올린 이미지 또는 시스템 기본 이미지(created_by NULL)만, 바이너리(data) 없이 id만 조회한다. */
  @Query("""
      select new com.example.server.domain.image.repository.ImageKey(i.id, i.publicId)
      from Image i
      where i.publicId in :publicIds and i.deletedAt is null
        and (i.createdBy = :userId or i.createdBy is null)
      """)
  List<ImageKey> findUsableKeys(@Param("publicIds") Collection<String> publicIds, @Param("userId") Long userId);

  @Query("""
      select new com.example.server.domain.image.repository.ProfileImageView(i.id, i.contentType, i.byteSize)
      from Image i
      where i.publicId = :publicId and i.deletedAt is null
        and (i.createdBy = :userId or i.createdBy is null)
      """)
  Optional<ProfileImageView> findUsableProfileImage(@Param("publicId") String publicId, @Param("userId") Long userId);
}

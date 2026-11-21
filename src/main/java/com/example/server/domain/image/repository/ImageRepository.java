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

  /** 바이너리(data)를 읽지 않고 id만 조회한다. */
  @Query("""
      select new com.example.server.domain.image.repository.ImageKey(i.id, i.publicId)
      from Image i
      where i.publicId in :publicIds and i.deletedAt is null
      """)
  List<ImageKey> findKeysByPublicIds(@Param("publicIds") Collection<String> publicIds);
}

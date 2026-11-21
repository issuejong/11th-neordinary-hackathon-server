package com.example.server.domain.routine.repository;

import com.example.server.domain.routine.entity.Routine;
import com.example.server.domain.routine.repository.projection.RoutineHeaderView;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoutineRepository extends JpaRepository<Routine, Long> {

  Optional<Routine> findByPublicIdAndDeletedAtIsNull(String publicId);

  boolean existsByPublicId(String publicId);

  /** 루틴 + 작성자 정보 (이미지 바이너리는 읽지 않음) */
  @Query("""
      select new com.example.server.domain.routine.repository.projection.RoutineHeaderView(
          r.id, r.publicId, r.title, r.content, u.publicId, u.nickname, pi.publicId, r.createdAt, r.updatedAt)
      from Routine r
      join r.user u
      join u.profileImage pi
      where r.publicId = :publicId and r.deletedAt is null
      """)
  Optional<RoutineHeaderView> findHeader(@Param("publicId") String publicId);
}

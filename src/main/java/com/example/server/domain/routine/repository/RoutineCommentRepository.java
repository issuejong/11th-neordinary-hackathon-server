package com.example.server.domain.routine.repository;

import com.example.server.domain.routine.entity.RoutineComment;
import com.example.server.domain.routine.repository.projection.RoutineCommentView;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoutineCommentRepository extends JpaRepository<RoutineComment, Long> {

  long countByRoutine_IdAndDeletedAtIsNull(Long routineId);

  boolean existsByPublicId(String publicId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<RoutineComment> findByRoutine_IdAndPublicIdAndDeletedAtIsNull(Long routineId, String publicId);

  @Query("""
      select new com.example.server.domain.routine.repository.projection.RoutineCommentView(
          c.publicId, u.id, u.nickname, i.publicId, c.content, c.createdAt)
      from RoutineComment c join c.user u join u.profileImage i
      where c.routine.id = :routineId and c.deletedAt is null
      order by c.id asc
      """)
  List<RoutineCommentView> findActiveViews(@Param("routineId") Long routineId);

  @Query("""
      select new com.example.server.domain.routine.repository.projection.RoutineCommentView(
          c.publicId, u.id, u.nickname, i.publicId, c.content, c.createdAt)
      from RoutineComment c join c.user u join u.profileImage i
      where c.publicId = :publicId and c.deletedAt is null
      """)
  Optional<RoutineCommentView> findActiveView(@Param("publicId") String publicId);
}

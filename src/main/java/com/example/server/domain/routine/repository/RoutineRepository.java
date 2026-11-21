package com.example.server.domain.routine.repository;

import com.example.server.domain.routine.entity.Routine;
import com.example.server.domain.routine.repository.projection.RoutineHeaderView;
import com.example.server.domain.routine.repository.projection.RoutineInteractionView;
import com.example.server.domain.routine.repository.projection.RoutineSummaryView;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoutineRepository extends JpaRepository<Routine, Long> {

  Optional<Routine> findByPublicIdAndDeletedAtIsNull(String publicId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("from Routine r where r.publicId = :publicId and r.deletedAt is null")
  Optional<Routine> findActiveForUpdate(@Param("publicId") String publicId);

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

  @Query("""
      select new com.example.server.domain.routine.repository.projection.RoutineInteractionView(
          (select count(l) from RoutineLike l where l.routine = r and l.deletedAt is null),
          (select count(l) from RoutineLike l where l.routine = r and l.user.id = :userId and l.deletedAt is null),
          (select count(p) from RoutineParticipation p
           where p.routine = r and p.status = IN_PROGRESS and p.deletedAt is null),
          (select count(p) from RoutineParticipation p
           where p.routine = r and p.status = COMPLETED and p.deletedAt is null),
          (select p.status from RoutineParticipation p
           where p.routine = r and p.user.id = :userId and p.deletedAt is null))
      from Routine r
      where r.publicId = :publicId and r.deletedAt is null
      """)
  Optional<RoutineInteractionView> findInteractions(@Param("publicId") String publicId, @Param("userId") Long userId);

  // ---------- 목록: 커서(키셋) 페이지네이션 ----------
  // 삭제되지 않은 루틴만. authorId(작성자 publicId)가 있으면 그 사람 루틴만 (마이페이지 내 루틴).
  // publicId(ULID, 생성 시각순)로 최신순 · 동점 처리.
  // 커서 = 직전 페이지 마지막 항목의 (정렬 값, publicId). 첫 페이지는 RoutineCursor.first()의 최댓값.

  @Query("""
      select new com.example.server.domain.routine.repository.projection.RoutineSummaryView(
          r.publicId, r.title, r.content, u.publicId, u.nickname, pi.publicId,
          (select ci.publicId from RoutineCard c join c.image ci where c.routine = r and c.sortOrder = 1),
          (select count(c) from RoutineCard c where c.routine = r),
          (select count(l) from RoutineLike l where l.routine = r and l.deletedAt is null),
          (select count(p) from RoutineParticipation p where p.routine = r and p.deletedAt is null),
          r.createdAt)
      from Routine r
      join r.user u
      join u.profileImage pi
      where r.deletedAt is null
        and (:authorId is null or u.publicId = :authorId)
        and r.publicId < :publicId
      order by r.publicId desc
      """)
  List<RoutineSummaryView> findLatest(@Param("authorId") String authorId, @Param("publicId") String publicId,
      Pageable pageable);

  @Query("""
      select new com.example.server.domain.routine.repository.projection.RoutineSummaryView(
          r.publicId, r.title, r.content, u.publicId, u.nickname, pi.publicId,
          (select ci.publicId from RoutineCard c join c.image ci where c.routine = r and c.sortOrder = 1),
          (select count(c) from RoutineCard c where c.routine = r),
          (select count(l) from RoutineLike l where l.routine = r and l.deletedAt is null),
          (select count(p) from RoutineParticipation p where p.routine = r and p.deletedAt is null),
          r.createdAt)
      from Routine r
      join r.user u
      join u.profileImage pi
      where r.deletedAt is null
        and (:authorId is null or u.publicId = :authorId)
        and ((select count(l) from RoutineLike l where l.routine = r and l.deletedAt is null) < :count
          or ((select count(l) from RoutineLike l where l.routine = r and l.deletedAt is null) = :count and r.publicId < :publicId))
      order by (select count(l) from RoutineLike l where l.routine = r and l.deletedAt is null) desc, r.publicId desc
      """)
  List<RoutineSummaryView> findMostLiked(@Param("authorId") String authorId, @Param("count") long count, @Param("publicId") String publicId,
      Pageable pageable);

  @Query("""
      select new com.example.server.domain.routine.repository.projection.RoutineSummaryView(
          r.publicId, r.title, r.content, u.publicId, u.nickname, pi.publicId,
          (select ci.publicId from RoutineCard c join c.image ci where c.routine = r and c.sortOrder = 1),
          (select count(c) from RoutineCard c where c.routine = r),
          (select count(l) from RoutineLike l where l.routine = r and l.deletedAt is null),
          (select count(p) from RoutineParticipation p where p.routine = r and p.deletedAt is null),
          r.createdAt)
      from Routine r
      join r.user u
      join u.profileImage pi
      where r.deletedAt is null
        and (:authorId is null or u.publicId = :authorId)
        and ((select count(p) from RoutineParticipation p where p.routine = r and p.deletedAt is null) < :count
          or ((select count(p) from RoutineParticipation p where p.routine = r and p.deletedAt is null) = :count and r.publicId < :publicId))
      order by (select count(p) from RoutineParticipation p where p.routine = r and p.deletedAt is null) desc, r.publicId desc
      """)
  List<RoutineSummaryView> findMostParticipated(@Param("authorId") String authorId, @Param("count") long count, @Param("publicId") String publicId,
      Pageable pageable);
}

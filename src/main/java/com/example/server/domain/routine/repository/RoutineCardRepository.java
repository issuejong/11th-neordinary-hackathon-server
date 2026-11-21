package com.example.server.domain.routine.repository;

import com.example.server.domain.routine.entity.RoutineCard;
import com.example.server.domain.routine.repository.projection.RoutineCardView;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoutineCardRepository extends JpaRepository<RoutineCard, Long> {

  /** 카드 목록 (sort_order 순, 이미지 바이너리는 읽지 않음) */
  @Query("""
      select new com.example.server.domain.routine.repository.projection.RoutineCardView(
          c.publicId, c.sortOrder, c.title, c.content, c.cardTime, i.publicId)
      from RoutineCard c
      join c.image i
      where c.routine.id = :routineId
      order by c.sortOrder
      """)
  List<RoutineCardView> findViewsByRoutineId(@Param("routineId") Long routineId);

  /** 루틴 수정 시 카드 전체 교체. (routine_id, sort_order) 유니크 충돌을 막기 위해 즉시 DELETE 실행 */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from RoutineCard c where c.routine.id = :routineId")
  void deleteAllByRoutineId(@Param("routineId") Long routineId);
}

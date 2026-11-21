package com.example.server.domain.routine.repository;

import com.example.server.domain.routine.entity.RoutineParticipation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutineParticipationRepository extends JpaRepository<RoutineParticipation, Long> {

  Optional<RoutineParticipation> findByRoutine_IdAndUser_Id(Long routineId, Long userId);

  @Query("""
      select p from RoutineParticipation p join fetch p.user u join fetch u.profileImage
      where p.routine.id = :routineId and p.deletedAt is null and p.status = COMPLETED and p.workJson is not null
      order by p.completedAt desc, p.id desc
      """)
  List<RoutineParticipation> findPublishedResults(@Param("routineId") Long routineId);

  Optional<RoutineParticipation> findByRoutine_IdAndUser_PublicId(Long routineId, String publicId);
}

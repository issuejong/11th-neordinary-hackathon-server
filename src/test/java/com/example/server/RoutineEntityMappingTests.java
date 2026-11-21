package com.example.server;

import com.example.server.domain.image.entity.Image;
import com.example.server.domain.routine.entity.Routine;
import com.example.server.domain.routine.entity.RoutineCard;
import com.example.server.domain.routine.entity.RoutineComment;
import com.example.server.domain.routine.entity.RoutineLike;
import com.example.server.domain.routine.entity.RoutineParticipation;
import com.example.server.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RoutineEntityMappingTests {

  @Autowired
  private EntityManager entityManager;

  @Test
  void savesAndReadsAllSevenEntities() {
    byte[] data = {1, 2, 3};
    Image image = new Image(publicId(1), "image/png", data, null);
    entityManager.persist(image);
    User owner = new User(publicId(2), "owner-token", "작성자", image);
    User member = new User(publicId(3), "member-token", "참여자", image);
    entityManager.persist(owner);
    entityManager.persist(member);
    Routine routine = new Routine(publicId(4), owner, "아침 준비 루틴", "출근 전 하루를 준비해요.");
    entityManager.persist(routine);
    RoutineCard card = new RoutineCard(publicId(5), routine, 1, image, "첫 번째 활동", null, LocalTime.of(7, 30));
    RoutineComment comment = new RoutineComment(publicId(6), routine, member, "좋아요");
    RoutineLike like = new RoutineLike(routine, member);
    RoutineParticipation participation = new RoutineParticipation(routine, member, LocalDateTime.of(2026, 11, 21, 7, 30));
    entityManager.persist(card);
    entityManager.persist(comment);
    entityManager.persist(like);
    entityManager.persist(participation);
    entityManager.flush();
    entityManager.clear();

    User savedUser = entityManager.createQuery("from AppUser where publicId = :publicId", User.class)
        .setParameter("publicId", member.getPublicId())
        .getSingleResult();
    assertArrayEquals(data, savedUser.getProfileImage().getData());
    Routine savedRoutine = entityManager.find(Routine.class, routine.getId());
    assertEquals(owner.getId(), savedRoutine.getCreatedBy());
    assertEquals("아침 준비 루틴", savedRoutine.getTitle());
    assertEquals("출근 전 하루를 준비해요.", savedRoutine.getContent());
    assertEquals(LocalTime.of(7, 30), entityManager.find(RoutineCard.class, card.getId()).getCardTime());
    assertNotNull(entityManager.find(RoutineComment.class, comment.getId()).getCreatedAt());
    assertEquals(member.getId(), entityManager.find(RoutineLike.class, like.getId()).getUser().getId());
    assertEquals(RoutineParticipation.Status.IN_PROGRESS,
        entityManager.find(RoutineParticipation.class, participation.getId()).getStatus());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 11})
  void databaseRejectsDuplicateOrOutOfRangeCardOrder(int invalidOrder) {
    Image image = new Image(publicId(1), "image/png", new byte[]{1}, null);
    entityManager.persist(image);
    User user = new User(publicId(2), "owner-token", "작성자", image);
    entityManager.persist(user);
    Routine routine = new Routine(publicId(3), user, "아침 준비 루틴", null);
    entityManager.persist(routine);
    entityManager.persist(new RoutineCard(publicId(4), routine, 1, image, "첫 번째 활동", null, null));
    entityManager.flush();

    assertThrows(ConstraintViolationException.class, () -> {
      entityManager.persist(new RoutineCard(publicId(5), routine, invalidOrder, image, "두 번째 활동", null, null));
      entityManager.flush();
    });
  }

  private String publicId(int suffix) {
    return "%026d".formatted(suffix);
  }
}

package com.example.server.global.init;

import com.example.server.domain.image.entity.Image;
import com.example.server.domain.image.repository.ImageRepository;
import com.example.server.domain.routine.dto.ParticipationWorkDTO.Draft;
import com.example.server.domain.routine.dto.ParticipationWorkDTO.DraftCard;
import com.example.server.domain.user.entity.TestAccountLease;
import com.example.server.domain.user.repository.TestAccountLeaseRepository;
import tools.jackson.databind.ObjectMapper;
import com.example.server.domain.routine.entity.Routine;
import com.example.server.domain.routine.entity.RoutineCard;
import com.example.server.domain.routine.entity.RoutineComment;
import com.example.server.domain.routine.entity.RoutineParticipation;
import com.example.server.domain.routine.repository.RoutineCardRepository;
import com.example.server.domain.routine.repository.RoutineCommentRepository;
import com.example.server.domain.routine.repository.RoutineParticipationRepository;
import com.example.server.domain.routine.repository.RoutineRepository;
import com.example.server.domain.user.entity.User;
import com.example.server.global.util.Ulid;
import jakarta.persistence.EntityManager;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기본 프로필 이미지와 Swagger 예시용 데모 데이터를 넣는다.
 * 이미 있으면 건너뛰므로 재시작해도 중복되지 않는다 (고정 publicId 기준).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataInitializer implements ApplicationRunner {

  private final ImageRepository imageRepository;
  private final RoutineRepository routineRepository;
  private final RoutineCardRepository cardRepository;
  private final RoutineCommentRepository commentRepository;
  private final RoutineParticipationRepository participationRepository;
  private final TestAccountLeaseRepository leaseRepository;
  private final ObjectMapper json;
  private final EntityManager entityManager;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    Image image1 = defaultImage(DemoData.IMAGE_ID, new Color(0xFF, 0xB4, 0x7A));
    Image image2 = defaultImage(DemoData.IMAGE_ID_2, new Color(0x8E, 0xC5, 0xFC));
    Image image3 = defaultImage(DemoData.IMAGE_ID_3, new Color(0xA7, 0xE3, 0xB5));

    User user = demoUser(DemoData.USER_ID, DemoData.USER_NICKNAME, image1);
    User participant = demoUser(DemoData.PARTICIPANT_ID, "데모 참여자", image2);
    User author = demoUser(DemoData.AUTHOR_ID, "데모 작성자", image3);
    registerReviewAccount(user);

    if (!routineRepository.existsByPublicId(DemoData.ROUTINE_ID)) {
      Routine routine = routineRepository.save(
          new Routine(DemoData.ROUTINE_ID, user, "갓생 아침 루틴", "출근 전 1시간으로 하루를 시작해요."));
      cardRepository.saveAll(List.of(
          new RoutineCard(Ulid.generate(), routine, 1, image1, "기상 후 물 한 잔 마시기", "미지근한 물 500ml", LocalTime.of(7, 0)),
          new RoutineCard(Ulid.generate(), routine, 2, image2, "10분 스트레칭 하기", "목, 어깨 위주로", LocalTime.of(7, 10)),
          new RoutineCard(Ulid.generate(), routine, 3, image3, "오늘 할 일 세 개 적기", null, LocalTime.of(7, 30))));
      log.info("Demo routine created: {}", DemoData.ROUTINE_ID);
    }
    if (!routineRepository.existsByPublicId(DemoData.EDITABLE_ROUTINE_ID)) {
      Routine routine = routineRepository.save(
          new Routine(DemoData.EDITABLE_ROUTINE_ID, user, "수정 · 삭제 테스트용 루틴", "Swagger에서 수정/삭제를 시험해 보세요."));
      cardRepository.save(
          new RoutineCard(Ulid.generate(), routine, 1, image2, "자기 전 독서 20분", null, LocalTime.of(23, 0)));
      log.info("Demo routine created: {}", DemoData.EDITABLE_ROUTINE_ID);
    }
    if (!routineRepository.existsByPublicId(DemoData.DELETABLE_ROUTINE_ID)) {
      Routine routine = routineRepository.save(
          new Routine(DemoData.DELETABLE_ROUTINE_ID, user, "삭제 테스트용 루틴", "Swagger에서 삭제를 시험해 보세요. 10초 안에 자동 복구됩니다."));
      cardRepository.save(
          new RoutineCard(Ulid.generate(), routine, 1, image3, "삭제해도 다시 돌아와요", null, LocalTime.of(12, 0)));
      log.info("Demo routine created: {}", DemoData.DELETABLE_ROUTINE_ID);
    }
    if (!commentRepository.existsByPublicId(DemoData.DELETABLE_COMMENT_ID)) {
      routineRepository.findByPublicIdAndDeletedAtIsNull(DemoData.ROUTINE_ID)
          .ifPresent(routine -> commentRepository.save(
              new RoutineComment(DemoData.DELETABLE_COMMENT_ID, routine, user, "삭제 예시용 댓글이에요.")));
    }
    if (!commentRepository.existsByPublicId(DemoData.COMMENT_ID)) {
      routineRepository.findByPublicIdAndDeletedAtIsNull(DemoData.ROUTINE_ID)
          .ifPresent(routine -> commentRepository.save(
              new RoutineComment(DemoData.COMMENT_ID, routine, user, "오늘부터 해볼게요.")));
    }
    // 함께하기 예시 루틴 (데모 작성자 소유, 스텝 1개: Swagger 예시 본문의 스텝 수와 같음)
    participationRoutine(DemoData.JOIN_ROUTINE_ID, author, "함께하기 예시: 참여 · 초안", image1);
    participationRoutine(DemoData.DRAFT_ROUTINE_ID, author, "함께하기 예시: 초안", image3);
    participationRoutine(DemoData.COMPLETE_ROUTINE_ID, author, "함께하기 예시: 완료 제출", image2);
    participationRoutine(DemoData.RESULT_EDIT_ROUTINE_ID, author, "함께하기 예시: 결과 수정", image3);
    participationRoutine(DemoData.RESULT_DELETE_ROUTINE_ID, author, "함께하기 예시: 결과 삭제", image1);
    restoreDeletedDemoData(); // 재배포 직후에도 바로 복구
  }

  /** 데모 유저를 심사용 테스트 계정(8번 슬롯)으로 등록한다. 이미 있으면 건너뛴다. */
  private void registerReviewAccount(User user) {
    if (!leaseRepository.existsBySlot(DemoData.REVIEW_SLOT) && !leaseRepository.existsByUser_Id(user.getId())) {
      leaseRepository.save(new TestAccountLease(DemoData.REVIEW_SLOT, user));
      log.info("Review test account registered: slot {}", DemoData.REVIEW_SLOT);
    }
  }

  private void participationRoutine(String publicId, User author, String title, Image image) {
    if (!routineRepository.existsByPublicId(publicId)) {
      Routine routine = routineRepository.save(
          new Routine(publicId, author, title, "Swagger 함께하기 예시용 루틴입니다. 10초마다 원래 상태로 돌아갑니다."));
      cardRepository.save(new RoutineCard(Ulid.generate(), routine, 1, image, "예시 스텝 하나", null, LocalTime.of(8, 0)));
      log.info("Demo routine created: {}", publicId);
    }
  }

  /**
   * 심사자가 Swagger 예시로 데모 루틴 · 댓글을 삭제해도, 10초 안에 되살려서
   * 다음 사람이 수정 · 삭제 예시를 실행해도 404가 나지 않게 한다.
   */
  @Scheduled(initialDelayString = "${app.demo.restore-interval:60s}", fixedDelayString = "${app.demo.restore-interval:60s}")
  @Transactional
  public void restoreDeletedDemoData() {
    for (String publicId : List.of(DemoData.ROUTINE_ID, DemoData.EDITABLE_ROUTINE_ID, DemoData.DELETABLE_ROUTINE_ID,
        DemoData.JOIN_ROUTINE_ID, DemoData.DRAFT_ROUTINE_ID, DemoData.COMPLETE_ROUTINE_ID, DemoData.RESULT_EDIT_ROUTINE_ID,
        DemoData.RESULT_DELETE_ROUTINE_ID)) {
      routineRepository.findByPublicId(publicId)
          .filter(routine -> routine.getDeletedAt() != null)
          .ifPresent(routine -> {
            routine.restoreDeleted(routine.getUser().getId());
            log.info("Demo routine restored: {}", publicId);
          });
    }
    for (String publicId : List.of(DemoData.COMMENT_ID, DemoData.DELETABLE_COMMENT_ID)) {
      commentRepository.findByPublicId(publicId)
          .filter(comment -> comment.getDeletedAt() != null)
          .ifPresent(comment -> {
            comment.restoreDeleted(comment.getUser().getId());
            log.info("Demo comment restored: {}", publicId);
          });
    }
    resetParticipations();
  }

  /**
   * 함께하기 예시가 항상 성공하도록 데모 참여 상태를 기준 상태로 되돌린다.
   * - 참여 시작 · 취소 · 초안 / 완료 제출 예시: 데모 유저가 "참여 중(결과 미공개)"
   * - 결과 수정 · 삭제 예시, 상세 루틴의 함께한 결과: "결과 공개"
   */
  private void resetParticipations() {
    User user = userByPublicId(DemoData.USER_ID);
    User participant = userByPublicId(DemoData.PARTICIPANT_ID);
    if (user == null || participant == null) {
      return;
    }
    inProgress(DemoData.JOIN_ROUTINE_ID, user);
    inProgress(DemoData.DRAFT_ROUTINE_ID, user);
    inProgress(DemoData.COMPLETE_ROUTINE_ID, user);
    published(DemoData.RESULT_EDIT_ROUTINE_ID, user);
    published(DemoData.RESULT_DELETE_ROUTINE_ID, user);
    published(DemoData.ROUTINE_ID, participant);
  }

  private void inProgress(String routineId, User user) {
    RoutineParticipation participation = participation(routineId, user);
    if (participation == null) {
      return;
    }
    if (participation.hasPublishedWork()) {
      participation.discard(LocalDateTime.now());
    }
    if (participation.getDeletedAt() != null) {
      participation.joinAgain(LocalDateTime.now());
    }
  }

  private void published(String routineId, User user) {
    RoutineParticipation participation = participation(routineId, user);
    if (participation == null || participation.hasPublishedWork()) {
      return;
    }
    if (participation.getDeletedAt() != null) {
      participation.joinAgain(LocalDateTime.now());
    }
    participation.saveWork(json.writeValueAsString(new Draft("함께한 나의 하루", "예시 루틴을 따라 해봤어요.",
        List.of(new DraftCard(DemoData.IMAGE_ID, "예시 스텝 하나", null, "08:00")))));
    participation.complete(LocalDateTime.now());
  }

  /** 없으면 "참여 중"으로 새로 만든다. 루틴이 없으면 null */
  private RoutineParticipation participation(String routineId, User user) {
    return routineRepository.findByPublicIdAndDeletedAtIsNull(routineId)
        .map(routine -> participationRepository.findByRoutine_IdAndUser_Id(routine.getId(), user.getId())
            .orElseGet(() -> participationRepository.save(new RoutineParticipation(routine, user, LocalDateTime.now()))))
        .orElse(null);
  }

  private User userByPublicId(String publicId) {
    return entityManager.createQuery("from AppUser u where u.publicId = :publicId", User.class)
        .setParameter("publicId", publicId)
        .getResultList().stream().findFirst().orElse(null);
  }

  private Image defaultImage(String publicId, Color color) {
    return imageRepository.findByPublicIdAndDeletedAtIsNull(publicId)
        .orElseGet(() -> imageRepository.save(new Image(publicId, "image/png", solidPng(color), null)));
  }

  private User demoUser(String publicId, String nickname, Image profileImage) {
    List<User> users = entityManager
        .createQuery("from AppUser u where u.publicId = :publicId", User.class)
        .setParameter("publicId", publicId)
        .getResultList();
    if (!users.isEmpty()) {
      return users.getFirst();
    }
    User user = new User(publicId, nickname, profileImage);
    entityManager.persist(user);
    return user;
  }

  private static byte[] solidPng(Color color) {
    BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = image.createGraphics();
    g.setColor(color);
    g.fillRect(0, 0, 256, 256);
    g.dispose();
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      ImageIO.write(image, "png", out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}

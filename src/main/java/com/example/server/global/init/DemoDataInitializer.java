package com.example.server.global.init;

import com.example.server.domain.image.entity.Image;
import com.example.server.domain.image.repository.ImageRepository;
import com.example.server.domain.routine.entity.Routine;
import com.example.server.domain.routine.entity.RoutineCard;
import com.example.server.domain.routine.entity.RoutineComment;
import com.example.server.domain.routine.repository.RoutineCardRepository;
import com.example.server.domain.routine.repository.RoutineCommentRepository;
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
  private final EntityManager entityManager;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    Image image1 = defaultImage(DemoData.IMAGE_ID, new Color(0xFF, 0xB4, 0x7A));
    Image image2 = defaultImage(DemoData.IMAGE_ID_2, new Color(0x8E, 0xC5, 0xFC));
    Image image3 = defaultImage(DemoData.IMAGE_ID_3, new Color(0xA7, 0xE3, 0xB5));

    User user = demoUser(image1);

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
          new Routine(DemoData.DELETABLE_ROUTINE_ID, user, "삭제 테스트용 루틴", "Swagger에서 삭제를 시험해 보세요. 1분 안에 자동 복구됩니다."));
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
    restoreDeletedDemoData(); // 재배포 직후에도 바로 복구
  }

  /**
   * 심사자가 Swagger 예시로 데모 루틴 · 댓글을 삭제해도, 1분 안에 되살려서
   * 다음 사람이 수정 · 삭제 예시를 실행해도 404가 나지 않게 한다.
   */
  @Scheduled(initialDelayString = "${app.demo.restore-interval:60s}", fixedDelayString = "${app.demo.restore-interval:60s}")
  @Transactional
  public void restoreDeletedDemoData() {
    for (String publicId : List.of(DemoData.ROUTINE_ID, DemoData.EDITABLE_ROUTINE_ID, DemoData.DELETABLE_ROUTINE_ID)) {
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
  }

  private Image defaultImage(String publicId, Color color) {
    return imageRepository.findByPublicIdAndDeletedAtIsNull(publicId)
        .orElseGet(() -> imageRepository.save(new Image(publicId, "image/png", solidPng(color), null)));
  }

  private User demoUser(Image profileImage) {
    List<User> users = entityManager
        .createQuery("from AppUser u where u.publicId = :publicId", User.class)
        .setParameter("publicId", DemoData.USER_ID)
        .getResultList();
    if (!users.isEmpty()) {
      return users.getFirst();
    }
    User user = new User(DemoData.USER_ID, DemoData.USER_DEVICE_TOKEN, "데모 유저", profileImage);
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

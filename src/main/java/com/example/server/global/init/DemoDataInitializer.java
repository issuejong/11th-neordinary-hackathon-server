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
import com.example.server.domain.routine.entity.RoutineLike;
import com.example.server.domain.routine.entity.RoutineParticipation;
import com.example.server.domain.routine.repository.RoutineCardRepository;
import com.example.server.domain.routine.repository.RoutineCommentRepository;
import com.example.server.domain.routine.repository.RoutineLikeRepository;
import com.example.server.domain.routine.repository.RoutineParticipationRepository;
import com.example.server.domain.routine.repository.RoutineRepository;
import com.example.server.domain.user.entity.User;
import com.example.server.global.init.DemoIllustrations.Theme;
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
  private final RoutineLikeRepository likeRepository;
  private final TestAccountLeaseRepository leaseRepository;
  private final ObjectMapper json;
  private final EntityManager entityManager;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    Image image1 = defaultImage(DemoData.IMAGE_ID, new Color(0xFF, 0xB4, 0x7A));
    Image image2 = defaultImage(DemoData.IMAGE_ID_2, new Color(0x8E, 0xC5, 0xFC));
    Image image3 = defaultImage(DemoData.IMAGE_ID_3, new Color(0xA7, 0xE3, 0xB5));
    for (Theme theme : Theme.values()) {
      photo(theme);
    }

    User user = demoUser(DemoData.USER_ID, DemoData.USER_NICKNAME, image1);
    User runner = demoUser(DemoData.PARTICIPANT_ID, "러너지수", image2);
    User bookworm = demoUser(DemoData.AUTHOR_ID, "책읽는곰", image3);
    User coffee = demoUser(DemoData.COFFEE_USER_ID, "모닝커피", image1);
    User yogi = demoUser(DemoData.YOGA_USER_ID, "요가하는민지", image2);
    registerReviewAccount(user);

    // 데모 유저(8번 심사용) 루틴: 상세 · 수정 · 삭제 예시
    syncRoutine(DemoData.ROUTINE_ID, user, "☀️ 미라클 모닝 루틴", "출근 전 1시간, 나만을 위한 시간으로 하루를 시작해요.", List.of(
        card(Theme.SUNRISE, "알람 없이 6시에 일어나기", "커튼부터 열고 햇빛 보기", 6, 0),
        card(Theme.WATER, "미지근한 물 한 잔 마시기", "자는 동안 빠진 수분 채우기", 6, 5),
        card(Theme.STRETCH, "전신 스트레칭 10분", "목 · 어깨 · 허리 순서로 천천히", 6, 10),
        card(Theme.NOTEBOOK, "오늘 할 일 세 가지 적기", "가장 중요한 일을 맨 위에", 6, 30)));
    syncRoutine(DemoData.EDITABLE_ROUTINE_ID, user, "🏃 퇴근 후 30분 러닝", "하루 스트레스를 한강 바람에 날려 보내요.", List.of(
        card(Theme.RUNNING, "5분 걷기로 몸 풀기", "발목 · 무릎 가볍게 돌리기", 19, 30),
        card(Theme.RUNNING, "3km 천천히 달리기", "대화할 수 있는 속도로", 19, 35)));
    syncRoutine(DemoData.DELETABLE_ROUTINE_ID, user, "🌙 꿀잠 수면 루틴", "자기 전 1시간은 화면 대신 나에게 집중해요.", List.of(
        card(Theme.NIGHT, "23시에 휴대폰 내려놓기", "충전기는 침대에서 먼 곳에", 23, 0),
        card(Theme.BOOK, "책 10쪽 읽고 불 끄기", null, 23, 10)));

    // 책읽는곰 루틴: 함께하기(참여 · 초안 · 완료 · 결과) 예시 — 결과 관련은 스텝 1개(예시 본문과 같은 수)
    syncRoutine(DemoData.JOIN_ROUTINE_ID, bookworm, "📚 자기 전 독서 20분", "하루 20분, 한 달이면 책 두 권을 읽어요.", List.of(
        card(Theme.BOOK, "읽고 싶던 책 20분 읽기", "종이책이면 더 좋아요", 22, 30),
        card(Theme.NOTEBOOK, "마음에 든 문장 한 줄 기록", null, 22, 50)));
    syncRoutine(DemoData.DRAFT_ROUTINE_ID, bookworm, "🚶 점심 먹고 10분 산책", "식곤증을 걷기로 이겨내요.", List.of(
        card(Theme.SUNRISE, "회사 근처 한 바퀴 걷기", "계단으로 내려가기", 12, 40)));
    syncRoutine(DemoData.COMPLETE_ROUTINE_ID, bookworm, "💧 하루 물 2L 마시기", "텀블러 하나로 시작하는 수분 습관", List.of(
        card(Theme.WATER, "500ml 텀블러 네 번 비우기", "알림을 2시간마다 맞춰두기", 9, 0)));
    syncRoutine(DemoData.RESULT_EDIT_ROUTINE_ID, bookworm, "🍵 커피 대신 따뜻한 차", "오후 커피를 허브티로 바꿔봐요.", List.of(
        card(Theme.COFFEE, "오후 3시 허브티 한 잔", "캐모마일이나 페퍼민트", 15, 0)));
    syncRoutine(DemoData.RESULT_DELETE_ROUTINE_ID, bookworm, "🧘 아침 요가 15분", "굳은 몸을 깨우는 가벼운 요가", List.of(
        card(Theme.STRETCH, "고양이 자세와 아기 자세", "호흡에 맞춰 천천히", 7, 0)));

    // 목록을 실제처럼 채우는 인기 루틴
    syncRoutine(DemoData.CAFFEINE_ROUTINE_ID, coffee, "☕ 카페인 줄이기 2주 챌린지", "하루 세 잔에서 한 잔으로, 2주면 충분해요.", List.of(
        card(Theme.COFFEE, "아침 커피는 한 잔만", "오후엔 디카페인으로", 8, 30),
        card(Theme.WATER, "커피 생각날 때 물 한 잔", null, 14, 0)));
    syncRoutine(DemoData.YOGA_ROUTINE_ID, yogi, "🧘 저녁 요가로 하루 마무리", "20분이면 하루 피로가 풀려요.", List.of(
        card(Theme.STRETCH, "골반 열기 스트레칭", "무리하지 않는 범위까지", 21, 0),
        card(Theme.NIGHT, "누워서 5분 호흡 명상", "4초 들이쉬고 6초 내쉬기", 21, 20)));
    syncRoutine(DemoData.RUNNING_ROUTINE_ID, runner, "🏃 주 3회 5km 러닝 습관", "월 · 수 · 금, 꾸준함이 속도를 이겨요.", List.of(
        card(Theme.RUNNING, "5km 내 페이스로 달리기", "첫 1km는 천천히", 7, 0),
        card(Theme.WATER, "달린 뒤 물 한 잔과 쿨다운", null, 7, 40)));

    // 댓글 (CMT001 · CMT002는 Swagger 수정 · 삭제 예시라 데모 유저가 작성)
    syncComment(DemoData.COMMENT_ID, DemoData.ROUTINE_ID, user, "내일부터 6시 기상 도전합니다! 💪");
    syncComment(DemoData.DELETABLE_COMMENT_ID, DemoData.ROUTINE_ID, user, "스트레칭 순서 꿀팁이네요");
    syncComment(DemoData.COMMENT_ID_3, DemoData.ROUTINE_ID, coffee, "3일째 하고 있는데 아침이 확실히 여유로워요 ☀️");
    syncComment(DemoData.COMMENT_ID_4, DemoData.ROUTINE_ID, yogi, "물 한 잔이 생각보다 효과가 커요!");
    syncComment(DemoData.COMMENT_ID_5, DemoData.RUNNING_ROUTINE_ID, coffee, "페이스 몇으로 뛰세요? 저도 같이 해볼게요");

    // 좋아요 · 참여 (좋아요순 · 참여순 정렬이 의미 있게 보이도록)
    for (User liker : List.of(runner, bookworm, coffee, yogi)) {
      like(DemoData.ROUTINE_ID, liker);
    }
    for (User liker : List.of(user, coffee, yogi)) {
      like(DemoData.RUNNING_ROUTINE_ID, liker);
    }
    like(DemoData.CAFFEINE_ROUTINE_ID, yogi);
    like(DemoData.CAFFEINE_ROUTINE_ID, runner);
    like(DemoData.YOGA_ROUTINE_ID, coffee);
    like(DemoData.JOIN_ROUTINE_ID, yogi);
    inProgress(DemoData.ROUTINE_ID, coffee);
    inProgress(DemoData.ROUTINE_ID, yogi);
    inProgress(DemoData.RUNNING_ROUTINE_ID, coffee);
    inProgress(DemoData.YOGA_ROUTINE_ID, bookworm);
    restoreDeletedDemoData(); // 재배포 직후에도 바로 복구
  }

  /** 데모 유저를 심사용 테스트 계정(8번 슬롯)으로 등록한다. 이미 있으면 건너뛴다. */
  private void registerReviewAccount(User user) {
    if (!leaseRepository.existsBySlot(DemoData.REVIEW_SLOT) && !leaseRepository.existsByUser_Id(user.getId())) {
      leaseRepository.save(new TestAccountLease(DemoData.REVIEW_SLOT, user));
      log.info("Review test account registered: slot {}", DemoData.REVIEW_SLOT);
    }
  }

  private record CardSpec(Theme theme, String title, String content, LocalTime time) {
  }

  private static CardSpec card(Theme theme, String title, String content, int hour, int minute) {
    return new CardSpec(theme, title, content, LocalTime.of(hour, minute));
  }

  /** 없으면 만들고, 내용(제목 · 소개 · 스텝)이 다르면 이 내용으로 맞춘다. 고정 publicId라 Swagger 예시는 그대로 동작한다. */
  private void syncRoutine(String publicId, User owner, String title, String content, List<CardSpec> cards) {
    Routine routine = routineRepository.findByPublicId(publicId).orElse(null);
    if (routine == null) {
      routine = routineRepository.save(new Routine(publicId, owner, title, content));
    } else if (!title.equals(routine.getTitle()) || !java.util.Objects.equals(content, routine.getContent())) {
      routine.update(title, content, owner.getId());
    }
    List<RoutineCard> existing = cardRepository.findAllByRoutine_IdOrderBySortOrderAsc(routine.getId());
    boolean same = existing.size() == cards.size();
    for (int i = 0; same && i < cards.size(); i++) {
      RoutineCard e = existing.get(i);
      CardSpec c = cards.get(i);
      same = e.getTitle().equals(c.title()) && java.util.Objects.equals(e.getContent(), c.content())
          && c.time().equals(e.getCardTime()) && photoId(c.theme()).equals(e.getImage().getPublicId());
    }
    if (!same) {
      cardRepository.deleteAll(existing);
      cardRepository.flush(); // (routine_id, sort_order) 유니크 충돌 방지
      for (int i = 0; i < cards.size(); i++) {
        CardSpec c = cards.get(i);
        cardRepository.save(new RoutineCard(Ulid.generate(), routine, i + 1, photo(c.theme()), c.title(), c.content(), c.time()));
      }
      log.info("Demo routine synced: {} {}", publicId, title);
    }
  }

  private void syncComment(String publicId, String routineId, User author, String content) {
    RoutineComment comment = commentRepository.findByPublicId(publicId).orElse(null);
    if (comment == null) {
      routineRepository.findByPublicIdAndDeletedAtIsNull(routineId)
          .ifPresent(routine -> commentRepository.save(new RoutineComment(publicId, routine, author, content)));
    } else if (!content.equals(comment.getContent())) {
      comment.edit(content);
    }
  }

  private void like(String routineId, User user) {
    routineRepository.findByPublicIdAndDeletedAtIsNull(routineId).ifPresent(routine -> likeRepository
        .findByRoutine_IdAndUser_Id(routine.getId(), user.getId())
        .ifPresentOrElse(like -> {
          if (like.getDeletedAt() != null) {
            like.likeAgain();
          }
        }, () -> likeRepository.save(new RoutineLike(routine, user))));
  }

  private static String photoId(Theme theme) {
    return switch (theme) {
      case SUNRISE -> DemoData.PHOTO_SUNRISE;
      case WATER -> DemoData.PHOTO_WATER;
      case STRETCH -> DemoData.PHOTO_STRETCH;
      case NOTEBOOK -> DemoData.PHOTO_NOTEBOOK;
      case RUNNING -> DemoData.PHOTO_RUNNING;
      case BOOK -> DemoData.PHOTO_BOOK;
      case NIGHT -> DemoData.PHOTO_NIGHT;
      case COFFEE -> DemoData.PHOTO_COFFEE;
    };
  }

  /** 루틴 일러스트 이미지 (시스템 등록). 없으면 그려서 저장한다. */
  private Image photo(Theme theme) {
    String publicId = photoId(theme);
    return imageRepository.findByPublicIdAndDeletedAtIsNull(publicId)
        .orElseGet(() -> imageRepository.save(new Image(publicId, "image/png", DemoIllustrations.png(theme), null)));
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
        DemoData.RESULT_DELETE_ROUTINE_ID, DemoData.CAFFEINE_ROUTINE_ID, DemoData.YOGA_ROUTINE_ID,
        DemoData.RUNNING_ROUTINE_ID)) {
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
    published(DemoData.RESULT_EDIT_ROUTINE_ID, user, new Draft("나만의 오후 티타임", "커피 대신 차를 마시니 저녁에 잠이 잘 와요.",
        List.of(new DraftCard(DemoData.PHOTO_COFFEE, "오후 3시 루이보스티 한 잔", "꿀 한 스푼 넣어서", "15:00"))));
    published(DemoData.RESULT_DELETE_ROUTINE_ID, user, new Draft("출근 전 요가 15분", "알람보다 10분 일찍 일어나서 해봤어요.",
        List.of(new DraftCard(DemoData.PHOTO_STRETCH, "고양이 자세 5번 반복", null, "06:50"))));
    published(DemoData.ROUTINE_ID, participant, new Draft("러너의 미라클 모닝", "스트레칭 대신 가벼운 조깅을 넣어봤어요.", List.of(
        new DraftCard(DemoData.PHOTO_SUNRISE, "5시 50분 기상", "알람은 한 번만", "05:50"),
        new DraftCard(DemoData.PHOTO_WATER, "물 한 잔 마시기", null, "05:55"),
        new DraftCard(DemoData.PHOTO_RUNNING, "동네 2km 조깅", "숨이 찰 듯 말 듯", "06:05"),
        new DraftCard(DemoData.PHOTO_NOTEBOOK, "오늘 할 일 세 가지 적기", null, "06:40"))));
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

  private void published(String routineId, User user, Draft work) {
    RoutineParticipation participation = participation(routineId, user);
    if (participation == null) {
      return;
    }
    String workJson = json.writeValueAsString(work);
    if (participation.hasPublishedWork()) {
      if (!workJson.equals(participation.getWorkJson())) {
        participation.saveWork(workJson); // 예시 수정으로 바뀐 결과를 원래 내용으로
      }
      return;
    }
    if (participation.getDeletedAt() != null) {
      participation.joinAgain(LocalDateTime.now());
    }
    participation.saveWork(workJson);
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
      User existing = users.getFirst();
      if (!nickname.equals(existing.getNickname()) && !DemoData.USER_ID.equals(publicId)) {
        existing.changeNickname(nickname); // 예전 데모 닉네임을 실제 같은 닉네임으로 (심사용 데모 유저는 제외)
      }
      return existing;
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

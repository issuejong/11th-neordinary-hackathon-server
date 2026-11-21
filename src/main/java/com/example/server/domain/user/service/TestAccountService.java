package com.example.server.domain.user.service;

import com.example.server.domain.user.dto.TestAccountRequestDTO;
import com.example.server.domain.user.dto.TestAccountResponseDTO;
import com.example.server.domain.user.dto.UserResponseDTO;
import com.example.server.domain.user.entity.TestAccountLease;
import com.example.server.domain.user.repository.TestAccountLeaseRepository;
import com.example.server.domain.user.repository.UserRepository;
import com.example.server.domain.user.service.command.UserCommandService;
import com.example.server.domain.user.support.TestAccountProperties;
import com.example.server.domain.user.support.TestAccountTokens;
import com.example.server.global.exception.BusinessException;
import com.example.server.global.response.code.status.ErrorStatus;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TestAccountService {

  private final TestAccountLeaseRepository leases;
  private final UserRepository users;
  private final UserCommandService userCommands;
  private final TestAccountProperties properties;

  @Transactional(readOnly = true)
  public List<TestAccountResponseDTO.Account> list() {
    Instant now = Instant.now();
    return leases.findVisibleAccounts(TestAccountRequestDTO.MAX_SLOTS).stream()
        .map(account -> new TestAccountResponseDTO.Account(account.slot(), account.publicId(), account.nickname(),
            account.profileImagePublicId(), account.expiresAt() == null || !account.expiresAt().isAfter(now)))
        .toList();
  }

  @Transactional
  public TestAccountResponseDTO.Account register(TestAccountRequestDTO.Register request) {
    if (leases.existsBySlot(request.slot())) {
      throw new BusinessException(ErrorStatus.TEST_SLOT_OCCUPIED);
    }
    UserResponseDTO profile = userCommands.create(request.nickname(), request.bio());
    Long userId = users.findActiveIdByPublicId(profile.publicId())
        .orElseThrow(() -> new BusinessException(ErrorStatus.USER_NOT_FOUND));
    try {
      leases.saveAndFlush(new TestAccountLease(request.slot(), users.getReferenceById(userId)));
    } catch (DataIntegrityViolationException exception) {
      throw new BusinessException(ErrorStatus.TEST_SLOT_OCCUPIED);
    }
    return new TestAccountResponseDTO.Account(request.slot(), profile.publicId(), profile.nickname(),
        profile.profileImagePublicId(), true);
  }

  @Transactional
  public TestAccountResponseDTO.Claim claim(String publicId) {
    TestAccountLease lease = lockAccount(publicId);
    Instant now = Instant.now();
    if (lease.isActiveAt(now)) {
      throw new BusinessException(ErrorStatus.TEST_ACCOUNT_IN_USE);
    }
    String token = TestAccountTokens.generate();
    Instant expiresAt = now.plus(properties.leaseDuration());
    lease.claim(TestAccountTokens.hash(token), expiresAt);
    return new TestAccountResponseDTO.Claim(publicId, token, expiresAt);
  }

  @Transactional
  public TestAccountResponseDTO.Heartbeat heartbeat(String publicId, String authorization) {
    TestAccountLease lease = lockAccount(publicId);
    Instant now = Instant.now();
    checkOwner(lease, authorization, now);
    Instant expiresAt = now.plus(properties.leaseDuration());
    lease.renew(expiresAt);
    return new TestAccountResponseDTO.Heartbeat(expiresAt);
  }

  @Transactional
  public void release(String publicId, String authorization) {
    TestAccountLease lease = lockAccount(publicId);
    checkOwner(lease, authorization, Instant.now());
    lease.release();
  }

  private TestAccountLease lockAccount(String publicId) {
    return leases.findVisibleAccountForUpdate(publicId, TestAccountRequestDTO.MAX_SLOTS)
        .orElseThrow(() -> new BusinessException(ErrorStatus.TEST_ACCOUNT_NOT_FOUND));
  }

  private void checkOwner(TestAccountLease lease, String authorization, Instant now) {
    String tokenHash = TestAccountTokens.hashAuthorization(authorization);
    if (!lease.isActiveAt(now) || !tokenHash.equals(lease.getTokenHash())) {
      throw new BusinessException(ErrorStatus.TEST_SESSION_INVALID);
    }
  }
}

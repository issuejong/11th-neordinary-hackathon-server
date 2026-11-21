package com.example.server.support;

import com.example.server.domain.user.entity.TestAccountLease;
import com.example.server.domain.user.repository.TestAccountLeaseRepository;
import com.example.server.domain.user.repository.UserRepository;
import com.example.server.domain.user.support.TestAccountTokens;
import com.example.server.global.util.Ulid;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.transaction.support.TransactionTemplate;

@TestComponent
public class TestSessions {

  private static final AtomicInteger NEXT_SLOT = new AtomicInteger(100);

  private final TestAccountLeaseRepository leases;
  private final UserRepository users;
  private final TransactionTemplate tx;

  public TestSessions(TestAccountLeaseRepository leases, UserRepository users, TransactionTemplate tx) {
    this.leases = leases;
    this.users = users;
    this.tx = tx;
  }

  public synchronized String authorization(String publicId) {
    if (!publicId.matches(Ulid.REGEX)) {
      return "Bearer invalid";
    }
    String token = publicId + "A".repeat(17);
    tx.executeWithoutResult(status -> users.findActiveIdByPublicId(publicId).ifPresent(userId -> {
      if (leases.findByUser_PublicId(publicId).isEmpty()) {
        TestAccountLease lease = new TestAccountLease(NEXT_SLOT.getAndIncrement(), users.getReferenceById(userId));
        lease.claim(TestAccountTokens.hash(token), Instant.now().plusSeconds(3600));
        leases.save(lease);
      }
    }));
    return "Bearer " + token;
  }
}

package com.example.server.domain.user.repository;

import com.example.server.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

  @Query("select u.id from AppUser u where u.deviceToken = :deviceToken and u.deletedAt is null")
  Optional<Long> findActiveIdByDeviceToken(@Param("deviceToken") String deviceToken);
}

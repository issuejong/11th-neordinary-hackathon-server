package com.example.server.domain.user.repository;

import com.example.server.domain.user.dto.UserResponseDTO;
import com.example.server.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

  @Query("select u.id from AppUser u where u.deviceToken = :deviceToken and u.deletedAt is null")
  Optional<Long> findActiveIdByDeviceToken(@Param("deviceToken") String deviceToken);

  boolean existsByDeviceToken(String deviceToken);

  @Query("""
      select new com.example.server.domain.user.dto.UserResponseDTO(u.publicId, u.nickname, i.publicId, u.bio)
      from AppUser u join u.profileImage i
      where u.deviceToken = :deviceToken and u.deletedAt is null
      """)
  List<UserResponseDTO> findActiveProfilesByDeviceToken(@Param("deviceToken") String deviceToken);

  @Query("""
      select new com.example.server.domain.user.dto.UserResponseDTO(u.publicId, u.nickname, i.publicId, u.bio)
      from AppUser u join u.profileImage i
      where u.id = :userId and u.deletedAt is null
      """)
  Optional<UserResponseDTO> findActiveProfileById(@Param("userId") Long userId);
}

package hud.SpringSecurityTemplate.repositories;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import hud.SpringSecurityTemplate.models.User;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(@NotBlank String email);

    Boolean existsByEmail(@NotBlank String email);

    Boolean existsByPhoneNumber(@NotBlank String phone);
    Optional<User> findByPasswordResetToken(@NotBlank String token);
    Optional<User> findByRefreshToken(@NotBlank String token);

    Optional<User> findByOauthExchangeCodeAndOauthExchangeCodeExpirationAfter(String otp, LocalDateTime now);

    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.oauthExchangeCode = null WHERE u.id = :id AND u.oauthExchangeCode = :oauthExchangeCode")
    int invalidateCode(@Param("id") Long id, @Param("oauthExchangeCode") String oauthExchangeCode);

}


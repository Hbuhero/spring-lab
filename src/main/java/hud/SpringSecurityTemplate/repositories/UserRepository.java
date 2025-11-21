package hud.SpringSecurityTemplate.repositories;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import hud.SpringSecurityTemplate.models.User;

import java.util.List;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(@NotBlank String email);

    Boolean existsByEmail(@NotBlank String email);

    Boolean existsByPhoneNumber(@NotBlank String phone);
    Optional<User> findByPasswordResetToken(@NotBlank String token);
    Optional<User> findByRefreshToken(@NotBlank String token);

}
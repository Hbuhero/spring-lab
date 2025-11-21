package hud.SpringSecurityTemplate.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import hud.SpringSecurityTemplate.models.User;
import hud.SpringSecurityTemplate.repositories.UserRepository;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Cacheable(value = "user-by-email", key = "#email")
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Cacheable(value = "users", key = "#id")
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

//    @Cacheable(value = "user-by-amcos", key = "#amcos")
//    public List<User> findByAmcos(Long amcos) {
//        return userRepository.findByAmcos(amcos);
//    }
//
//    @Cacheable(value = "user-by-mcu", key = "#mcu")
//    public List<User> findByMcu(Long mcu) {
//        return userRepository.findByMcu(mcu);
//    }
//
//    @Cacheable(value = "user-by-amcos", key = "#amcos + '-page-' + #pageable.pageNumber")
//    public Page<User> findByAmcosPageable(Long amcos, Pageable pageable) {
//        return userRepository.findByAmcosPageable(amcos, pageable);
//    }
    @CachePut(value = "users", key = "#user.id")
    @CacheEvict(value = {"user-by-email", "user-by-amcos", "user-by-mcu"}, allEntries = true)
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @CacheEvict(value = {"users", "user-by-email", "user-by-amcos", "user-by-mcu"}, 
                key = "#id", beforeInvocation = true)
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @CachePut(value = "users", key = "#user.id")
    @CacheEvict(value = {"user-by-email", "user-by-amcos", "user-by-mcu"}, allEntries = true)
    public User updateUser(User user) {
        return userRepository.save(user);
    }

//    // Non-cached methods for operations that shouldn't be cached
//    public Optional<User> findByRefreshToken(String token) {
//        return userRepository.findByRefreshToken(token, LocalDateTime.now());
//    }
//
//    public Optional<User> findByPasswordResetToken(String token) {
//        return userRepository.findByPasswordResetToken(token, LocalDateTime.now());
//    }

    public Boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public Boolean existsByPhoneNumber(String phone) {
        return userRepository.existsByPhoneNumber(phone);
    }

//    @Cacheable(value = "user-stats", key = "'count-by-status-' + #status")
//    public Long countByStatus(String status) {
//        return userRepository.countByStatus(status);
//    }
//
//    @Cacheable(value = "user-stats", key = "'count-by-amcos-' + #amcos")
//    public Long countByAmcos(Long amcos) {
//        return userRepository.countByAmcos(amcos);
//    }

    // Cache eviction for administrative operations
    @CacheEvict(value = {"users", "user-by-email", "user-by-amcos", "user-by-mcu", "user-stats"}, 
                allEntries = true)
    public void clearAllCaches() {
        // This method will clear all user-related caches
    }
}

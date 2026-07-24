package hud.SpringSecurityTemplate.controllers;

import hud.SpringSecurityTemplate.models.User;
import hud.SpringSecurityTemplate.models.UserStatus;
import hud.SpringSecurityTemplate.payloads.requests.auth.AdminPasswordResetRequest;
import hud.SpringSecurityTemplate.payloads.requests.user.UserRequest;
import hud.SpringSecurityTemplate.payloads.requests.user.UserRoleRequest;
import hud.SpringSecurityTemplate.payloads.responses.LoginResponse;
import hud.SpringSecurityTemplate.payloads.responses.Message;
import hud.SpringSecurityTemplate.payloads.responses.UserResponse;
import hud.SpringSecurityTemplate.repositories.UserRepository;
import hud.SpringSecurityTemplate.security.CurrentUser;
import hud.SpringSecurityTemplate.security.UserPrincipal;
import hud.SpringSecurityTemplate.services.AuthService;
import hud.SpringSecurityTemplate.services.EmailService;
import hud.SpringSecurityTemplate.utils.Constants;
import hud.SpringSecurityTemplate.utils.PageableConfig;
import hud.SpringSecurityTemplate.utils.PageableParam;
import hud.SpringSecurityTemplate.utils.pagination.PageResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


import java.util.*;
import java.util.stream.Collectors;

import static hud.SpringSecurityTemplate.models.UserStatus.*;

@Controller
@RequestMapping(Constants.API_V1 + "/users")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final PageableConfig pageableConfig;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService, PageableConfig pageableConfig) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.pageableConfig = pageableConfig;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getAllUsers(
            PageableParam pageableParam,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search
    ) {
        try {
            Pageable pageable = pageableConfig.pageable(pageableParam);
            Page<User> userPage = userRepository.findAll(pageable);

            List<LoginResponse.UserResponse> userResponses = userPage.getContent().stream()
                .filter(user -> {
                    boolean matches = true;
                    if (status != null) matches = matches && status.equals(user.getStatus());
                    if (role != null) matches = matches && role.equals(user.getRole());
                    if (search != null && !search.trim().isEmpty()) {
                        String searchLower = search.toLowerCase();
                        matches = matches && (user.getFullName().toLowerCase().contains(searchLower) ||
                                            user.getEmail().toLowerCase().contains(searchLower));
                    }
                    return matches;
                })
                .map(AuthService::getUserInformation)
                .collect(Collectors.toList());

            PageResponse<LoginResponse.UserResponse> pageResponse = new PageResponse<>();
            pageResponse.setPage(new hud.SpringSecurityTemplate.utils.pagination.Page(
                    String.valueOf(userPage.getNumber()),
                    String.valueOf(userPage.getNumberOfElements()),
                    String.valueOf(userPage.getTotalElements()),
                    String.valueOf(userPage.getTotalPages() - 1)));
            if (userPage.isEmpty()) {
                pageResponse.setRecords(new ArrayList<>());
            } else {
                pageResponse.setRecords(userResponses);
            }

            return new ResponseEntity<>(pageResponse, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(new Message("Error retrieving users: " + e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        Optional<User> userOptional = userRepository.findById(id);
        
        if (userOptional.isEmpty()) {
            return new ResponseEntity<>(new Message("User with id " + id + " not found"), 
                HttpStatus.NOT_FOUND);
        }

        LoginResponse.UserResponse userResponse = AuthService.getUserInformation(userOptional.get());
        return new ResponseEntity<>(userResponse, HttpStatus.OK);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody UserRequest signupRequest) {
        if (signupRequest.getPassword() == null || signupRequest.getPassword().isBlank()) {
            return new ResponseEntity<>(new Message("Password is required"), HttpStatus.BAD_REQUEST);
        }

        // Check if email already exists
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            return new ResponseEntity<>(new Message("Email is already in use"), HttpStatus.BAD_REQUEST);
        }

        // Check if phone number already exists
        if (userRepository.existsByPhoneNumber(signupRequest.getPhoneNumber())) {
            return new ResponseEntity<>(new Message("Phone number is already in use"), HttpStatus.BAD_REQUEST);
        }

        try {
            User user = new User();
            user.setFullName(signupRequest.getName());
            user.setEmail(signupRequest.getEmail());
            user.setPhoneNumber(signupRequest.getPhoneNumber());
            user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
            user.setRole(signupRequest.getRole());

            user.setPermissions(signupRequest.getPermissions());
            user.setStatus(ACTIVE.name());
            user.setProvider("secure system");
            user.setPasswordChanged(false);

            User savedUser = userRepository.save(user);
            LoginResponse.UserResponse userResponse = AuthService.getUserInformation(savedUser);

            return new ResponseEntity<>(userResponse, HttpStatus.CREATED);

        } catch (Exception e) {
            return new ResponseEntity<>(new Message("Error creating user: " + e.getMessage()), 
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<?> updateUser(@PathVariable Long id, 
                                       @Valid @RequestBody UserRequest userRequest,
                                       @CurrentUser UserPrincipal currentUser) {
        Optional<User> userOptional = userRepository.findById(id);
        
        if (userOptional.isEmpty()) {
            return new ResponseEntity<>(new Message("User with id " + id + " not found"), 
                HttpStatus.NOT_FOUND);
        }

        User user = userOptional.get();

        // Check if email is being changed and if it already exists
        if (!user.getEmail().equals(userRequest.getEmail())) {
            if (userRepository.existsByEmail(userRequest.getEmail())) {
                return new ResponseEntity<>(new Message("Email is already in use"), HttpStatus.BAD_REQUEST);
            }
        }

        // Check if phone number is being changed and if it already exists
        if (!user.getPhoneNumber().equals(userRequest.getPhoneNumber())) {
            if (userRepository.existsByPhoneNumber(userRequest.getPhoneNumber())) {
                return new ResponseEntity<>(new Message("Phone number is already in use"), HttpStatus.BAD_REQUEST);
            }
        }

        try {
            user.setFullName(userRequest.getName());
            user.setEmail(userRequest.getEmail());
            user.setPhoneNumber(userRequest.getPhoneNumber());
            
            // Only admins can change role and organizational data
            if (currentUser.getRole().equals("ADMIN") || currentUser.getRole().equals("SUPER_ADMIN")) {
                user.setRole(userRequest.getRole());
                user.setPermissions(userRequest.getPermissions());
            }

            User savedUser = userRepository.save(user);
            LoginResponse.UserResponse userResponse = AuthService.getUserInformation(savedUser);

            return new ResponseEntity<>(userResponse, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(new Message("Error updating user: " + e.getMessage()), 
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, @CurrentUser UserPrincipal currentUser) {
        // Prevent self-deletion
        if (id.equals(currentUser.getId())) {
            return new ResponseEntity<>(new Message("Cannot delete your own account"), HttpStatus.BAD_REQUEST);
        }

        Optional<User> userOptional = userRepository.findById(id);
        
        if (userOptional.isEmpty()) {
            return new ResponseEntity<>(new Message("User with id " + id + " not found"), 
                HttpStatus.NOT_FOUND);
        }

        try {
            userRepository.delete(userOptional.get());
            return new ResponseEntity<>(new Message("User deleted successfully"), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new Message("Error deleting user: " + e.getMessage()), 
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> enableUser(@PathVariable Long id, @CurrentUser UserPrincipal currentUser) {
        return updateUserStatus(id, "ACTIVE", "User enabled by " + currentUser.getEmail());
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> disableUser(@PathVariable Long id, @CurrentUser UserPrincipal currentUser) {
        // Prevent self-disable
        if (id.equals(currentUser.getId())) {
            return new ResponseEntity<>(new Message("Cannot disable your own account"), HttpStatus.BAD_REQUEST);
        }
        
        return updateUserStatus(id, "DISABLED", "User disabled by " + currentUser.getEmail());
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> suspendUser(@PathVariable Long id, @CurrentUser UserPrincipal currentUser) {
        // Prevent self-suspension
        if (id.equals(currentUser.getId())) {
            return new ResponseEntity<>(new Message("Cannot suspend your own account"), HttpStatus.BAD_REQUEST);
        }
        
        return updateUserStatus(id, "SUSPENDED", "User suspended by " + currentUser.getEmail());
    }

    @PostMapping("/admin-reset-password")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> adminResetPassword(@Valid @RequestBody AdminPasswordResetRequest resetRequest) {
        Optional<User> userOptional = userRepository.findById(resetRequest.getUserId());
        
        if (userOptional.isEmpty()) {
            return new ResponseEntity<>(new Message("User with id " + resetRequest.getUserId() + " not found"), 
                HttpStatus.NOT_FOUND);
        }

        try {
            User user = userOptional.get();
            
            // If password is provided, use it; otherwise generate a temporary one
            String newPassword = resetRequest.getNewPassword();
            if (newPassword == null || newPassword.trim().isEmpty()) {
                newPassword = generateTemporaryPassword();
            }

            user.setPassword(passwordEncoder.encode(newPassword));
            user.setPasswordChanged(!resetRequest.getRequirePasswordChange());
            
            // Clear refresh tokens for security
            user.setRefreshToken(null);
            user.setRefreshTokenExpiration(null);
            
            // Clear any existing password reset tokens
            user.setPasswordResetToken(null);
            user.setPasswordResetTokenExpiration(null);

            userRepository.save(user);

            // Send temporary password email if password was generated
            if (resetRequest.getNewPassword() == null || resetRequest.getNewPassword().trim().isEmpty()) {
                try {
                    emailService.sendTemporaryPasswordEmail(user.getEmail(), user.getFullName(), newPassword);
                } catch (Exception e) {
                    System.err.println("Failed to send temporary password email: " + e.getMessage());
                }
            } else {
                // Send password change notification for admin-set password
                try {
                    emailService.sendPasswordChangeConfirmationEmail(user.getEmail(), user.getFullName());
                } catch (Exception e) {
                    System.err.println("Failed to send password change notification email: " + e.getMessage());
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Password reset successfully by admin");
            response.put("userId", user.getId());
            response.put("requirePasswordChange", resetRequest.getRequirePasswordChange());
            if (resetRequest.getNewPassword() == null) {
                response.put("temporaryPassword", newPassword);
                response.put("note", "This is a temporary password. User must change it on first login.");
            }

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(new Message("Error resetting password: " + e.getMessage()), 
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/change-role")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> changeUserRole(@Valid @RequestBody UserRoleRequest roleRequest) {
        Optional<User> userOptional = userRepository.findById(roleRequest.getUserId());
        
        if (userOptional.isEmpty()) {
            return new ResponseEntity<>(new Message("User with id " + roleRequest.getUserId() + " not found"), 
                HttpStatus.NOT_FOUND);
        }

        try {
            User user = userOptional.get();
            user.setRole(roleRequest.getRole());
            if (roleRequest.getPermissions() != null) {
                user.setPermissions(roleRequest.getPermissions());
            }

            // Clear refresh tokens for security when role changes
            user.setRefreshToken(null);
            user.setRefreshTokenExpiration(null);

            User savedUser = userRepository.save(user);
            LoginResponse.UserResponse userResponse = AuthService.getUserInformation(savedUser);

            return new ResponseEntity<>(userResponse, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(new Message("Error changing user role: " + e.getMessage()), 
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getUserStats() {
        try {
            long totalUsers = userRepository.count();
            List<User> allUsers = userRepository.findAll();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", totalUsers);
            stats.put("activeUsers", allUsers.stream().filter(u -> "ACTIVE".equals(u.getStatus())).count());
            stats.put("disabledUsers", allUsers.stream().filter(u -> "DISABLED".equals(u.getStatus())).count());
            stats.put("suspendedUsers", allUsers.stream().filter(u -> "SUSPENDED".equals(u.getStatus())).count());
            
            // Role-based stats
            Map<String, Long> roleStats = new HashMap<>();
            roleStats.put("ADMIN", allUsers.stream().filter(u -> "ADMIN".equals(u.getRole())).count());
            roleStats.put("USER", allUsers.stream().filter(u -> "USER".equals(u.getRole())).count());
            roleStats.put("MANAGER", allUsers.stream().filter(u -> "MANAGER".equals(u.getRole())).count());
            stats.put("roleStats", roleStats);

            return new ResponseEntity<>(stats, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new Message("Error retrieving user stats: " + e.getMessage()), 
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Helper methods
    private ResponseEntity<?> updateUserStatus(Long userId, String status, String reason) {
        Optional<User> userOptional = userRepository.findById(userId);
        
        if (userOptional.isEmpty()) {
            return new ResponseEntity<>(new Message("User with id " + userId + " not found"), 
                HttpStatus.NOT_FOUND);
        }

        try {
            User user = userOptional.get();
            user.setStatus(status);

            // If disabling or suspending, clear refresh tokens
            if ("DISABLED".equals(status) || "SUSPENDED".equals(status)) {
                user.setRefreshToken(null);
                user.setRefreshTokenExpiration(null);
            }

            User savedUser = userRepository.save(user);
            
            // Send account status change email
            try {
                emailService.sendAccountStatusEmail(savedUser.getEmail(), user.getFullName(), status, reason);
            } catch (Exception e) {
                System.err.println("Failed to send account status email: " + e.getMessage());
            }
            
            LoginResponse.UserResponse userResponse = AuthService.getUserInformation(savedUser);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User status updated successfully");
            response.put("user", userResponse);
            response.put("reason", reason);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(new Message("Error updating user status: " + e.getMessage()), 
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String generateTemporaryPassword() {
        // Generate a secure random temporary password
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        Random random = new Random();
        StringBuilder password = new StringBuilder();
        
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return password.toString();
    }
}

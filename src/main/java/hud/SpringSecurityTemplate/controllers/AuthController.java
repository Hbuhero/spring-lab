package hud.SpringSecurityTemplate.controllers;

import hud.SpringSecurityTemplate.models.User;
import hud.SpringSecurityTemplate.models.UserStatus;
import hud.SpringSecurityTemplate.payloads.requests.auth.*;
import hud.SpringSecurityTemplate.payloads.requests.user.UserRequest;
import hud.SpringSecurityTemplate.payloads.responses.*;
import hud.SpringSecurityTemplate.repositories.UserRepository;
import hud.SpringSecurityTemplate.security.CurrentUser;
import hud.SpringSecurityTemplate.security.JwtProvider;
import hud.SpringSecurityTemplate.security.UserPrincipal;
import hud.SpringSecurityTemplate.services.EmailService;
import hud.SpringSecurityTemplate.utils.Constants;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping(Constants.API_V1 + "/auth")
public class AuthController {

    static Logger logger = LogManager.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;


    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private EmailService emailService;

    private static LoginResponse.UserResponse getUserInformation(User user) {
        LoginResponse.UserResponse userResponse = new LoginResponse.UserResponse();
        userResponse.setId(user.getId());
        userResponse.setName(user.getFullName());
        userResponse.setEmail(user.getEmail());
        userResponse.setPhoneNumber(user.getPhoneNumber());
        userResponse.setRole(user.getRole());
        userResponse.setPasswordReset(user.getPasswordChanged());
        userResponse.setCreatedAt(user.getCreatedAt());
        userResponse.setStatus(user.getStatus().name());
        return userResponse;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // Check if user exists
            Optional<User> userOptional = userRepository.findByEmail(loginRequest.getEmail());
            if (userOptional.isEmpty()) {
                return new ResponseEntity<>(new Message("User not found with email: " + loginRequest.getEmail()), HttpStatus.BAD_REQUEST);
            }

            User user = userOptional.get();

            // Check if user is active
            if (!"ACTIVE".equals(user.getStatus().name())) {
                return new ResponseEntity<>(new Message("User account is not active"), HttpStatus.BAD_REQUEST);
            }

            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );

            // Generate JWT token
            JwtResponse jwtResponse = jwtProvider.generateJwtToken(authentication);

            // Generate refresh token
            String refreshToken = UUID.randomUUID().toString();
            user.setRefreshToken(refreshToken);
            user.setRefreshTokenExpiration(LocalDateTime.now().plusDays(30));
            userRepository.save(user);

            return createLoginResponse(user, refreshToken, jwtResponse);

        } catch (BadCredentialsException e) {
            return new ResponseEntity<>(new Message("Invalid email or password"), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            return new ResponseEntity<>(new Message("Authentication failed: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/signup")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRequest signupRequest) {

        if (signupRequest.getPassword() == null || signupRequest.getPassword().isBlank()) {
            return new ResponseEntity<>(new Message("Password cannot be blank"), HttpStatus.BAD_REQUEST);
        }

        // Check if email already exists
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            return new ResponseEntity<>(new Message("Email is already in use"), HttpStatus.BAD_REQUEST);
        }

        // Check if phone number already exists
        if (userRepository.existsByPhoneNumber(signupRequest.getPhoneNumber())) {
            return new ResponseEntity<>(new Message("Phone number is already in use"), HttpStatus.BAD_REQUEST);
        }

        // Create new user
        User user = new User();
        user.setFullName(signupRequest.getName());
        user.setEmail(signupRequest.getEmail());
        user.setPhoneNumber(signupRequest.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        user.setRole("USER");
        user.setStatus(UserStatus.PENDING);
        user.setProvider("secure system");

        user.setPasswordResetToken(UUID.randomUUID().toString());
        user.setPasswordResetTokenExpiration(LocalDateTime.now().plusMinutes(30));

        User savedUser = userRepository.save(user);

        // Send verification email  asynchronously
        try {
            emailService.sendVerificationEmail(savedUser.getEmail(),
                    savedUser.getFullName() != null ? savedUser.getFullName().split(" ")[0] : "User",
                    savedUser.getPasswordResetToken()
            );
        } catch (Exception e) {
            // Log error but don't fail registration
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }

        SignupResponse response = new SignupResponse();
        response.setMessage("User registered successfully");
        response.setEmail(savedUser.getEmail());
        response.setStatus(savedUser.getStatus().name());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/activate-account")
    public ResponseEntity<?> activateAccount(@RequestParam("token") String token) {
        // Check if user exists
        Optional<User> userOptional = userRepository.findByPasswordResetToken(token);

        if (userOptional.isEmpty()) {
            return new ResponseEntity<>(new Message("Invalid account activation token"), HttpStatus.BAD_REQUEST);
        }
        User user = userOptional.get();

        // check if otp is expired
        if (LocalDateTime.now().isAfter(user.getPasswordResetTokenExpiration())) {
            return new ResponseEntity<>(new Message("Token is expired. Please provide a valid token"), HttpStatus.BAD_REQUEST);
        }

        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiration(null);
        user.setStatus(UserStatus.ACTIVE);
        String refreshToken = UUID.randomUUID().toString();
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiration(LocalDateTime.now().plusDays(30));
        userRepository.save(user);

        // Send welcome email  asynchronously
        try {
            emailService.sendWelcomeEmail(user.getEmail(),
                    user.getFullName().split(" ")[0],
                    user.getFullName().split(" ")[1]
            );
        } catch (Exception e) {
            // Log error but don't fail registration
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }

        JwtResponse jwtResponse = jwtProvider.generateJwtTokenByUser(user.getId());

        return createLoginResponse(user, refreshToken, jwtResponse);
    }

    @PostMapping("/resend-verification-email/{email}")
    public ResponseEntity<?> resendVerificationEmail(@PathVariable("email") String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            return new ResponseEntity<>(new Message("User with email " + email + " is not found"), HttpStatus.BAD_REQUEST);
        }

        User user = userOptional.get();

        if (user.getPasswordResetToken() == null) {
            return new ResponseEntity<>(new Message("Activation Request already processed"), HttpStatus.BAD_REQUEST);
        }

        user.setPasswordResetToken(UUID.randomUUID().toString());
        user.setPasswordResetTokenExpiration(LocalDateTime.now().plusMinutes(30));

        User savedUser = userRepository.save(user);

        // Send verification email  asynchronously
        try {
            emailService.sendVerificationEmail(savedUser.getEmail(),
                    savedUser.getFullName() != null ? savedUser.getFullName().split(" ")[0] : "User",
                    savedUser.getPasswordResetToken()
            );
        } catch (Exception e) {
            // Log error but don't fail registration
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }

        return new ResponseEntity<>(new Message("Verification email has been sent"), HttpStatus.OK);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        Optional<User> userOptional = userRepository.findByRefreshToken(refreshTokenRequest.getRefreshToken());

        if (userOptional.isEmpty()) {
            return new ResponseEntity<>(new Message("Invalid refresh token"), HttpStatus.BAD_REQUEST);
        }

        User user = userOptional.get();

        // Check if refresh token is expired
        if (user.getRefreshTokenExpiration().isBefore(LocalDateTime.now())) {
            return new ResponseEntity<>(new Message("Refresh token is expired"), HttpStatus.BAD_REQUEST);
        }

        // Generate new JWT token
        JwtResponse jwtResponse = jwtProvider.generateJwtTokenByUser(user.getId());

        // Update refresh token
        String newRefreshToken = UUID.randomUUID().toString();
        user.setRefreshToken(newRefreshToken);
        user.setRefreshTokenExpiration(LocalDateTime.now().plusDays(30));
        userRepository.save(user);

        return createLoginResponse(user, newRefreshToken, jwtResponse);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody PasswordResetRequest passwordResetRequest) {
        Optional<User> userOptional = userRepository.findByEmail(passwordResetRequest.getEmail());

        if (userOptional.isEmpty()) {
            return new ResponseEntity<>(new Message("User not found with email: " + passwordResetRequest.getEmail()), HttpStatus.BAD_REQUEST);
        }

        User user = userOptional.get();

        // Generate password reset token
        String otp = String.valueOf(new Random().nextInt(100001, 999999));
        user.setOtp(otp);
        user.setOtpExpiration(LocalDateTime.now().plusMinutes(30)); // Token expires in 24 hours
        user.setIsOtpVerified(false);
        userRepository.save(user);

        // Send password reset email asynchronously
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), otp);
        } catch (Exception e) {
            logger.error("Failed to send password reset email: " + e.getMessage());
            return new ResponseEntity<>(new Message("Failed to send password reset email. Please try again later."), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        PasswordResetResponse response = new PasswordResetResponse();
        response.setMessage("Password reset instructions have been sent to your email");
        response.setEmail(passwordResetRequest.getEmail());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/resend-otp/{email}")
    public ResponseEntity<?> resendOtp(@PathVariable("email") String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            return new ResponseEntity<>(new Message("User not found with email: " + email), HttpStatus.BAD_REQUEST);
        }
        User user = userOptional.get();

        if (user.getOtp() == null) {
            return new ResponseEntity<>(new Message("OTP request is already verified."), HttpStatus.BAD_REQUEST);
        }

        user.setOtp(String.valueOf(new Random().nextInt(100001, 999999)));
        user.setOtpExpiration(LocalDateTime.now().plusMinutes(30));
        user.setIsOtpVerified(false);

        User savedUser = userRepository.save(user);
        try {
            emailService.sendVerificationEmail(savedUser.getEmail(),
                    savedUser.getFullName() != null ? savedUser.getFullName().split(" ")[0] : "User",
                    savedUser.getOtp()
            );
        } catch (Exception e) {
            // Log error but don't fail registration
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }

        return new ResponseEntity<>(new Message("OTP sent successfully"), HttpStatus.OK);
    }

    @PostMapping("/validate-otp")
    public ResponseEntity<?> validateOTP (@Valid @RequestBody OTPValidationRequest otpValidationRequest) {
        Optional<User> userOptional = userRepository.findByEmail(otpValidationRequest.getEmail());
        if (userOptional.isEmpty()) {
            return new ResponseEntity<>(new Message("User not found with email: " + otpValidationRequest.getEmail()), HttpStatus.BAD_REQUEST);
        }

        User user = userOptional.get();

        if (user.getIsOtpVerified()) {
            return new ResponseEntity<>(new Message("OTP is already verified."), HttpStatus.BAD_REQUEST);
        }

        if (user.getOtpExpiration() != null && user.getOtpExpiration().isBefore(LocalDateTime.now())) {
            return new ResponseEntity<>(new Message("OTP is expired"), HttpStatus.BAD_REQUEST);
        }

        if (user.getOtp() != null && !user.getOtp().equals(otpValidationRequest.getOtp())) {
            return new ResponseEntity<>(new Message("Invalid OTP."), HttpStatus.BAD_REQUEST);
        }

        user.setOtpExpiration(null);
        user.setOtp(null);
        user.setIsOtpVerified(true);
        userRepository.save(user);
        return new ResponseEntity<>(new Message("OTP has been validated"), HttpStatus.OK);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordResetConfirmRequest passwordResetConfirmRequest) {
        Optional<User> userOptional = userRepository.findByEmail(passwordResetConfirmRequest.getEmail());
        if (userOptional.isEmpty()) {
            return new ResponseEntity<>(new Message("User not found with email: " + passwordResetConfirmRequest.getEmail()), HttpStatus.BAD_REQUEST);
        }

        User user = userOptional.get();

        // Check if token is expired
        if (!user.getIsOtpVerified()) {
            return new ResponseEntity<>(new Message("Invalid Request"), HttpStatus.BAD_REQUEST);
        }

        // Update password
        user.setPassword(passwordEncoder.encode(passwordResetConfirmRequest.getNewPassword()));
        user.setIsOtpVerified(false);

        // Invalidate all refresh tokens for security
        user.setRefreshToken(null);
        user.setRefreshTokenExpiration(null);
        userRepository.save(user);

        // Send password change confirmation email
        try {
            emailService.sendPasswordChangeConfirmationEmail(user.getEmail(), user.getFullName());
        } catch (Exception e) {
            System.err.println("Failed to send password reset confirmation email: " + e.getMessage());
        }

        return new ResponseEntity<>(new Message("Password has been reset successfully"), HttpStatus.OK);
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest changePasswordRequest,
                                            @CurrentUser UserPrincipal currentUser) {
        Optional<User> userOptional = userRepository.findById(currentUser.getId());
        if (userOptional.isEmpty()) {
            return new ResponseEntity<>(new Message("Please provide a valid authorization token"), HttpStatus.BAD_REQUEST);
        }

        User user = userOptional.get();

        // check current password
        if (!passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), user.getPassword())) {
            return new ResponseEntity<>(new Message("Current password is incorrect"), HttpStatus.BAD_REQUEST);
        }

        // Update password
        user.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
        // Invalidate all refresh tokens for security
        user.setRefreshToken(null);
        user.setRefreshTokenExpiration(null);
        userRepository.save(user);

        // Send password change confirmation email
        try {
            emailService.sendPasswordChangeConfirmationEmail(user.getEmail(), user.getFullName());
        } catch (Exception e) {
            System.err.println("Failed to send password change confirmation email: " + e.getMessage());
        }

        return new ResponseEntity<>(new Message("Password changed successfully"), HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CurrentUser UserPrincipal currentUser) {
        Optional<User> userOptional = userRepository.findById(currentUser.getId());

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setRefreshToken(null);
            user.setRefreshTokenExpiration(null);
            userRepository.save(user);
        }

        return new ResponseEntity<>(new Message("User logged out successfully"), HttpStatus.OK);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(@CurrentUser UserPrincipal currentUser) {
        Optional<User> userOptional = userRepository.findById(currentUser.getId());

        if (userOptional.isEmpty()) {
            return new ResponseEntity<>(new Message("User not found"), HttpStatus.BAD_REQUEST);
        }
        User user = userOptional.get();
        return new ResponseEntity<>(getUserInformation(user), HttpStatus.OK);
    }

    @GetMapping("/validate-token")
    public ResponseEntity<?> validateToken(@CurrentUser UserPrincipal currentUser) {
        Map<String, Object> response = new HashMap<>();
        response.put("valid", true);
        response.put("userId", currentUser.getId());
        response.put("email", currentUser.getEmail());
        response.put("role", currentUser.getRole());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private ResponseEntity<?> createLoginResponse(User user, String refreshToken, JwtResponse jwtResponse) {
        LoginResponse response = new LoginResponse();
        response.setUser(getUserInformation(user));
        response.setToken(jwtResponse.getToken());
        response.setTokenExpiration(jwtResponse.getTokenExpiration().toString());
        response.setRefreshToken(refreshToken);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}


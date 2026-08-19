package hud.SpringSecurityTemplate.controller;

import hud.SpringSecurityTemplate.controllers.AuthController;
import hud.SpringSecurityTemplate.models.User;
import hud.SpringSecurityTemplate.models.UserStatus;
import hud.SpringSecurityTemplate.payloads.requests.user.UserRequest;
import hud.SpringSecurityTemplate.payloads.responses.SignupResponse;
import hud.SpringSecurityTemplate.repositories.UserRepository;
import hud.SpringSecurityTemplate.services.EmailService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Unit Test")
public class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthController authController;

    private UserRequest testUserRequest;
    private SignupResponse testSignupResponse;

    @BeforeEach
    void setUp() {
        testUserRequest = UserRequest.builder()
                .name("test")
                .password("test")
                .email("test@gmail.com")
                .phoneNumber("1234567890")
                .build();

        testSignupResponse =  new SignupResponse("User registered successfully", testUserRequest.getEmail(), "PENDING");
    }

    @Nested
    @DisplayName("Create User Test")
    class CreateUserTest {

        @Test
        @DisplayName("Test successful user creation with valid input")
        void testCreateUserSuccessfully() {
            // arrange
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0)); // thenReturn method gives an explicit defined answer

            // act
            var response = authController.registerUser(testUserRequest);

            // assert
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(testSignupResponse, response.getBody());
            verify(passwordEncoder).encode(testUserRequest.getPassword());
            verify(emailService, times(1)).sendVerificationEmail(
                    eq(testUserRequest.getEmail()),
                    eq(testUserRequest.getName() != null ? testUserRequest.getName().split(" ")[0] : "User"),
                    argThat(token -> token != null && !token.isBlank()) // can use anyString() too
            );
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();

            assertEquals(testUserRequest.getName(), savedUser.getFullName());
            assertEquals(testUserRequest.getEmail(), savedUser.getEmail());
            assertEquals(testUserRequest.getPhoneNumber(), savedUser.getPhoneNumber());
            assertEquals("USER", savedUser.getRole());
            assertEquals(UserStatus.PENDING.name(), savedUser.getStatus());
            assertNotNull(savedUser.getPasswordResetToken());
        }
    }
}

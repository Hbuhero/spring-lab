package hud.SpringSecurityTemplate.payloads.responses;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class LoginResponse {

    private UserResponse user;
    private String tokenType="Bearer";
    private String token;
    private String tokenExpiration;
    private String refreshToken;



    @Data
    @NoArgsConstructor
    public static class UserResponse {
        private Long id;
        private String name;
        private String email;
        private String phoneNumber;
        private String role;
        private Boolean passwordReset;
        private LocalDateTime createdAt;
        private String status;
    }

}

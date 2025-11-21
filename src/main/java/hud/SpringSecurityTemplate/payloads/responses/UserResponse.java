package hud.SpringSecurityTemplate.payloads.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private String role;
    private String permissions;
    private String status;
    private Boolean passwordReset;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

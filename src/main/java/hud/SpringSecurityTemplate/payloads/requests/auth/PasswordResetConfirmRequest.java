package hud.SpringSecurityTemplate.payloads.requests.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetConfirmRequest {
    
    @NotBlank
    private String token;
    
    @NotBlank
    @Size(min = 6, max = 100)
    private String newPassword;
}

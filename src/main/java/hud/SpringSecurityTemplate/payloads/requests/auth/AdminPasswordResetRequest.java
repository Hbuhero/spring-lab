package hud.SpringSecurityTemplate.payloads.requests.auth;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminPasswordResetRequest {
    
    @NotNull
    private Long userId;
    
    @Size(min = 6, max = 100)
    private String newPassword;
    
    private Boolean requirePasswordChange = true;
}

package hud.SpringSecurityTemplate.payloads.requests.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserStatusRequest {
    
    @NotNull
    private Long userId;
    
    @NotBlank
    private String status;
    
    private String reason;
}

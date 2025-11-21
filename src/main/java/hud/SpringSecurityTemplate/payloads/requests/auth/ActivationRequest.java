package hud.SpringSecurityTemplate.payloads.requests.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ActivationRequest {

    @Email
    private String email;
    @NotBlank
    private String otp;
}

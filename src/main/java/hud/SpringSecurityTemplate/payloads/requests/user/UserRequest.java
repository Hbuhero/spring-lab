package hud.SpringSecurityTemplate.payloads.requests.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {
    
    private Long id;
    
    @NotBlank
    @Size(min = 2, max = 100)
    private String name;
    
    @NotBlank
    @Email
    private String email;
    
    @NotBlank
    @Size(min = 10, max = 15)
    private String phoneNumber;

    private String password;

    private String role;
    
    private String permissions;

    private String status;

    private String image;
}

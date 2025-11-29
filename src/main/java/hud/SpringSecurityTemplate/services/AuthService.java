package hud.SpringSecurityTemplate.services;

import hud.SpringSecurityTemplate.models.User;
import hud.SpringSecurityTemplate.payloads.responses.JwtResponse;
import hud.SpringSecurityTemplate.payloads.responses.LoginResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    public static LoginResponse.UserResponse getUserInformation(User user) {
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

    public ResponseEntity<?> createLoginResponse(User user, String refreshToken, JwtResponse jwtResponse) {
        LoginResponse response = new LoginResponse();
        response.setUser(getUserInformation(user));
        response.setToken(jwtResponse.getToken());
        response.setTokenExpiration(jwtResponse.getTokenExpiration().toString());
        response.setRefreshToken(refreshToken);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}

package hud.SpringSecurityTemplate.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import hud.SpringSecurityTemplate.models.User;
import hud.SpringSecurityTemplate.models.UserStatus;
import hud.SpringSecurityTemplate.payloads.responses.JwtResponse;
import hud.SpringSecurityTemplate.repositories.UserRepository;
import hud.SpringSecurityTemplate.services.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

//@Component
//public class CustomOAuth2AuthorizationSuccessHandler implements OAuth2AuthorizationSuccessHandler {
//
//    private final Logger logger = LogManager.getLogger(CustomOAuth2AuthorizationSuccessHandler.class);
//    @Override
//    public void onAuthorizationSuccess(OAuth2AuthorizedClient authorizedClient, Authentication principal, Map<String, Object> attributes) {
//        // take user info needed
//        logger.log(Level.INFO, "Authentication successful" + attributes.toString());
//        System.out.println("Authentication successful" + attributes.toString());
//
//
//        // register or update if exist the user account
//
//        // generate the auth token
//
//        // update the security context holder
////        OAuth2AuthenticationToken
//        SecurityContextHolder.getContext().setAuthentication(principal);
////        OAuth2AuthenticationToken authentication = (OAuth2AuthenticationToken) principal;
//
//    }
//}

@Component
public class CustomOAuth2AuthorizationSuccessHandler implements AuthenticationSuccessHandler {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomUserDetailService userDetailsService;

    @Autowired
    private AuthService  authService;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private ObjectMapper mapper;

    private final Logger logger = LogManager.getLogger(CustomOAuth2AuthorizationSuccessHandler.class);

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken oauth2Authentication = (OAuth2AuthenticationToken) authentication;
        var oauth2User = oauth2Authentication.getPrincipal().getAttributes();

        Optional<User> optionalUser = userRepository.findByEmail((String) oauth2User.get("email"));

        User user;

        if (optionalUser.isPresent()) {
            optionalUser.get().setEmail((String) oauth2User.get("email"));
            optionalUser.get().setFullName((String) oauth2User.get("name"));
            optionalUser.get().setPassword((String) oauth2User.get("password"));
            optionalUser.get().setStatus(UserStatus.ACTIVE);
            optionalUser.get().setPhoneNumber((String) oauth2User.get("phone"));
            optionalUser.get().setRefreshToken(UUID.randomUUID().toString());
            optionalUser.get().setRefreshTokenExpiration(LocalDateTime.now().plusMinutes(30));

            user = userRepository.save(optionalUser.get());
        } else {
            user = userRepository.save(
                    User.builder()
                            .provider(oauth2Authentication.getAuthorizedClientRegistrationId())
                            .email((String) oauth2User.get("email"))
                            .fullName((String) oauth2User.get("name"))
                            .phoneNumber((String) oauth2User.get("phone"))
                            .passwordChanged(false)
                            .role("USER")
                            .permissions(null)
                            .status(UserStatus.ACTIVE)
                            .refreshToken(UUID.randomUUID().toString())
                            .refreshTokenExpiration(LocalDateTime.now().plusDays(30))
                            .otp(null)
                            .otpExpiration(null)
                            .passwordResetToken(null)
                            .passwordResetTokenExpiration(null)
                            .build()
            );
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        // generate jwt for user
        JwtResponse jwt = jwtProvider.generateJwtToken(authToken);

        // create response
        var body = authService.createLoginResponse(user, user.getRefreshToken(), jwt);

        // convert to JSON
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        String json = mapper.writeValueAsString(body);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.toString());
        response.getWriter().write(json);

    }

}

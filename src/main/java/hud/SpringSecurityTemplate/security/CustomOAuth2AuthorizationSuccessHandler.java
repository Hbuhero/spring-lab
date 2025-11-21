package hud.SpringSecurityTemplate.security;

import hud.SpringSecurityTemplate.models.User;
import hud.SpringSecurityTemplate.models.UserStatus;
import hud.SpringSecurityTemplate.repositories.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

import java.io.IOException;
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
    private JwtProvider jwtProvider;

    private final Logger logger = LogManager.getLogger(CustomOAuth2AuthorizationSuccessHandler.class);

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        logger.info("Here is the Authentication Success");

        OAuth2AuthenticationToken oauth2Authentication = (OAuth2AuthenticationToken) authentication;
        var oauth2User = oauth2Authentication.getPrincipal().getAttributes();

        Optional<User> optionalUser = userRepository.findByEmail((String) oauth2User.get("email"));

        User user = optionalUser.orElseGet(() -> userRepository.save(
                User.builder()
                        .provider(oauth2Authentication.getAuthorizedClientRegistrationId())
                        .email((String) oauth2User.get("email"))
                        .password((String) oauth2User.get("password"))
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
        ));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        String jwt = jwtProvider.generateJwtToken(authToken).getToken();
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // send correct redirect
        response.sendRedirect("/api/v1/users/" + jwt);
    }

}

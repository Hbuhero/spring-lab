package hud.SpringSecurityTemplate.security.oauth;

import hud.SpringSecurityTemplate.models.User;
import hud.SpringSecurityTemplate.repositories.UserRepository;
import hud.SpringSecurityTemplate.utils.OAuthExchangeCodeUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class CustomOAuth2AuthorizationSuccessHandler implements AuthenticationSuccessHandler {
    private final UserRepository userRepository;

    @Value("${app.oauth2.frontend-redirect-uri}")
    private String frontendRedirectUri;

    private static final Duration CODE_TTL = Duration.ofSeconds(60);

    public CustomOAuth2AuthorizationSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(
            @NonNull HttpServletRequest request, HttpServletResponse response,
            @NonNull Authentication authentication
    ) throws IOException {

        String email = extractEmail((OAuth2AuthenticationToken) authentication);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User must exist after OAuth2UserService processing"));

        String rawCode = OAuthExchangeCodeUtil.generateCode();
        user.setOauthExchangeCode(OAuthExchangeCodeUtil.hash(rawCode));
        user.setOauthExchangeCodeExpiration(LocalDateTime.now().plus(CODE_TTL));
        userRepository.save(user);

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("code", rawCode)
                .build(true)
                .toUriString();

        response.sendRedirect(redirectUrl);
    }

    private String extractEmail(OAuth2AuthenticationToken token) {
        Object principal = token.getPrincipal();
        if (principal instanceof OidcUser oidcUser) return oidcUser.getEmail();
        if (principal instanceof OAuth2User oAuth2User) return (String) oAuth2User.getAttributes().get("email");
        throw new IllegalStateException("Unsupported principal type: " + principal.getClass());
    }

}

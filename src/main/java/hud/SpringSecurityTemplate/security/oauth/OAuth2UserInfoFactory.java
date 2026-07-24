package hud.SpringSecurityTemplate.security.oauth;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.util.Map;

public class OAuth2UserInfoFactory {
    public static OAuth2UserInfo getInstance(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "github" -> new OAuth2UserInfo.Github(attributes);
            default -> throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_provider"), "Login with " + registrationId + " is not supported");
        };
    }
}

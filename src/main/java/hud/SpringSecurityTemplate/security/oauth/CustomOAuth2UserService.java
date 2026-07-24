package hud.SpringSecurityTemplate.security.oauth;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final OAuth2Service processingService;

    @Override
    public OAuth2User loadUser(@NonNull OAuth2UserRequest request) {
        OAuth2User oAuth2User = super.loadUser(request);
        processingService.processOAuth2User(request.getClientRegistration().getRegistrationId(), oAuth2User.getAttributes());
        return oAuth2User;
    }
}



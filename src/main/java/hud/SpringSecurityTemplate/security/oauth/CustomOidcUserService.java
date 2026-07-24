package hud.SpringSecurityTemplate.security.oauth;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {
    private final OAuth2Service processingService;

    @Override
    public OidcUser loadUser(@NonNull OidcUserRequest request) {
        OidcUser oidcUser = super.loadUser(request);
        processingService.processOAuth2User(request.getClientRegistration().getRegistrationId(), oidcUser.getAttributes());
        return oidcUser;
    }
}

package hud.SpringSecurityTemplate.security.oauth;

import hud.SpringSecurityTemplate.models.User;
import hud.SpringSecurityTemplate.models.UserStatus;
import hud.SpringSecurityTemplate.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OAuth2Service {

    private final UserRepository userRepository;

    public void processOAuth2User(String registrationId, Map<String, Object> attributes) {
        OAuth2UserInfo info = OAuth2UserInfoFactory.getInstance(registrationId, attributes);

        if (!StringUtils.hasText(info.getEmail())) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_found"), "Email not provided by " + registrationId);
        }

        userRepository.findByEmail(info.getEmail())
                .map(existing -> updateExisting(existing, info, registrationId))
                .or(() -> Optional.of(createNew(info, registrationId)))
                .ifPresent(userRepository::save);
    }

    private User updateExisting(User user, OAuth2UserInfo info, String provider) {
        user.setFullName(info.getFullName());
        user.setImage(info.getImageUrl());
        user.setProvider(provider);
        user.setStatus(UserStatus.ACTIVE.name());
        return user;
    }

    private User createNew(OAuth2UserInfo info, String provider) {
        return User.builder()
                .provider(provider)
                .email(info.getEmail())
                .fullName(info.getFullName())
                .phoneNumber(info.getPhoneNumber())
                .role("USER")
                .status(UserStatus.ACTIVE.name())
                .passwordChanged(false)
                .isOtpVerified(false)
                .image(info.getImageUrl())
                .build();
    }
}

package hud.SpringSecurityTemplate.security.oauth;

import java.util.Map;

public interface OAuth2UserInfo {
    String getProviderId();
    String getFullName();
    String getEmail();
    String getPhoneNumber();
    String getImageUrl();


    class Github implements OAuth2UserInfo {
        private final Map<String, Object> attributes;

        public Github(Map<String, Object> attributes) {
            this.attributes = attributes;
        }

        @Override
        public String getProviderId() {
            return String.valueOf(attributes.get("id"));
        }

        @Override
        public String getEmail() {
            return (String) attributes.get("email");
        } // may be null if private

        @Override
        public String getFullName() {
            return (String) attributes.get("name");
        }

        @Override
        public String getPhoneNumber() {
            return null;
        }

        @Override
        public String getImageUrl() {
            return (String) attributes.get("avatar_url");
        }
    }
}

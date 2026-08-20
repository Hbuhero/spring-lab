package hud.SpringSecurityTemplate.config;

import hud.SpringSecurityTemplate.security.oauth.CustomOAuth2AuthorizationSuccessHandler;
import hud.SpringSecurityTemplate.security.oauth.CustomOAuth2UserService;
import hud.SpringSecurityTemplate.security.oauth.CustomOidcUserService;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import hud.SpringSecurityTemplate.security.CustomUserDetailService;
import hud.SpringSecurityTemplate.security.JwtRequestFilter;

@Configuration
@EnableWebSecurity
public class AppSecurityConfig {

    private final CustomUserDetailService userDetailsService;
    private final JwtRequestFilter jwtRequestFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService  customOidcUserService;
    private final CustomOAuth2AuthorizationSuccessHandler customOAuth2AuthorizationSuccessHandler;

    public AppSecurityConfig(
            CustomUserDetailService userDetailsService,
            JwtRequestFilter jwtRequestFilter,
            CustomOAuth2UserService customOAuth2UserService,
            CustomOidcUserService customOidcUserService,
            CustomOAuth2AuthorizationSuccessHandler customOAuth2AuthorizationSuccessHandler
    ) {
        this.userDetailsService = userDetailsService;
        this.jwtRequestFilter = jwtRequestFilter;
        this.customOAuth2UserService = customOAuth2UserService;
        this.customOidcUserService = customOidcUserService;
        this.customOAuth2AuthorizationSuccessHandler = customOAuth2AuthorizationSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationEntryPoint authenticationEntryPoint) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(corsPolicy -> corsPolicy.configure(http))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(
                                "/login/**",
                                "/actuator/**",
                                "/sign-in",
                                "/signin",
                                "/*.html",
                                "/assets/**",
                                "/fonts/**",
                                "/icons/**",
                                "/favicon.ico",
                                "/manifest.json",
                                "/robots.txt",
                                "/error"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                                .oidcUserService(customOidcUserService)
                        )
                        .successHandler(customOAuth2AuthorizationSuccessHandler)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                ;

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
        return authenticationManagerBuilder.build();
    }

}

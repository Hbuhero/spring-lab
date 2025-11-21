package hud.SpringSecurityTemplate.config;

import hud.SpringSecurityTemplate.security.CustomOAuth2AuthorizationSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CorsFilter;
import hud.SpringSecurityTemplate.security.CustomUserDetailService;
import hud.SpringSecurityTemplate.security.JwtRequestFilter;

@Configuration
@EnableWebSecurity
public class AppSecurityConfig {

    @Autowired
    private CustomUserDetailService userDetailsService;

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private CustomOAuth2AuthorizationSuccessHandler customOAuth2AuthorizationSuccessHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Autowired
    CorsFilter corsFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(corsPolicy -> corsPolicy.configure(http))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/kafka/**").permitAll()
                        .requestMatchers("/api/v1/dynamic-urls/**").permitAll()
                        .requestMatchers("/api/v1/livestock/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                                .loginProcessingUrl("/api/v1/auth/login")
                                .isCustomLoginPage()
                        )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(customOAuth2AuthorizationSuccessHandler)
                        .redirectionEndpoint(Customizer.withDefaults())

                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
        return authenticationManagerBuilder.build();
    }

}
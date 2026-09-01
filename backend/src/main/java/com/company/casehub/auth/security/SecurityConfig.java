package com.company.casehub.auth.security;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;

/**
 * V1 security baseline:
 *  - Server-side HTTP session (JSESSIONID), NO JWT.
 *  - CSRF enabled with a non-HttpOnly XSRF-TOKEN cookie (front-end reads it for the
 *    X-XSRF-TOKEN header). CSRF is never disabled.
 *  - Stateless JSON 401/403 responses (no login-page redirect).
 *  - Session-expiry enforcement via SessionExpiryFilter (HIGH-01): an expired
 *    session (admin disable / password reset) is invalidated on the next request.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SessionRegistry sessionRegistry) throws Exception {
        http
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/csrf", "/actuator/health").permitAll()
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/logout").permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .securityContext(security -> security
                        .securityContextRepository(securityContextRepository()))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .addFilterAfter(sessionExpiryFilter(sessionRegistry), SecurityContextHolderFilter.class);
        return http.build();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new org.springframework.security.web.context.HttpSessionSecurityContextRepository();
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        // Frozen front-end contract: cookie XSRF-TOKEN (HttpOnly=false) + header X-XSRF-TOKEN.
        // The default CookieCsrfTokenRepository uses header X-CSRF-TOKEN; override it so the
        // server validates the same header the SPA actually sends.
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setHeaderName(SecurityConstants.CSRF_HEADER_NAME);
        return repository;
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /**
     * Publishes {@code SessionCreatedEvent} / {@code SessionDestroyedEvent} for every
     * HttpSession lifecycle change. {@link SessionRegistryImpl} listens for the destroy
     * event, so it automatically drops {@link SessionInformation} on logout, session
     * invalidation, timeout and server-side expiry — keeping the registry free of stale
     * entries (Phase 0-3 review, MEDIUM, session lifecycle).
     */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    /**
     * Session-authentication strategy applied by {@code AuthenticationService} after a
     * successful login (round 2, session fixation + registry registration):
     *   1. {@link ChangeSessionIdAuthenticationStrategy} mints a new session id on login,
     *      neutralising session fixation (the pre-login anonymous id is invalidated).
     *   2. {@link RegisterSessionAuthenticationStrategy} registers the new session with the
     *      {@link SessionRegistry} so admin disable / password reset can expire it.
     */
    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy(SessionRegistry sessionRegistry) {
        return new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(),
                new RegisterSessionAuthenticationStrategy(sessionRegistry)));
    }

    @Bean
    public SessionExpiryFilter sessionExpiryFilter(SessionRegistry sessionRegistry) {
        return new SessionExpiryFilter(sessionRegistry, authenticationEntryPoint);
    }

    /**
     * SessionExpiryFilter is wired into the Spring Security filter chain via
     * {@code addFilterAfter(...)}. As a {@code Filter} bean it would otherwise also be
     * auto-registered as a standalone servlet filter (running once more for every request
     * outside the chain). Disabling the registration keeps it running ONLY inside the
     * security chain at the intended position.
     */
    @Bean
    public FilterRegistrationBean<SessionExpiryFilter> sessionExpiryFilterRegistration(SessionExpiryFilter sessionExpiryFilter) {
        FilterRegistrationBean<SessionExpiryFilter> registration = new FilterRegistrationBean<>(sessionExpiryFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new org.springframework.security.authentication.ProviderManager(provider);
    }
}

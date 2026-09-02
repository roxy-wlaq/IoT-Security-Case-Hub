package com.company.casehub.auth.service;

import com.company.casehub.auth.dto.ChangePasswordRequest;
import com.company.casehub.auth.dto.CurrentUserResponse;
import com.company.casehub.auth.dto.LoginRequest;
import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.common.exception.CaseHubException;
import com.company.casehub.common.exception.ErrorCode;
import com.company.casehub.user.entity.UserEntity;
import com.company.casehub.user.repository.UserRepository;
import com.company.casehub.user.service.CurrentUserService;
import com.company.casehub.user.service.UserDetailsServiceImpl;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Session-based authentication orchestration. CSRF is enforced by the Spring
 * Security filter chain; this service only performs credential verification,
 * builds the {@link SecurityContext}, and persists it to the HTTP session.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final LoginAttemptService loginAttemptService;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final UserDetailsServiceImpl userDetailsService;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SessionRegistry sessionRegistry;

    public CurrentUserResponse login(LoginRequest request, HttpServletRequest httpRequest,
                                     HttpServletResponse httpResponse) {
        String ip = clientIp(httpRequest);
        String key = loginAttemptService.key(request.username(), ip);
        if (loginAttemptService.isBlocked(key)) {
            throw new CaseHubException(ErrorCode.AUTH_LOGIN_TEMPORARILY_BLOCKED);
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (DisabledException e) {
            throw new CaseHubException(ErrorCode.USER_DISABLED);
        } catch (AuthenticationException e) {
            loginAttemptService.recordFailure(key);
            throw new CaseHubException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        loginAttemptService.recordSuccess(key);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        // Apply Spring Security's session authentication strategy FIRST. This both
        // mitigates session fixation (ChangeSessionIdAuthenticationStrategy mints a new
        // session id after authentication) and registers the new session with the
        // SessionRegistry (RegisterSessionAuthenticationStrategy) so admin disable /
        // password reset can expire it.
        // It may throw SessionAuthenticationException, so it MUST run before we persist
        // the authenticated SecurityContext: otherwise a failed strategy would leave an
        // authenticated context bound to an unverified session (phase 0-3 review R3).
        sessionAuthenticationStrategy.onAuthentication(authentication, httpRequest, httpResponse);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        log.info("Login success user={} ip={}", principal.getUsername(), ip);
        return toResponse(principal);
    }

    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        UserPrincipal principal = currentUserOrNull();
        SecurityContextHolder.clearContext();
        securityContextRepository.saveContext(SecurityContextHolder.createEmptyContext(), httpRequest, httpResponse);
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            sessionRegistry.removeSessionInformation(session.getId());
            session.invalidate();
        }
        Cookie cookie = new Cookie("XSRF-TOKEN", null);
        cookie.setPath("/");
        cookie.setHttpOnly(false);
        cookie.setMaxAge(0);
        httpResponse.addCookie(cookie);
        if (principal != null) {
            log.info("Logout user={}", principal.getUsername());
        }
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        UserPrincipal principal = currentUserService.currentUser();
        UserEntity user = userDetailsService.requireUser(principal.getId());

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new CaseHubException(ErrorCode.AUTH_CURRENT_PASSWORD_MISMATCH);
        }
        passwordPolicy.validate(request.newPassword(), user.getUsername());

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
        log.info("Password changed user={}", user.getUsername());
    }

    public CurrentUserResponse currentUser() {
        return toResponse(currentUserService.currentUser());
    }

    private CurrentUserResponse toResponse(UserPrincipal principal) {
        return new CurrentUserResponse(
                principal.getId(),
                principal.getUsername(),
                principal.getDisplayName(),
                principal.isEnabled(),
                principal.isMustChangePassword(),
                List.copyOf(principal.getRoles()),
                List.copyOf(principal.getPermissions()));
    }

    private UserPrincipal currentUserOrNull() {
        try {
            return currentUserService.currentUser();
        } catch (CaseHubException e) {
            return null;
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

package com.company.casehub.auth.controller;

import com.company.casehub.auth.dto.ChangePasswordRequest;
import com.company.casehub.auth.dto.CurrentUserResponse;
import com.company.casehub.auth.dto.CsrfResponse;
import com.company.casehub.auth.dto.LoginRequest;
import com.company.casehub.auth.security.SecurityConstants;
import com.company.casehub.auth.service.AuthenticationService;
import com.company.casehub.common.web.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final CsrfTokenRepository csrfTokenRepository;

    @GetMapping("/csrf")
    public CsrfResponse csrf(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token == null) {
            token = csrfTokenRepository.generateToken(request);
        }
        csrfTokenRepository.saveToken(token, request, response);
        // Report the frozen-contract names explicitly so the client never depends on
        // CookieCsrfTokenRepository.getHeaderName() internals.
        return new CsrfResponse(SecurityConstants.CSRF_HEADER_NAME, SecurityConstants.CSRF_COOKIE_NAME, token.getToken());
    }

    @PostMapping("/login")
    public CurrentUserResponse login(@Valid @RequestBody LoginRequest request,
                                     HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return authenticationService.login(request, httpRequest, httpResponse);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authenticationService.logout(httpRequest, httpResponse);
    }

    @GetMapping("/me")
    public CurrentUserResponse me() {
        return authenticationService.currentUser();
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authenticationService.changePassword(request);
    }
}

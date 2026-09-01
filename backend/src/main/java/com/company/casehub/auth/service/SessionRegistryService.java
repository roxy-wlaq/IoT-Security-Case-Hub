package com.company.casehub.auth.service;

import com.company.casehub.auth.security.UserPrincipal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper over Spring's {@link SessionRegistry} to support the
 * "disable user / reset password -> expire active sessions" requirement
 * (Final Technical Review §7). Expires every session bound to the given user id.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionRegistryService {

    private final SessionRegistry sessionRegistry;

    public void expireSessions(UUID userId) {
        int expired = 0;
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof UserPrincipal up && userId.equals(up.getId())) {
                List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);
                for (SessionInformation info : sessions) {
                    info.expireNow();
                    expired++;
                }
            }
        }
        if (expired > 0) {
            log.info("Expired {} active session(s) for user {}", expired, userId);
        }
    }
}

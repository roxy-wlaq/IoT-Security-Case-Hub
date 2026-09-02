package com.company.casehub.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Shared test slice configuration for {@code @WebMvcTest} RBAC tests.
 *
 * <p>{@code @WebMvcTest} only loads the web slice, so the production
 * {@code SecurityConfig} (which carries {@code @EnableMethodSecurity}) is not part of
 * the context and controller-level {@code @PreAuthorize} expressions would silently not
 * be enforced — a controller action would return 200 even for a principal that lacks the
 * required authority. Importing this config into the test restores method security so the
 * RBAC assertions are real.
 *
 * <p>Usage:
 * <pre>{@code
 * @WebMvcTest(controllers = StandardTaskTypeController.class)
 * @Import(MethodSecurityTestConfig.class)
 * class StandardTaskTypeControllerRbacTest { ... }
 * }</pre>
 *
 * <p>Remember to add {@code .with(csrf())} to POST/PUT/DELETE requests: CSRF is enabled
 * in the test slice exactly as in production, and a missing token yields 403 for a reason
 * unrelated to the permission under test.
 */
@Configuration
@EnableMethodSecurity
public class MethodSecurityTestConfig {
}

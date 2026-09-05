package com.company.casehub.auth.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.casehub.auth.controller.AuthController;
import com.company.casehub.auth.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@TestPropertySource(properties = "casehub.security.cors.allowed-origins=http://localhost:5173")
class ProductionSecurityHeadersCorsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void csrfEndpointEmitsSecurityHeadersAndConfiguredCredentialedCors() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf").header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Permissions-Policy", "camera=(), microphone=(), geolocation=()"))
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void unlistedOriginIsNotAcceptedForCredentialedCors() throws Exception {
        mockMvc.perform(options("/api/v1/auth/csrf")
                        .header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void rawReadableCookieTokenIsAcceptedInTheFrozenSpaHeaderContract() throws Exception {
        var csrf = mockMvc.perform(get("/api/v1/auth/csrf")).andReturn().getResponse();
        String token = csrf.getCookie("XSRF-TOKEN").getValue();

        mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(new MockCookie("XSRF-TOKEN", token))
                        .header("X-XSRF-TOKEN", token)
                        .contentType("application/json")
                        .content("{\"username\":\"u\",\"password\":\"p\"}"))
                .andExpect(status().isOk());
    }
}

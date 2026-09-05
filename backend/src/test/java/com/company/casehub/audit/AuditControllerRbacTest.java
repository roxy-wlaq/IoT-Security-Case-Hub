package com.company.casehub.audit;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.casehub.audit.controller.AuditController;
import com.company.casehub.audit.service.AuditService;
import com.company.casehub.common.MethodSecurityTestConfig;
import com.company.casehub.testcase.dto.PagedResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuditController.class)
@Import(MethodSecurityTestConfig.class)
class AuditControllerRbacTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditService auditService;

    @Test
    @WithMockUser(authorities = {"ROLE_ADMIN", "audit:read"})
    void adminWithAuditReadMayQueryAuditLogs() throws Exception {
        when(auditService.query(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PagedResponse<>(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get("/api/v1/audit-logs"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_COORDINATOR", "audit:read"})
    void nonAdminWithAuditReadIsDenied() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMIN", "project:read"})
    void adminWithoutAuditReadIsDenied() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs"))
                .andExpect(status().isForbidden());
    }
}

package com.company.casehub.testcase.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.casehub.common.MethodSecurityTestConfig;
import com.company.casehub.testcase.dto.CreateDraftRequest;
import com.company.casehub.testcase.service.TestCaseDraftService;
import com.company.casehub.testcase.service.TestCaseQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TestCaseController.class)
@Import(MethodSecurityTestConfig.class)
class TestCaseControllerRbacTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private TestCaseDraftService draftService;
    @MockBean private TestCaseQueryService queryService;

    @Test
    void testerCannotCreateDraft() throws Exception {
        mockMvc.perform(post("/api/v1/test-cases").with(user("tester")).with(csrf())
                        .contentType("application/json")
                        .content("{\"caseCode\":\"BLE-001\",\"categoryId\":\"00000000-0000-0000-0000-000000000001\",\"caseName\":\"Pairing\",\"selectionMode\":\"SINGLE\"}"))
                .andExpect(status().isForbidden());
        verify(draftService, never()).createDraft(any(CreateDraftRequest.class), any());
    }
}

package com.company.casehub.batch4;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

/** RED contract tests for the Batch 4 vertical slice. */
class Batch4FeatureContractTest {

    @Test
    void customCaseServiceIsPresent() {
        assertThatCode(() -> Class.forName("com.company.casehub.customcase.service.CustomTestCaseService"))
                .doesNotThrowAnyException();
    }

    @Test
    void capabilityRequestServiceIsPresent() {
        assertThatCode(() -> Class.forName("com.company.casehub.change.service.CapabilityUpdateRequestService"))
                .doesNotThrowAnyException();
    }

    @Test
    void changeRequestServiceIsPresent() {
        assertThatCode(() -> Class.forName("com.company.casehub.change.service.TestCaseChangeRequestService"))
                .doesNotThrowAnyException();
    }

    @Test
    void versionUpgradeServiceIsPresent() {
        assertThatCode(() -> Class.forName("com.company.casehub.upgrade.service.VersionUpgradeService"))
                .doesNotThrowAnyException();
    }
}

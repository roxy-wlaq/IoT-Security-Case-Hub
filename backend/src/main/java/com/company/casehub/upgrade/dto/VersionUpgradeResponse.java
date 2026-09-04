package com.company.casehub.upgrade.dto;

import java.util.UUID;

public record VersionUpgradeResponse(UUID projectTestCaseId, UUID previousVersionId, UUID currentVersionId,
                                     boolean upgraded, VersionDiffResponse diff) { }

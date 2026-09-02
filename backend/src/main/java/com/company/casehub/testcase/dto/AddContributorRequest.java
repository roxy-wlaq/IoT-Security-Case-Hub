package com.company.casehub.testcase.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddContributorRequest(@NotNull UUID userId) {
}

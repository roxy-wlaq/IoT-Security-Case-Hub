package com.company.casehub.evidence.dto;

import jakarta.validation.constraints.NotBlank;

public record NoteRequest(@NotBlank String body) {
}

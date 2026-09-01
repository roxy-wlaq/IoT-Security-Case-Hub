package com.company.casehub.auth.dto;

public record CsrfResponse(
        String headerName,
        String cookieName,
        String token) {
}

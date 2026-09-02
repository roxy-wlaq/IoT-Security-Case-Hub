package com.company.casehub.testcase.dto;

import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Creates a new Revision Draft from a PUBLISHED source version. The server
 * rejects any client-supplied version number: the new major equals the source
 * major and the new minor is MAX(minor of all versions with that major) + 1,
 * computed inside a PESSIMISTIC_WRITE-locked Master transaction.
 *
 * @param sourceVersionId optional; when omitted the current PUBLISHED version is used
 * @param changeReason    optional human-readable reason for the revision
 */
public record CreateRevisionRequest(UUID sourceVersionId, @Size(max = 2000) String changeReason) {
}

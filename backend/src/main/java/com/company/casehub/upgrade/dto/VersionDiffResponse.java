package com.company.casehub.upgrade.dto;

import java.util.List;

public record VersionDiffResponse(List<String> changedFields, boolean logicChanged, boolean compatible,
                                   String warning) { }

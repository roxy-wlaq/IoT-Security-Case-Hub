package com.company.casehub.testcase.repository;

import com.company.casehub.testcase.entity.TestCaseVersionStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;

public interface TestCaseLibraryQueryRepository {
    PageResult search(Query query);

    record Query(String q, UUID categoryId, List<UUID> tagIds, List<UUID> toolIds,
                 List<UUID> standardTaskTypeIds, TestCaseVersionStatus status,
                 UUID principalId, boolean admin, int page, int size, Sort.Order order) {}

    record Row(UUID masterId, UUID versionId) {}

    record PageResult(List<Row> rows, long totalElements) {}
}

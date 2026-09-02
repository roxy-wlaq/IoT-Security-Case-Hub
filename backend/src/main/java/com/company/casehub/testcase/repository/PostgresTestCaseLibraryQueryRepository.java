package com.company.casehub.testcase.repository;

import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class PostgresTestCaseLibraryQueryRepository implements TestCaseLibraryQueryRepository {

    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "caseName", "selected_versions.case_name",
            "updatedAt", "selected_versions.updated_at",
            "createdAt", "selected_versions.created_at",
            "caseCode", "selected_versions.case_code");

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PostgresTestCaseLibraryQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PageResult search(Query query) {
        Objects.requireNonNull(query, "query must not be null");
        List<UUID> tagIds = immutableIds(query.tagIds());
        List<UUID> toolIds = immutableIds(query.toolIds());
        List<UUID> standardTaskTypeIds = immutableIds(query.standardTaskTypeIds());
        MapSqlParameterSource parameters = parameters(query, tagIds, toolIds, standardTaskTypeIds);
        String sharedCte = candidateAndSelectionCte(query, tagIds, toolIds, standardTaskTypeIds);
        String pageSql = sharedCte + pageSql(query.order());
        String countSql = sharedCte + "\nSELECT COUNT(*) FROM selected_versions";

        List<Row> rows = jdbcTemplate.query(pageSql, parameters,
                (resultSet, rowNum) -> new Row(
                        resultSet.getObject("master_id", UUID.class),
                        resultSet.getObject("version_id", UUID.class)));
        Long total = jdbcTemplate.queryForObject(countSql, parameters, Long.class);
        return new PageResult(List.copyOf(rows), total == null ? 0 : total);
    }

    private MapSqlParameterSource parameters(Query query, List<UUID> tagIds, List<UUID> toolIds,
                                               List<UUID> standardTaskTypeIds) {
        int normalizedPage = Math.max(query.page(), 0);
        long offset = (long) normalizedPage * query.size();
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("status", query.status() == null ? null : query.status().name(), Types.VARCHAR)
                .addValue("admin", query.admin())
                .addValue("principal_id", query.principalId(), Types.OTHER)
                .addValue("q_pattern", qPattern(query.q()), Types.VARCHAR)
                .addValue("size", query.size(), Types.INTEGER)
                .addValue("offset", offset, Types.BIGINT);
        if (query.categoryId() != null) {
            parameters.addValue("category_id", query.categoryId(), Types.OTHER);
        }
        if (!tagIds.isEmpty()) {
            parameters.addValue("tag_ids", tagIds);
        }
        if (!toolIds.isEmpty()) {
            parameters.addValue("tool_ids", toolIds);
        }
        if (!standardTaskTypeIds.isEmpty()) {
            parameters.addValue("standard_ids", standardTaskTypeIds);
        }
        return parameters;
    }

    private String candidateAndSelectionCte(Query query, List<UUID> tagIds, List<UUID> toolIds,
                                             List<UUID> standardTaskTypeIds) {
        StringBuilder predicates = new StringBuilder("""
                (:status IS NULL OR v.status = :status)
                  AND (:admin = TRUE OR v.status = 'PUBLISHED'
                       OR (v.status = 'DRAFT' AND v.created_by = :principal_id))
                  AND (
                      :q_pattern IS NULL
                      OR m.case_code ILIKE :q_pattern
                      OR EXISTS (
                          SELECT 1 FROM casehub.test_case_tags mt
                          JOIN casehub.tags t ON t.id = mt.tag_id
                          WHERE mt.master_test_case_id = m.id AND t.name ILIKE :q_pattern
                      )
                      OR v.case_name ILIKE :q_pattern
                      OR v.test_purpose ILIKE :q_pattern
                      OR EXISTS (
                          SELECT 1 FROM casehub.test_steps s
                          WHERE s.test_case_version_id = v.id
                            AND (s.title ILIKE :q_pattern OR s.content ILIKE :q_pattern)
                      )
                      OR EXISTS (
                          SELECT 1 FROM casehub.test_case_tools vt
                          JOIN casehub.tools tool ON tool.id = vt.tool_id
                          WHERE vt.test_case_version_id = v.id AND tool.name ILIKE :q_pattern
                      )
                  )""");
        if (!toolIds.isEmpty()) {
            predicates.append("""
                    
                      AND EXISTS (
                          SELECT 1 FROM casehub.test_case_tools ct
                          WHERE ct.test_case_version_id = v.id AND ct.tool_id IN (:tool_ids)
                      )""");
        }
        if (!standardTaskTypeIds.isEmpty()) {
            predicates.append("""
                    
                      AND EXISTS (
                          SELECT 1 FROM casehub.test_case_standard_mappings cm
                          WHERE cm.test_case_version_id = v.id
                            AND cm.standard_task_type_id IN (:standard_ids)
                      )""");
        }
        if (query.categoryId() != null) {
            predicates.append("\n  AND m.category_id = :category_id");
        }
        if (!tagIds.isEmpty()) {
            predicates.append("""
                    
                      AND EXISTS (
                          SELECT 1 FROM casehub.test_case_tags filter_tag
                          WHERE filter_tag.master_test_case_id = m.id AND filter_tag.tag_id IN (:tag_ids)
                      )""");
        }
        return """
                WITH candidate_versions AS (
                    SELECT m.id AS master_id, v.id AS version_id,
                           v.version_major, v.version_minor, v.status,
                           v.is_current_version, v.case_name, v.created_at, v.updated_at,
                           m.case_code
                    FROM casehub.master_test_cases m
                    JOIN casehub.test_case_versions v ON v.master_test_case_id = m.id
                    WHERE %s
                ), selected_versions AS (
                    SELECT DISTINCT ON (master_id) *
                    FROM candidate_versions
                    ORDER BY master_id,
                             (status = 'PUBLISHED' AND is_current_version) DESC,
                             version_major DESC,
                             version_minor DESC,
                             version_id ASC
                )
                """.formatted(predicates);
    }

    private String pageSql(Sort.Order order) {
        Objects.requireNonNull(order, "sort order must not be null");
        String column = SORT_COLUMNS.get(order.getProperty());
        if (column == null) {
            throw new IllegalArgumentException("Unsupported test case sort: " + order.getProperty());
        }
        String direction = order.isAscending() ? "ASC" : "DESC";
        return """
                SELECT master_id, version_id
                FROM selected_versions
                ORDER BY %s %s NULLS LAST,
                         selected_versions.master_id ASC
                LIMIT :size OFFSET :offset
                """.formatted(column, direction);
    }

    private static List<UUID> immutableIds(List<UUID> ids) {
        return ids == null ? List.of() : List.copyOf(ids);
    }

    private static String qPattern(String q) {
        return StringUtils.hasText(q) ? "%" + q.trim() + "%" : null;
    }
}

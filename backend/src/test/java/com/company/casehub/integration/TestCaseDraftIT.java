package com.company.casehub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.category.entity.CategoryEntity;
import com.company.casehub.category.repository.CategoryRepository;
import com.company.casehub.common.exception.ValidationException;
import com.company.casehub.standard.entity.StandardTaskTypeEntity;
import com.company.casehub.standard.repository.StandardTaskTypeRepository;
import com.company.casehub.tag.entity.TagEntity;
import com.company.casehub.tag.repository.TagRepository;
import com.company.casehub.testcase.dto.CreateDraftRequest;
import com.company.casehub.testcase.dto.StandardMappingRequest;
import com.company.casehub.testcase.dto.StepRequest;
import com.company.casehub.testcase.dto.TestCaseDetailResponse;
import com.company.casehub.testcase.dto.TestCaseSummaryResponse;
import com.company.casehub.testcase.dto.UpdateDraftRequest;
import com.company.casehub.testcase.entity.ProgressiveRole;
import com.company.casehub.testcase.entity.SelectionMode;
import com.company.casehub.testcase.entity.MasterTestCaseEntity;
import com.company.casehub.testcase.entity.TestCaseStandardMappingEntity;
import com.company.casehub.testcase.entity.TestCaseTagEntity;
import com.company.casehub.testcase.entity.TestCaseToolEntity;
import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import com.company.casehub.testcase.entity.TestCaseVersionStatus;
import com.company.casehub.testcase.repository.MasterTestCaseRepository;
import com.company.casehub.testcase.repository.TestCaseStandardMappingRepository;
import com.company.casehub.testcase.repository.TestCaseTagRepository;
import com.company.casehub.testcase.repository.TestCaseToolRepository;
import com.company.casehub.testcase.repository.TestCaseVersionRepository;
import com.company.casehub.testcase.repository.TestStepRepository;
import com.company.casehub.testcase.service.TestCaseDraftService;
import com.company.casehub.testcase.service.TestCaseQueryService;
import com.company.casehub.tool.entity.ToolEntity;
import com.company.casehub.tool.repository.ToolRepository;
import com.company.casehub.user.entity.UserEntity;
import com.company.casehub.user.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TestCaseDraftIT extends AbstractIntegrationTest {

    @Autowired private TestCaseDraftService draftService;
    @Autowired private TestCaseQueryService queryService;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private TagRepository tagRepository;
    @Autowired private ToolRepository toolRepository;
    @Autowired private StandardTaskTypeRepository standardRepository;
    @Autowired private MasterTestCaseRepository masterRepository;
    @Autowired private TestCaseVersionRepository versionRepository;
    @Autowired private TestStepRepository stepRepository;
    @Autowired private TestCaseTagRepository caseTagRepository;
    @Autowired private TestCaseToolRepository caseToolRepository;
    @Autowired private TestCaseStandardMappingRepository mappingRepository;

    private UserEntity user;
    private CategoryEntity category;
    private TagEntity tag;
    private ToolEntity tool;
    private StandardTaskTypeEntity standard;
    private UserPrincipal coordinator;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        user = userRepository.save(new UserEntity("it_coord_" + suffix, "IT Coordinator", "hash"));
        category = new CategoryEntity();
        category.setCode("it-cat-" + suffix);
        category.setName("IT Category");
        category.setLevel(1);
        category = categoryRepository.save(category);
        tag = new TagEntity();
        tag.setCode("it-tag-" + suffix);
        tag.setName("IT Tag " + suffix);
        tag = tagRepository.save(tag);
        tool = new ToolEntity();
        tool.setCode("it-tool-" + suffix);
        tool.setName("IT Tool " + suffix);
        tool = toolRepository.save(tool);
        standard = new StandardTaskTypeEntity();
        standard.setCode("it-standard-" + suffix);
        standard.setName("IT Standard " + suffix);
        standard.setType("STANDARD");
        standard = standardRepository.save(standard);
        coordinator = new UserPrincipal(user.getId(), user.getUsername(), "hash", user.getDisplayName(), true, false,
                Set.of("TEST_COORDINATOR"), Set.of("test_case:read", "test_case:draft_create", "test_case:draft_edit"));
    }

    @Test
    void coordinatorDraftPersistsAggregateAndQueryReadsItBack() {
        TestCaseDetailResponse created = draftService.createDraft(request("BLE-IT-001", "Pairing Flow", List.of(
                new StepRequest("Prepare", "Power on device"), new StepRequest("Pair", "Connect over BLE"))), coordinator);

        assertThat(masterRepository.existsById(created.id())).isTrue();
        assertThat(versionRepository.findAll()).anyMatch(version -> version.getMasterTestCase().getId().equals(created.id())
                && version.getVersionMajor() == 1 && version.getVersionMinor() == 0
                && version.getStatus().name().equals("DRAFT"));
        UUID versionId = created.visibleVersion().id();
        assertThat(stepRepository.findByTestCaseVersionId(versionId)).hasSize(2);
        assertThat(caseTagRepository.findByMasterTestCaseId(created.id())).hasSize(1);
        assertThat(caseToolRepository.findByTestCaseVersionId(versionId)).hasSize(1);
        assertThat(mappingRepository.findByTestCaseVersionId(versionId)).hasSize(1);

        TestCaseDetailResponse readBack = queryService.detail(created.id(), coordinator);
        assertThat(readBack.visibleVersion().caseName()).isEqualTo("Pairing Flow");
        assertThat(readBack.visibleVersion().steps()).extracting("content")
                .containsExactly("Power on device", "Connect over BLE");
        assertThat(readBack.tags()).extracting("id").containsExactly(tag.getId());
        assertThat(readBack.visibleVersion().tools()).extracting("id").containsExactly(tool.getId());
        assertThat(readBack.visibleVersion().standardMappings()).extracting("mappingNote")
                .containsExactly("pairing baseline");
    }

    @Test
    void draftUpdateReplacesPersistedContentAndRelationsTransactionally() {
        TestCaseDetailResponse created = draftService.createDraft(request("BLE-IT-002", "Before", List.of(
                new StepRequest(null, "old step"))), coordinator);
        TagEntity replacementTag = enabledTag("replacement");
        ToolEntity replacementTool = enabledTool("replacement");
        StandardTaskTypeEntity replacementStandard = enabledStandard("replacement");

        draftService.updateDraft(created.id(), new UpdateDraftRequest("After", "new purpose", "new preconditions",
                SelectionMode.MULTIPLE, true, "attach logs", "keep notes", ProgressiveRole.ENTRY,
                List.of(new StepRequest("New", "new step")), List.of(replacementTag.getId()), List.of(replacementTool.getId()),
                List.of(new StandardMappingRequest(replacementStandard.getId(), "replacement note"))), coordinator);

        TestCaseDetailResponse readBack = queryService.detail(created.id(), coordinator);
        assertThat(readBack.visibleVersion().caseName()).isEqualTo("After");
        assertThat(readBack.visibleVersion().selectionMode()).isEqualTo("MULTIPLE");
        assertThat(readBack.visibleVersion().progressiveRole()).isEqualTo("ENTRY");
        assertThat(readBack.visibleVersion().steps()).extracting("content").containsExactly("new step");
        assertThat(readBack.tags()).extracting("id").containsExactly(replacementTag.getId());
        assertThat(readBack.visibleVersion().tools()).extracting("id").containsExactly(replacementTool.getId());
        assertThat(readBack.visibleVersion().standardMappings()).extracting("mappingNote")
                .containsExactly("replacement note");
    }

    @Test
    void sortByCaseNameDoesNotFail() {
        draftService.createDraft(request("BLE-IT-003", "Zulu", List.of()), coordinator);
        draftService.createDraft(request("BLE-IT-004", "Alpha", List.of()), coordinator);

        assertThat(queryService.list(null, category.getId(), null, null, null, null, 0, 20, "caseName,asc", coordinator).content())
                .extracting("caseName").containsExactly("Alpha", "Zulu");
        assertThat(queryService.list(null, category.getId(), null, null, null, null, 0, 20, "caseName,desc", coordinator).content())
                .extracting("caseName").containsExactly("Zulu", "Alpha");
    }

    @Test
    void multiVersionCaseNameSortSemantics() {
        MasterTestCaseEntity masterA = newMaster("BLE-SEM-A");
        addVersion(masterA, 1, 0, TestCaseVersionStatus.PUBLISHED, false, "Alpha");
        addVersion(masterA, 2, 0, TestCaseVersionStatus.PUBLISHED, true, "Zulu");
        MasterTestCaseEntity masterB = newMaster("BLE-SEM-B");
        addVersion(masterB, 1, 0, TestCaseVersionStatus.PUBLISHED, true, "Beta");

        var ascending = queryService.list(null, category.getId(), null, null, null, null, 0, 20,
                "caseName,asc", coordinator).content();
        var descending = queryService.list(null, category.getId(), null, null, null, null, 0, 20,
                "caseName,desc", coordinator).content();
        assertThat(ascending).extracting("caseName", "versionLabel", "status")
                .containsExactly(tuple("Beta", "1.0", "PUBLISHED"), tuple("Zulu", "2.0", "PUBLISHED"));
        assertThat(descending).extracting("caseName", "versionLabel", "status")
                .containsExactly(tuple("Zulu", "2.0", "PUBLISHED"), tuple("Beta", "1.0", "PUBLISHED"));
    }

    @Test
    void statusFilterReturnsMatchingVersion() {
        TestCaseDetailResponse created = draftService.createDraft(request("BLE-SEM-STATUS", "Draft version", List.of()), coordinator);
        MasterTestCaseEntity master = masterRepository.findById(created.id()).orElseThrow();
        addVersion(master, 2, 0, TestCaseVersionStatus.PUBLISHED, true, "Published version");

        var draft = queryService.list(null, category.getId(), null, null, null, "DRAFT", 0, 20,
                "updatedAt,desc", coordinator).content().stream()
                .filter(summary -> summary.id().equals(created.id())).findFirst().orElseThrow();
        var published = queryService.list(null, category.getId(), null, null, null, "PUBLISHED", 0, 20,
                "updatedAt,desc", coordinator).content().stream()
                .filter(summary -> summary.id().equals(created.id())).findFirst().orElseThrow();
        assertThat(draft).extracting("status", "versionLabel", "caseName")
                .containsExactly("DRAFT", "1.0", "Draft version");
        assertThat(published).extracting("status", "versionLabel", "caseName")
                .containsExactly("PUBLISHED", "2.0", "Published version");
    }

    @Test
    void masterQueryMatchDoesNotRestrictVersionCandidate() {
        MasterTestCaseEntity master = newMaster("BLE-001");
        addVersion(master, 1, 0, TestCaseVersionStatus.PUBLISHED, true, "BLE Pairing");
        addVersion(master, 2, 0, TestCaseVersionStatus.DRAFT, false, "New Draft");

        var result = queryService.list("BLE", category.getId(), null, null, null, "DRAFT", 0, 20,
                "updatedAt,desc", coordinator);

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).singleElement()
                .extracting("id", "caseName", "status", "versionLabel")
                .containsExactly(master.getId(), "New Draft", "DRAFT", "2.0");
    }

    @Test
    void versionQueryMatchAndStatusMustReferToSameVersion() {
        MasterTestCaseEntity master = newMaster("ARCH-SPLIT-MATCH");
        addVersion(master, 1, 0, TestCaseVersionStatus.PUBLISHED, false, "Needle Published");
        addVersion(master, 2, 0, TestCaseVersionStatus.DRAFT, false, "Ordinary Draft");

        var splitMatch = queryService.list("Needle", category.getId(), null, null, null, "DRAFT", 0, 20,
                "updatedAt,desc", coordinator);

        assertThat(splitMatch.totalElements()).isZero();
        assertThat(splitMatch.content()).isEmpty();

        addVersion(master, 3, 1, TestCaseVersionStatus.DRAFT, false, "Needle Draft");
        var sameVersionMatch = queryService.list("Needle", category.getId(), null, null, null, "DRAFT", 0, 20,
                "updatedAt,desc", coordinator);

        assertThat(sameVersionMatch.totalElements()).isEqualTo(1);
        assertThat(sameVersionMatch.content()).singleElement()
                .extracting("id", "caseName", "status", "versionLabel")
                .containsExactly(master.getId(), "Needle Draft", "DRAFT", "3.1");
    }

    @Test
    void listVisibilityIncludesPublishedAndOwnDraftButOnlyAdminSeesOtherOwnersDraft() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UserEntity otherOwner = userRepository.save(new UserEntity(
                "it_other_" + suffix, "IT Other Owner", "hash"));
        UserEntity adminUser = userRepository.save(new UserEntity(
                "it_admin_" + suffix, "IT Admin", "hash"));
        UserPrincipal admin = new UserPrincipal(adminUser.getId(), adminUser.getUsername(), "hash",
                adminUser.getDisplayName(), true, false, Set.of("ADMIN"), Set.of("test_case:read"));

        MasterTestCaseEntity published = newMaster("VIS-PUBLISHED");
        addVersion(published, 1, 0, TestCaseVersionStatus.PUBLISHED, true, "Visible published", otherOwner);
        MasterTestCaseEntity ownDraft = newMaster("VIS-OWN-DRAFT");
        addVersion(ownDraft, 1, 1, TestCaseVersionStatus.DRAFT, false, "Visible own draft", user);
        MasterTestCaseEntity otherDraft = newMaster("VIS-OTHER-DRAFT", category, otherOwner);
        addVersion(otherDraft, 2, 3, TestCaseVersionStatus.DRAFT, false, "Admin-only draft", otherOwner);

        var coordinatorPage = queryService.list(null, category.getId(), null, null, null, null, 0, 20,
                "caseCode,asc", coordinator);

        assertThat(coordinatorPage.totalElements()).isEqualTo(2);
        assertThat(coordinatorPage.content()).extracting("caseCode", "caseName", "status", "versionLabel")
                .containsExactly(
                        tuple("VIS-OWN-DRAFT", "Visible own draft", "DRAFT", "1.1"),
                        tuple("VIS-PUBLISHED", "Visible published", "PUBLISHED", "1.0"));

        var adminPage = queryService.list(null, category.getId(), null, null, null, null, 0, 20,
                "caseCode,asc", admin);

        assertThat(adminPage.totalElements()).isEqualTo(3);
        assertThat(adminPage.content()).extracting("caseCode", "caseName", "status", "versionLabel")
                .containsExactly(
                        tuple("VIS-OTHER-DRAFT", "Admin-only draft", "DRAFT", "2.3"),
                        tuple("VIS-OWN-DRAFT", "Visible own draft", "DRAFT", "1.1"),
                        tuple("VIS-PUBLISHED", "Visible published", "PUBLISHED", "1.0"));
    }

    @Test
    void combinedToolAndStandardFiltersMustMatchOneVersion() {
        MasterTestCaseEntity master = newMaster("REL-COMBINED");
        ToolEntity otherTool = enabledTool("split");
        StandardTaskTypeEntity otherStandard = enabledStandard("split");
        TestCaseVersionEntity toolOnlyMatch = addVersion(
                master, 1, 0, TestCaseVersionStatus.PUBLISHED, false, "Tool-only match");
        attachTool(toolOnlyMatch, tool);
        attachMapping(toolOnlyMatch, otherStandard, "other standard");
        TestCaseVersionEntity standardOnlyMatch = addVersion(
                master, 2, 0, TestCaseVersionStatus.PUBLISHED, false, "Standard-only match");
        attachTool(standardOnlyMatch, otherTool);
        attachMapping(standardOnlyMatch, standard, "requested standard");

        var splitRelations = queryService.list(null, category.getId(), null, List.of(tool.getId()),
                List.of(standard.getId()), null, 0, 20, "updatedAt,desc", coordinator);

        assertThat(splitRelations.totalElements()).isZero();
        assertThat(splitRelations.content()).isEmpty();

        TestCaseVersionEntity combinedMatch = addVersion(
                master, 3, 2, TestCaseVersionStatus.PUBLISHED, true, "Exact combined match");
        attachTool(combinedMatch, tool);
        attachMapping(combinedMatch, standard, "same version");
        var sameVersion = queryService.list(null, category.getId(), null, List.of(tool.getId()),
                List.of(standard.getId()), null, 0, 20, "updatedAt,desc", coordinator);

        assertThat(sameVersion.totalElements()).isEqualTo(1);
        assertThat(sameVersion.content()).containsExactly(new TestCaseSummaryResponse(
                master.getId(), "REL-COMBINED", "Exact combined match", category.getId(), category.getName(),
                "PUBLISHED", 3, 2, "3.2", List.of(), true, combinedMatch.getUpdatedAt()));
    }

    @Test
    void categoryAndTagFiltersIsolateCountAndPageContent() {
        CategoryEntity filteredCategory = enabledCategory("count-filtered");
        CategoryEntity otherCategory = enabledCategory("count-other");
        TagEntity filteredTag = enabledTag("count-filtered");
        TagEntity otherTag = enabledTag("count-other");

        MasterTestCaseEntity first = newMaster("COUNT-MATCH-001", filteredCategory, user);
        MasterTestCaseEntity second = newMaster("COUNT-MATCH-002", filteredCategory, user);
        MasterTestCaseEntity third = newMaster("COUNT-MATCH-003", filteredCategory, user);
        for (MasterTestCaseEntity master : List.of(first, second, third)) {
            addVersion(master, 1, 0, TestCaseVersionStatus.PUBLISHED, true, master.getCaseCode());
            attachTag(master, filteredTag);
        }
        MasterTestCaseEntity wrongTag = newMaster("COUNT-WRONG-TAG", filteredCategory, user);
        addVersion(wrongTag, 1, 0, TestCaseVersionStatus.PUBLISHED, true, "Wrong tag");
        attachTag(wrongTag, otherTag);
        MasterTestCaseEntity wrongCategory = newMaster("COUNT-WRONG-CATEGORY", otherCategory, user);
        addVersion(wrongCategory, 1, 0, TestCaseVersionStatus.PUBLISHED, true, "Wrong category");
        attachTag(wrongCategory, filteredTag);

        var firstPage = queryService.list(null, filteredCategory.getId(), List.of(filteredTag.getId()),
                null, null, null, 0, 2, "caseCode,asc", coordinator);
        var secondPage = queryService.list(null, filteredCategory.getId(), List.of(filteredTag.getId()),
                null, null, null, 1, 2, "caseCode,asc", coordinator);

        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(secondPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.content()).extracting("id", "caseCode")
                .containsExactly(tuple(first.getId(), "COUNT-MATCH-001"), tuple(second.getId(), "COUNT-MATCH-002"));
        assertThat(secondPage.content()).extracting("id", "caseCode")
                .containsExactly(tuple(third.getId(), "COUNT-MATCH-003"));
    }

    @Test
    void databasePaginationIsStable() {
        List<MasterTestCaseEntity> masters = List.of(
                newMaster("PAGE-001"),
                newMaster("PAGE-002"),
                newMaster("PAGE-003"),
                newMaster("PAGE-004"),
                newMaster("PAGE-005"));
        masters.forEach(master -> addVersion(master, 1, 0, TestCaseVersionStatus.PUBLISHED, true, "Same Case Name"));

        var codePage0 = queryService.list(null, category.getId(), null, null, null, null, 0, 2,
                "caseCode,asc", coordinator);
        var codePage1 = queryService.list(null, category.getId(), null, null, null, null, 1, 2,
                "caseCode,asc", coordinator);

        assertThat(codePage0.totalElements()).isEqualTo(5);
        assertThat(codePage1.totalElements()).isEqualTo(codePage0.totalElements());
        assertThat(codePage0.content()).extracting("caseCode").containsExactly("PAGE-001", "PAGE-002");
        assertThat(codePage1.content()).extracting("caseCode").containsExactly("PAGE-003", "PAGE-004");
        assertThat(codePage0.content()).extracting("id")
                .doesNotContainAnyElementsOf(codePage1.content().stream().map(summary -> summary.id()).toList());
        assertThat(concatenatedIds(codePage0.content(), codePage1.content()))
                .containsExactlyInAnyOrderElementsOf(masters.subList(0, 4).stream().map(MasterTestCaseEntity::getId).toList());

        List<UUID> expectedByMasterId = masters.stream().map(MasterTestCaseEntity::getId)
                .sorted(Comparator.comparing(UUID::toString)).toList();
        var tiedPage0 = queryService.list(null, category.getId(), null, null, null, null, 0, 2,
                "caseName,asc", coordinator);
        var tiedPage1 = queryService.list(null, category.getId(), null, null, null, null, 1, 2,
                "caseName,asc", coordinator);

        assertThat(tiedPage0.totalElements()).isEqualTo(5);
        assertThat(tiedPage1.totalElements()).isEqualTo(tiedPage0.totalElements());
        assertThat(tiedPage0.content()).extracting("id")
                .containsExactlyElementsOf(expectedByMasterId.subList(0, 2));
        assertThat(tiedPage1.content()).extracting("id")
                .containsExactlyElementsOf(expectedByMasterId.subList(2, 4));
        assertThat(tiedPage0.content()).extracting("id")
                .doesNotContainAnyElementsOf(tiedPage1.content().stream().map(summary -> summary.id()).toList());
        assertThat(concatenatedIds(tiedPage0.content(), tiedPage1.content()))
                .containsExactlyElementsOf(expectedByMasterId.subList(0, 4));
    }

    @Test
    void toolAndStandardFilterReturnSameListVersion() {
        MasterTestCaseEntity master = newMaster("BLE-SEM-FILTER");
        TestCaseVersionEntity oldVersion = addVersion(master, 1, 0, TestCaseVersionStatus.PUBLISHED, false, "Alpha old");
        TestCaseVersionEntity currentVersion = addVersion(master, 2, 0, TestCaseVersionStatus.PUBLISHED, true, "Zulu current");
        ToolEntity currentTool = enabledTool("current");
        StandardTaskTypeEntity currentStandard = enabledStandard("current");
        attachTool(oldVersion, tool);
        attachMapping(oldVersion, standard, "old mapping");
        attachTool(currentVersion, currentTool);
        attachMapping(currentVersion, currentStandard, "current mapping");

        assertThat(queryService.list("alpha", category.getId(), null, null, null, null, 0, 20,
                "updatedAt,desc", coordinator).content()).singleElement()
                .extracting("caseName", "status", "versionLabel").containsExactly("Alpha old", "PUBLISHED", "1.0");
        assertThat(queryService.list(null, category.getId(), null, List.of(tool.getId()), null, null, 0, 20,
                "updatedAt,desc", coordinator).content()).singleElement()
                .extracting("caseName", "status", "versionLabel").containsExactly("Alpha old", "PUBLISHED", "1.0");
        assertThat(queryService.list(null, category.getId(), null, null, List.of(standard.getId()), null, 0, 20,
                "updatedAt,desc", coordinator).content()).singleElement()
                .extracting("caseName", "status", "versionLabel").containsExactly("Alpha old", "PUBLISHED", "1.0");
    }

    @Test
    void summaryUpdatedAtUsesVersion() {
        TestCaseDetailResponse created = draftService.createDraft(request("BLE-SEM-TIME", "Before", List.of()), coordinator);
        var before = queryService.list(null, category.getId(), null, null, null, null, 0, 20,
                "updatedAt,desc", coordinator).content().stream()
                .filter(summary -> summary.id().equals(created.id())).findFirst().orElseThrow();
        draftService.updateDraft(created.id(), new UpdateDraftRequest("After", "purpose", "preconditions",
                SelectionMode.SINGLE, false, null, null, null, List.of(), List.of(tag.getId()), List.of(tool.getId()),
                List.of(new StandardMappingRequest(standard.getId(), "note"))), coordinator);
        var after = queryService.list(null, category.getId(), null, null, null, null, 0, 20,
                "updatedAt,desc", coordinator).content().stream()
                .filter(summary -> summary.id().equals(created.id())).findFirst().orElseThrow();
        TestCaseVersionEntity persisted = versionRepository.findById(created.visibleVersion().id()).orElseThrow();
        assertThat(after.updatedAt()).isEqualTo(persisted.getUpdatedAt());
        assertThat(after.updatedAt()).isAfter(before.updatedAt());
    }

    @Test
    void disabledCategoryRejected() {
        category.setEnabled(false);
        categoryRepository.save(category);
        assertThatThrownBy(() -> draftService.createDraft(request("BLE-DISABLED-CAT", "Disabled Category", List.of()), coordinator))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void disabledTagRejected() {
        tag.setEnabled(false);
        tagRepository.save(tag);
        assertDisabledReferenceRejected(request("BLE-DISABLED-TAG", "Disabled Tag", List.of()));
    }

    @Test
    void disabledToolRejected() {
        tool.setEnabled(false);
        toolRepository.save(tool);
        assertDisabledReferenceRejected(request("BLE-DISABLED-TOOL", "Disabled Tool", List.of()));
    }

    @Test
    void disabledStandardRejected() {
        standard.setEnabled(false);
        standardRepository.save(standard);
        assertDisabledReferenceRejected(request("BLE-DISABLED-STD", "Disabled Standard", List.of()));
    }

    private void assertDisabledReferenceRejected(CreateDraftRequest base) {
        CreateDraftRequest request = new CreateDraftRequest(base.caseCode(), base.categoryId(), base.caseName(), base.testPurpose(),
                base.preconditions(), base.selectionMode(), base.evidenceRequired(), base.evidenceRequirement(), base.remarkRequirement(),
                base.progressiveRole(), base.steps(), List.of(tag.getId()), List.of(tool.getId()),
                List.of(new StandardMappingRequest(standard.getId(), "note")));
        assertThatThrownBy(() -> draftService.createDraft(request, coordinator)).isInstanceOf(ValidationException.class);
    }

    private CreateDraftRequest request(String code, String name, List<StepRequest> steps) {
        return new CreateDraftRequest(code, category.getId(), name, "purpose", "preconditions", SelectionMode.SINGLE,
                false, null, "notes", null, steps, List.of(tag.getId()), List.of(tool.getId()),
                List.of(new StandardMappingRequest(standard.getId(), "pairing baseline")));
    }

    private TagEntity enabledTag(String label) {
        TagEntity entity = new TagEntity();
        entity.setCode("it-tag-" + label + "-" + UUID.randomUUID().toString().substring(0, 8));
        entity.setName("IT Tag " + label);
        return tagRepository.save(entity);
    }

    private ToolEntity enabledTool(String label) {
        ToolEntity entity = new ToolEntity();
        entity.setCode("it-tool-" + label + "-" + UUID.randomUUID().toString().substring(0, 8));
        entity.setName("IT Tool " + label);
        return toolRepository.save(entity);
    }

    private StandardTaskTypeEntity enabledStandard(String label) {
        StandardTaskTypeEntity entity = new StandardTaskTypeEntity();
        entity.setCode("it-standard-" + label + "-" + UUID.randomUUID().toString().substring(0, 8));
        entity.setName("IT Standard " + label);
        entity.setType("STANDARD");
        return standardRepository.save(entity);
    }

    private CategoryEntity enabledCategory(String label) {
        CategoryEntity entity = new CategoryEntity();
        entity.setCode("it-cat-" + label + "-" + UUID.randomUUID().toString().substring(0, 8));
        entity.setName("IT Category " + label);
        entity.setLevel(1);
        return categoryRepository.save(entity);
    }

    private MasterTestCaseEntity newMaster(String code) {
        return newMaster(code, category, user);
    }

    private MasterTestCaseEntity newMaster(String code, CategoryEntity referencedCategory, UserEntity owner) {
        MasterTestCaseEntity master = new MasterTestCaseEntity();
        master.setCaseCode(code);
        master.setCategory(referencedCategory);
        master.setCreatedBy(owner);
        return masterRepository.saveAndFlush(master);
    }

    private TestCaseVersionEntity addVersion(MasterTestCaseEntity master, int major, int minor,
                                               TestCaseVersionStatus status, boolean current, String caseName) {
        return addVersion(master, major, minor, status, current, caseName, user);
    }

    private TestCaseVersionEntity addVersion(MasterTestCaseEntity master, int major, int minor,
                                               TestCaseVersionStatus status, boolean current, String caseName,
                                               UserEntity owner) {
        TestCaseVersionEntity version = new TestCaseVersionEntity();
        version.setMasterTestCase(master);
        version.setVersionMajor(major);
        version.setVersionMinor(minor);
        version.setStatus(status);
        version.setCurrentVersion(current);
        version.setCaseName(caseName);
        version.setSelectionMode(SelectionMode.SINGLE);
        version.setEvidenceRequired(false);
        version.setCreatedBy(owner);
        version.setRevisionClosed(status != TestCaseVersionStatus.DRAFT);
        return versionRepository.saveAndFlush(version);
    }

    private void attachTag(MasterTestCaseEntity master, TagEntity referencedTag) {
        TestCaseTagEntity relation = new TestCaseTagEntity();
        relation.setMasterTestCase(master);
        relation.setTag(referencedTag);
        caseTagRepository.saveAndFlush(relation);
    }

    private void attachTool(TestCaseVersionEntity version, ToolEntity referencedTool) {
        TestCaseToolEntity relation = new TestCaseToolEntity();
        relation.setTestCaseVersion(version);
        relation.setTool(referencedTool);
        relation.setSortOrder(0);
        caseToolRepository.saveAndFlush(relation);
    }

    private void attachMapping(TestCaseVersionEntity version, StandardTaskTypeEntity referencedStandard, String note) {
        TestCaseStandardMappingEntity relation = new TestCaseStandardMappingEntity();
        relation.setTestCaseVersion(version);
        relation.setStandardTaskType(referencedStandard);
        relation.setMappingNote(note);
        mappingRepository.saveAndFlush(relation);
    }

    private static List<UUID> concatenatedIds(List<TestCaseSummaryResponse> first,
                                              List<TestCaseSummaryResponse> second) {
        return java.util.stream.Stream.concat(first.stream(), second.stream()).map(TestCaseSummaryResponse::id).toList();
    }
}

import { httpClient } from '@/shared/api/httpClient';
import type {
  Contributor,
  PagedResponse,
  ReviewRecord,
  TestCaseDetail,
  TestCaseSummary,
  TestCaseVersion,
  VersionSummary,
  SelectionMode,
  ProgressiveRole,
  DecisionPoint,
  MasterLogicGraph,
  TransitionType,
} from '@/shared/types/testCase';

const TEST_CASE_BASE = '/test-cases';

export interface StandardMappingPayload { standardTaskTypeId: string; mappingNote?: string }
export interface StepPayload { title?: string; content: string }
export interface DraftPayload {
  caseCode?: string;
  categoryId?: string;
  caseName: string;
  testPurpose?: string;
  preconditions?: string;
  selectionMode: SelectionMode;
  evidenceRequired?: boolean;
  evidenceRequirement?: string;
  remarkRequirement?: string;
  progressiveRole?: ProgressiveRole;
  steps?: StepPayload[];
  tagIds?: string[];
  toolIds?: string[];
  standardMappings?: StandardMappingPayload[];
}
export interface TestCaseListParams {
  q?: string;
  categoryId?: string;
  tagIds?: string[];
  toolIds?: string[];
  standardTaskTypeIds?: string[];
  status?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export async function listTestCases(params?: TestCaseListParams): Promise<PagedResponse<TestCaseSummary>> {
  const response = await httpClient.get<PagedResponse<TestCaseSummary>>(TEST_CASE_BASE, { params });
  return response.data;
}
export async function createTestCase(payload: DraftPayload): Promise<TestCaseDetail> {
  const response = await httpClient.post<TestCaseDetail>(TEST_CASE_BASE, payload);
  return response.data;
}
export async function getTestCase(masterId: string): Promise<TestCaseDetail> {
  const response = await httpClient.get<TestCaseDetail>(`${TEST_CASE_BASE}/${masterId}`);
  return response.data;
}
export async function updateTestCaseDraft(masterId: string, payload: Omit<DraftPayload, 'caseCode' | 'categoryId'>): Promise<TestCaseDetail> {
  const response = await httpClient.put<TestCaseDetail>(`${TEST_CASE_BASE}/${masterId}/draft`, payload);
  return response.data;
}
export async function listTestCaseVersions(masterId: string): Promise<VersionSummary[]> {
  const response = await httpClient.get<VersionSummary[]>(`${TEST_CASE_BASE}/${masterId}/versions`);
  return response.data;
}
export async function getTestCaseVersion(masterId: string, versionId: string): Promise<TestCaseVersion> {
  const response = await httpClient.get<TestCaseVersion>(`${TEST_CASE_BASE}/${masterId}/versions/${versionId}`);
  return response.data;
}

// ---------------------------------------------------------------------------
// Phase 7 — Test Case Lifecycle
// ---------------------------------------------------------------------------

/** Optional comment payload for submit/publish/return/reject/deprecate. */
export interface LifecycleActionPayload { comment?: string }
/** Create-revision payload; sourceVersionId null → server uses current PUBLISHED. */
export interface CreateRevisionPayload { sourceVersionId?: string | null; changeReason?: string | null }

export async function submitReview(masterId: string, payload: LifecycleActionPayload = {}): Promise<TestCaseDetail> {
  const response = await httpClient.post<TestCaseDetail>(`${TEST_CASE_BASE}/${masterId}/draft/submit-review`, payload);
  return response.data;
}
export async function publishVersion(masterId: string, versionId: string, payload: LifecycleActionPayload = {}): Promise<TestCaseDetail> {
  const response = await httpClient.post<TestCaseDetail>(`${TEST_CASE_BASE}/${masterId}/versions/${versionId}/publish`, payload);
  return response.data;
}
export async function returnReview(masterId: string, versionId: string, payload: LifecycleActionPayload = {}): Promise<TestCaseDetail> {
  const response = await httpClient.post<TestCaseDetail>(`${TEST_CASE_BASE}/${masterId}/versions/${versionId}/return`, payload);
  return response.data;
}
export async function rejectVersion(masterId: string, versionId: string, payload: LifecycleActionPayload = {}): Promise<TestCaseDetail> {
  const response = await httpClient.post<TestCaseDetail>(`${TEST_CASE_BASE}/${masterId}/versions/${versionId}/reject`, payload);
  return response.data;
}
export async function deprecateVersion(masterId: string, versionId: string, payload: LifecycleActionPayload = {}): Promise<TestCaseDetail> {
  const response = await httpClient.post<TestCaseDetail>(`${TEST_CASE_BASE}/${masterId}/versions/${versionId}/deprecate`, payload);
  return response.data;
}
export async function createRevision(masterId: string, payload: CreateRevisionPayload = {}): Promise<TestCaseDetail> {
  const response = await httpClient.post<TestCaseDetail>(`${TEST_CASE_BASE}/${masterId}/revisions`, payload);
  return response.data;
}
export async function getReviewRecords(masterId: string, versionId: string): Promise<ReviewRecord[]> {
  const response = await httpClient.get<ReviewRecord[]>(`${TEST_CASE_BASE}/${masterId}/versions/${versionId}/review-records`);
  return response.data;
}
export async function listContributors(masterId: string): Promise<Contributor[]> {
  const response = await httpClient.get<Contributor[]>(`${TEST_CASE_BASE}/${masterId}/draft/contributors`);
  return response.data;
}
export async function addContributor(masterId: string, userId: string): Promise<Contributor[]> {
  const response = await httpClient.post<Contributor[]>(`${TEST_CASE_BASE}/${masterId}/draft/contributors`, { userId });
  return response.data;
}
export async function removeContributor(masterId: string, userId: string): Promise<Contributor[]> {
  const response = await httpClient.delete<Contributor[]>(`${TEST_CASE_BASE}/${masterId}/draft/contributors/${userId}`);
  return response.data;
}

// ---------------------------------------------------------------------------
// Phase 8 — Decision Point / Master Logic Graph
// ---------------------------------------------------------------------------

export interface DecisionPointPayload {
  name: string;
  description?: string;
  displayOrder: number;
  transitionType: TransitionType;
  targetMasterTestCaseIds?: string[];
}
const versionLogicBase = (masterId: string, versionId: string) => `${TEST_CASE_BASE}/${masterId}/versions/${versionId}`;
export async function listDecisionPoints(masterId: string, versionId: string): Promise<DecisionPoint[]> {
  const response = await httpClient.get<DecisionPoint[]>(`${versionLogicBase(masterId, versionId)}/decision-points`);
  return response.data;
}
export async function createDecisionPoint(masterId: string, versionId: string, payload: DecisionPointPayload): Promise<DecisionPoint> {
  const response = await httpClient.post<DecisionPoint>(`${versionLogicBase(masterId, versionId)}/decision-points`, payload);
  return response.data;
}
export async function updateDecisionPoint(masterId: string, versionId: string, pointId: string, payload: DecisionPointPayload): Promise<DecisionPoint> {
  const response = await httpClient.put<DecisionPoint>(`${versionLogicBase(masterId, versionId)}/decision-points/${pointId}`, payload);
  return response.data;
}
export async function deleteDecisionPoint(masterId: string, versionId: string, pointId: string): Promise<void> {
  await httpClient.delete(`${versionLogicBase(masterId, versionId)}/decision-points/${pointId}`);
}
export async function getMasterLogicGraph(masterId: string, versionId: string): Promise<MasterLogicGraph> {
  const response = await httpClient.get<MasterLogicGraph>(`${versionLogicBase(masterId, versionId)}/logic-graph`);
  return response.data;
}

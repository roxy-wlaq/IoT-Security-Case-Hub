import { httpClient } from '@/shared/api/httpClient';
import type { PagedResponse, TestCaseDetail, TestCaseSummary, TestCaseVersion, VersionSummary, SelectionMode, ProgressiveRole } from '@/shared/types/testCase';

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

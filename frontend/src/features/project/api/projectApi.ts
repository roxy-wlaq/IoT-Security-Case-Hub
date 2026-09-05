import { httpClient } from '@/shared/api/httpClient';
import type { GenerationRun, GenerationRunMode, GenerationRule, Project, ProjectCapability, ProjectSummary, ProjectTestCase } from '@/shared/types/project';

export type CapabilityRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export type ChangeRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export interface CustomStep { id?: string; sequenceNo: number; title?: string; content: string }
export interface CustomDecisionPoint { id?: string; displayOrder: number; name: string; description?: string; transitionType?: string; targetMasterTestCaseIds?: string[]; targetCustomTestCaseIds?: string[]; targets?: { masterTestCaseId?: string; customTestCaseId?: string }[] }
export interface CustomTestCase { id: string; projectId: string; caseCode: string; caseName: string; testPurpose?: string; preconditions?: string; selectionMode: 'SINGLE' | 'MULTIPLE'; evidenceRequired: boolean; evidenceRequirement?: string; remarkRequirement?: string; projectTestCaseId?: string; createdBy: string; steps: CustomStep[]; decisionPoints: CustomDecisionPoint[]; createdAt: string; updatedAt: string }
export interface CapabilityUpdateRequest { id: string; projectId: string; capabilityId: string; currentValue: string; proposedValue: string; reason: string; evidenceReference?: string; submittedBy: string; reviewedBy?: string; status: CapabilityRequestStatus; createdAt: string; updatedAt: string }
export interface TestCaseChangeRequest { id: string; masterTestCaseId: string; sourceVersionId: string; reason: string; submittedBy: string; reviewedBy?: string; revisionDraftVersionId?: string; status: ChangeRequestStatus; createdAt: string; updatedAt: string }
export interface VersionAvailability { projectTestCaseId: string; masterTestCaseId?: string; boundVersionId?: string; currentPublishedVersionId?: string; newVersionAvailable: boolean; executionStatus: string; diff: { changedFields: string[]; logicChanged: boolean; compatible: boolean; warning?: string } }
export interface VersionUpgradeResult { projectTestCaseId: string; previousVersionId?: string; currentVersionId?: string; upgraded: boolean; diff: VersionAvailability['diff'] }

export async function listProjects(): Promise<ProjectSummary[]> { return (await httpClient.get<ProjectSummary[]>('/projects')).data; }
export async function getProject(id: string): Promise<Project> { return (await httpClient.get<Project>(`/projects/${id}`)).data; }
export async function downloadProjectExport(id: string): Promise<Blob> {
  return (await httpClient.get<Blob>(`/projects/${id}/export.xlsx`, { responseType: 'blob' })).data;
}
export async function createProject(payload: { projectName: string; deviceName: string; generationMode?: string; standardTaskTypeIds: string[] }): Promise<Project> {
  return (await httpClient.post<Project>('/projects', payload)).data;
}
export async function listProjectCapabilities(id: string): Promise<ProjectCapability[]> { return (await httpClient.get<ProjectCapability[]>(`/projects/${id}/capabilities`)).data; }
export async function updateProjectCapability(id: string, capabilityId: string, payload: { value: string; source: string; comment?: string }): Promise<ProjectCapability> {
  return (await httpClient.put<ProjectCapability>(`/projects/${id}/capabilities/${capabilityId}`, payload)).data;
}
export async function runGeneration(projectId: string, mode: GenerationRunMode): Promise<GenerationRun> {
  return (await httpClient.post<GenerationRun>(`/projects/${projectId}/generation/runs`, { mode, triggerType: 'MANUAL_REGENERATE' })).data;
}
export async function listGenerationRuns(projectId: string): Promise<GenerationRun[]> { return (await httpClient.get<GenerationRun[]>(`/projects/${projectId}/generation/runs`)).data; }
export async function addRecommendation(projectId: string, recommendationId: string): Promise<void> { await httpClient.post(`/projects/${projectId}/generation/recommendations/${recommendationId}/add`); }
export async function ignoreRecommendation(projectId: string, recommendationId: string): Promise<void> { await httpClient.post(`/projects/${projectId}/generation/recommendations/${recommendationId}/ignore`, { ignored: true }); }
export async function listGenerationRules(): Promise<GenerationRule[]> { return (await httpClient.get<GenerationRule[]>('/generation-rules')).data; }
export async function createGenerationRule(payload: { ruleCode: string; name: string; mode: string; status: string; groups: { parentGroupIndex: null; logicOperator: 'AND' | 'OR'; sortOrder: number; conditions: { targetType: 'STANDARD_TASK_TYPE'; standardTaskTypeId: string; operator: 'ANY'; sortOrder: number }[] }[]; outputMasterTestCaseIds: string[] }): Promise<GenerationRule> { return (await httpClient.post<GenerationRule>('/generation-rules', payload)).data; }
export async function listProjectTestPlan(projectId: string): Promise<ProjectTestCase[]> { return (await httpClient.get<ProjectTestCase[]>(`/projects/${projectId}/test-plan`)).data; }
export async function addProjectTestCase(projectId: string, masterTestCaseId: string): Promise<ProjectTestCase> { return (await httpClient.post<ProjectTestCase>(`/projects/${projectId}/test-plan`, null, { params: { masterTestCaseId, source: 'MANUAL' } })).data; }
export async function removeProjectTestCase(projectId: string, id: string): Promise<ProjectTestCase> { return (await httpClient.post<ProjectTestCase>(`/projects/${projectId}/test-plan/${id}/remove`)).data; }
export async function restoreProjectTestCase(projectId: string, id: string): Promise<ProjectTestCase> { return (await httpClient.post<ProjectTestCase>(`/projects/${projectId}/test-plan/${id}/restore`)).data; }
export async function assignProjectTestCase(projectId: string, id: string, userId: string): Promise<ProjectTestCase> { return (await httpClient.post<ProjectTestCase>(`/projects/${projectId}/test-plan/${id}/assignees`, { userId })).data; }
export async function listCustomTestCases(projectId: string): Promise<CustomTestCase[]> { return (await httpClient.get<CustomTestCase[]>(`/projects/${projectId}/custom-test-cases`)).data; }
export async function createCustomTestCase(projectId: string, payload: Record<string, unknown>): Promise<CustomTestCase> { return (await httpClient.post<CustomTestCase>(`/projects/${projectId}/custom-test-cases`, payload)).data; }
export async function updateCustomTestCase(projectId: string, id: string, payload: Record<string, unknown>): Promise<CustomTestCase> { return (await httpClient.put<CustomTestCase>(`/projects/${projectId}/custom-test-cases/${id}`, payload)).data; }
export async function assignCustomTestCase(projectId: string, id: string, userId: string): Promise<void> { await httpClient.post(`/projects/${projectId}/custom-test-cases/${id}/assignees/${userId}`); }
export async function submitCustomTestCaseToLibrary(projectId: string, id: string): Promise<{ masterTestCaseId: string; draftVersionId: string; contributorId?: string }> { return (await httpClient.post(`/projects/${projectId}/custom-test-cases/${id}/submit-to-library`)).data; }
export async function listCapabilityUpdateRequests(projectId: string): Promise<CapabilityUpdateRequest[]> { return (await httpClient.get<CapabilityUpdateRequest[]>(`/projects/${projectId}/capability-update-requests`)).data; }
export async function submitCapabilityUpdateRequest(projectId: string, capabilityId: string, payload: { proposedValue: string; reason: string; evidenceReference?: string }): Promise<CapabilityUpdateRequest> { return (await httpClient.post<CapabilityUpdateRequest>(`/projects/${projectId}/capability-update-requests/${capabilityId}`, payload)).data; }
export async function reviewCapabilityUpdateRequest(projectId: string, id: string, approved: boolean, comment?: string): Promise<CapabilityUpdateRequest> { return (await httpClient.post<CapabilityUpdateRequest>(`/projects/${projectId}/capability-update-requests/${id}/${approved ? 'approve' : 'reject'}`, { comment })).data; }
export async function listTestCaseChangeRequests(masterId: string): Promise<TestCaseChangeRequest[]> { return (await httpClient.get<TestCaseChangeRequest[]>(`/test-cases/${masterId}/change-requests`)).data; }
export async function submitTestCaseChangeRequest(masterId: string, payload: { sourceVersionId: string; reason: string }): Promise<TestCaseChangeRequest> { return (await httpClient.post<TestCaseChangeRequest>(`/test-cases/${masterId}/change-requests`, payload)).data; }
export async function reviewTestCaseChangeRequest(masterId: string, id: string, approved: boolean, comment?: string): Promise<TestCaseChangeRequest> { return (await httpClient.post<TestCaseChangeRequest>(`/test-cases/${masterId}/change-requests/${id}/${approved ? 'approve' : 'reject'}`, { comment })).data; }
export async function getVersionAvailability(projectTestCaseId: string): Promise<VersionAvailability> { return (await httpClient.get<VersionAvailability>(`/project-test-cases/${projectTestCaseId}/version`)).data; }
export async function keepProjectTestCaseVersion(projectTestCaseId: string): Promise<VersionUpgradeResult> { return (await httpClient.post<VersionUpgradeResult>(`/project-test-cases/${projectTestCaseId}/version/keep`)).data; }
export async function upgradeProjectTestCaseVersion(projectTestCaseId: string): Promise<VersionUpgradeResult> { return (await httpClient.post<VersionUpgradeResult>(`/project-test-cases/${projectTestCaseId}/version/upgrade`)).data; }

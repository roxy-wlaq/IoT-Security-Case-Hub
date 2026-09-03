import { httpClient } from '@/shared/api/httpClient';
import type { GenerationRun, GenerationRunMode, GenerationRule, Project, ProjectCapability, ProjectSummary, ProjectTestCase } from '@/shared/types/project';

export async function listProjects(): Promise<ProjectSummary[]> { return (await httpClient.get<ProjectSummary[]>('/projects')).data; }
export async function getProject(id: string): Promise<Project> { return (await httpClient.get<Project>(`/projects/${id}`)).data; }
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
export async function listProjectTestPlan(projectId: string): Promise<ProjectTestCase[]> { return (await httpClient.get<ProjectTestCase[]>(`/projects/${projectId}/test-plan`)).data; }
export async function addProjectTestCase(projectId: string, masterTestCaseId: string): Promise<ProjectTestCase> { return (await httpClient.post<ProjectTestCase>(`/projects/${projectId}/test-plan`, null, { params: { masterTestCaseId, source: 'MANUAL' } })).data; }
export async function removeProjectTestCase(projectId: string, id: string): Promise<ProjectTestCase> { return (await httpClient.post<ProjectTestCase>(`/projects/${projectId}/test-plan/${id}/remove`)).data; }
export async function restoreProjectTestCase(projectId: string, id: string): Promise<ProjectTestCase> { return (await httpClient.post<ProjectTestCase>(`/projects/${projectId}/test-plan/${id}/restore`)).data; }
export async function assignProjectTestCase(projectId: string, id: string, userId: string): Promise<ProjectTestCase> { return (await httpClient.post<ProjectTestCase>(`/projects/${projectId}/test-plan/${id}/assignees`, { userId })).data; }

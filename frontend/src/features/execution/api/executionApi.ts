import { httpClient } from '@/shared/api/httpClient';

export type ExecutionStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED';
export type RelationStatus = 'CONNECTED' | 'FLOATING';
export type SelectionMode = 'SINGLE' | 'MULTIPLE';
export interface Evidence { id: string; projectTestCaseId: string; originalFilename: string; fileSize: number; contentType?: string; sha256: string; uploadedBy: string; createdAt: string }
export interface Note { id: string; projectTestCaseId: string; authorId: string; authorName: string; body: string; createdAt: string; updatedAt: string; editable: boolean }
export interface ExecutionDetail { projectTestCaseId: string; projectId: string; testCaseVersionId: string; executionStatus: ExecutionStatus; selectionMode: SelectionMode; evidenceRequired: boolean; decisionPoints: { id: string; displayOrder: number; name: string; transitionType: string | null; targetMasterTestCaseIds: string[] }[] }
export interface ExecutionResult { projectTestCaseId: string; executionStatus: ExecutionStatus; selectedDecisionPointIds: string[]; branchOutcomes: { decisionPointId: string; transitionType: string; targetMasterTestCaseId: string | null }[]; affectedTargetProjectTestCaseIds: string[] }
export interface ProjectLogicGraph { nodes: { projectTestCaseId: string; masterTestCaseId: string; caseCode: string; testCaseVersionId: string; executionStatus: ExecutionStatus; relationStatus: RelationStatus; root: boolean; assignees: string[] }[]; edges: { id: string; sourceProjectTestCaseId: string; targetProjectTestCaseId: string; sourceDecisionPointId: string; label: string }[] }

export const executionApi = {
  detail: (id: string) => httpClient.get<ExecutionDetail>(`/project-test-cases/${id}/execution`).then((r) => r.data),
  evidence: (id: string) => httpClient.get<Evidence[]>(`/project-test-cases/${id}/evidence`).then((r) => r.data),
  downloadEvidence: async (id: string, evidenceId: string, filename: string) => {
    const response = await httpClient.get<Blob>(`/project-test-cases/${id}/evidence/${evidenceId}/download`, { responseType: 'blob' });
    const url = URL.createObjectURL(response.data);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    anchor.click();
    URL.revokeObjectURL(url);
  },
  uploadEvidence: (id: string, file: File) => { const form = new FormData(); form.append('file', file); return httpClient.post<Evidence>(`/project-test-cases/${id}/evidence`, form, { headers: { 'Content-Type': 'multipart/form-data' } }).then((r) => r.data); },
  deleteEvidence: (id: string, evidenceId: string) => httpClient.delete(`/project-test-cases/${id}/evidence/${evidenceId}`),
  notes: (id: string) => httpClient.get<Note[]>(`/project-test-cases/${id}/notes`).then((r) => r.data),
  createNote: (id: string, body: string) => httpClient.post<Note>(`/project-test-cases/${id}/notes`, { body }).then((r) => r.data),
  updateNote: (id: string, noteId: string, body: string) => httpClient.patch<Note>(`/project-test-cases/${id}/notes/${noteId}`, { body }).then((r) => r.data),
  deleteNote: (id: string, noteId: string) => httpClient.delete(`/project-test-cases/${id}/notes/${noteId}`),
  start: (id: string) => httpClient.post(`/project-test-cases/${id}/execution/start`).then((r) => r.data as { executionStatus: ExecutionStatus }),
  complete: (id: string, selectedDecisionPointIds: string[]) => httpClient.post<ExecutionResult>(`/project-test-cases/${id}/execution/complete`, { selectedDecisionPointIds }).then((r) => r.data),
  reopen: (id: string) => httpClient.post(`/project-test-cases/${id}/execution/reopen`).then((r) => r.data as { executionStatus: ExecutionStatus }),
  graph: (projectId: string) => httpClient.get<ProjectLogicGraph>(`/projects/${projectId}/logic-graph`).then((r) => r.data),
};

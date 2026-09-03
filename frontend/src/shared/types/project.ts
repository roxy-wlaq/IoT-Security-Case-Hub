export type ProjectGenerationMode = 'FULL' | 'PROGRESSIVE';
export type ProjectStatus = 'DRAFT' | 'ACTIVE' | 'COMPLETED' | 'ARCHIVED';
export type CapabilityValue = 'YES' | 'NO' | 'UNKNOWN';
export type CapabilitySource = 'CUSTOMER_PROVIDED' | 'TESTER_DISCOVERED' | 'DOCUMENT' | 'AUTOMATIC_DETECTION' | 'COORDINATOR_INPUT' | 'OTHER';

export interface ProjectSummary {
  id: string; projectNumber: string; projectName: string; deviceName: string;
  generationMode: ProjectGenerationMode; status: ProjectStatus; createdAt: string;
}
export interface ProjectCoordinator { userId: string; username: string; displayName: string; primary: boolean }
export interface Project {
  id: string; projectNumber: string; projectName: string; deviceName: string;
  generationMode: ProjectGenerationMode; status: ProjectStatus; createdBy: string;
  standardTaskTypeIds: string[]; coordinators: ProjectCoordinator[]; createdAt: string; updatedAt: string;
}
export interface ProjectCapability {
  capabilityId: string; parentId: string | null; code: string; name: string; value: CapabilityValue;
  source: CapabilitySource | null; derived: boolean; comment: string | null; updatedAt: string | null;
}
export type GenerationRunMode = 'FULL' | 'PROGRESSIVE_INITIAL';
export interface Recommendation {
  id: string; runId: string; masterTestCaseId: string; caseCode: string; resolvedVersionId: string;
  status: 'NEW' | 'ADDED' | 'IGNORED'; recommendedBecause: { ruleId: string; ruleCode: string; ruleName: string }[];
}
export interface GenerationRun { id: string; projectId: string; mode: GenerationRunMode; triggerType: string; executedAt: string; recommendations: Recommendation[] }
export interface GenerationRule { id: string; ruleCode: string; name: string; description?: string; mode: string; status: string; groups: unknown[]; outputMasterTestCaseIds: string[] }
export interface ProjectTestCase { id: string; masterTestCaseId: string; testCaseVersionId: string; caseCode: string; removed: boolean; sources: string[]; assignees: { userId: string; username: string; displayName: string; firstViewedAt: string | null }[] }

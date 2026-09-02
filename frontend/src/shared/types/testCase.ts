export type TestCaseStatus = 'DRAFT' | 'REVIEW' | 'PUBLISHED' | 'DEPRECATED';
export type SelectionMode = 'SINGLE' | 'MULTIPLE';
export type ProgressiveRole = 'ENTRY' | 'NORMAL';

export interface TagRef { id: string; code: string; name: string }
export interface ToolRef { id: string; code: string; name: string }
export interface StandardMappingRef {
  standardTaskTypeId: string;
  standardCode: string;
  standardName: string;
  mappingNote?: string | null;
}
export interface TestStep { id: string; sequenceNo: number; title?: string | null; content: string }
export interface AttachmentRef {
  id: string;
  originalFilename: string;
  fileSize: number;
  contentType?: string | null;
  description?: string | null;
  uploadedBy: string;
  createdAt: string;
}
export interface VersionSummary {
  id: string;
  versionLabel: string;
  versionMajor: number;
  versionMinor: number;
  status: TestCaseStatus;
  isCurrentVersion: boolean;
  changeReason?: string | null;
  createdBy: string;
  publishedAt?: string | null;
  createdAt: string;
}
export interface TestCaseVersion {
  id: string;
  masterTestCaseId: string;
  versionLabel: string;
  versionMajor: number;
  versionMinor: number;
  status: TestCaseStatus;
  isCurrentVersion: boolean;
  caseName: string;
  testPurpose?: string | null;
  preconditions?: string | null;
  selectionMode: SelectionMode;
  evidenceRequired: boolean;
  evidenceRequirement?: string | null;
  remarkRequirement?: string | null;
  progressiveRole?: ProgressiveRole | null;
  basedOnVersionId?: string | null;
  changeReason?: string | null;
  createdBy: string;
  reviewedBy?: string | null;
  publishedAt?: string | null;
  deprecatedAt?: string | null;
  revisionClosed: boolean;
  steps: TestStep[];
  tools: ToolRef[];
  standardMappings: StandardMappingRef[];
  attachments: AttachmentRef[];
  createdAt: string;
  updatedAt: string;
}
export interface TestCaseSummary {
  id: string;
  caseCode: string;
  caseName: string;
  categoryId: string;
  categoryName: string;
  status: TestCaseStatus;
  versionMajor: number;
  versionMinor: number;
  versionLabel: string;
  tags: TagRef[];
  enabled: boolean;
  updatedAt: string;
}
export interface AllowedActions { editDraft: boolean; createDraft: boolean }
export interface TestCaseDetail {
  id: string;
  caseCode: string;
  categoryId: string;
  categoryName: string;
  createdBy: string;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
  tags: TagRef[];
  currentVersion?: TestCaseVersion | null;
  draftVersion?: TestCaseVersion | null;
  visibleVersion: TestCaseVersion;
  versions: VersionSummary[];
  allowedActions: AllowedActions;
}
export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

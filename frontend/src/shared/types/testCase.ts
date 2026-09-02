export type TestCaseStatus = 'DRAFT' | 'REVIEW' | 'PUBLISHED' | 'DEPRECATED';
export type SelectionMode = 'SINGLE' | 'MULTIPLE';
export type ProgressiveRole = 'ENTRY' | 'NORMAL';

/**
 * Review-record action values (Phase 7). Mirrors the backend
 * {@code ReviewRecordAction} enum. There is deliberately NO "REJECTED" status —
 * a rejected version stays in REVIEW and a REJECT review record is appended.
 */
export type ReviewRecordAction = 'SUBMIT' | 'PUBLISH' | 'RETURN' | 'REJECT' | 'DEPRECATE';

export interface ReviewRecord {
  id: string;
  testCaseVersionId: string;
  action: ReviewRecordAction;
  reviewerId: string;
  reviewerName: string;
  comment?: string | null;
  createdAt: string;
}

export interface Contributor {
  id: string;
  userId: string;
  username: string;
  displayName: string;
  addedBy: string;
  createdAt: string;
}

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
  /** Latest review-record action on this version; drives the UI "Rejected" label. */
  latestReviewAction?: ReviewRecordAction | null;
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
/**
 * Server-computed per-version action flags (Phase 7). The UI MUST render its
 * action bar from these flags and never from raw role strings. Field semantics:
 * editDraft/submitReview → DRAFT ∧ revision_closed=false ∧ (ADMIN∨owner∨contributor);
 * publish/returnReview/reject → REVIEW ∧ revision_closed=false ∧ required permission;
 * deprecate → PUBLISHED ∧ test_case:deprecate; createRevision → current PUBLISHED exists ∧
 * test_case:draft_create; manageContributors → DRAFT ∧ (ADMIN∨owner) ∧ test_case:draft_edit.
 */
export interface AllowedActions {
  editDraft: boolean;
  createDraft: boolean;
  submitReview: boolean;
  publish: boolean;
  returnReview: boolean;
  reject: boolean;
  deprecate: boolean;
  createRevision: boolean;
  manageContributors: boolean;
}
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

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { UseMutationResult, UseQueryResult } from '@tanstack/react-query';
import {
  addContributor,
  createRevision,
  createTestCase,
  deprecateVersion,
  getReviewRecords,
  getTestCase,
  getTestCaseVersion,
  listContributors,
  listTestCaseVersions,
  listTestCases,
  publishVersion,
  rejectVersion,
  removeContributor,
  returnReview,
  submitReview,
  updateTestCaseDraft,
} from '@/features/testcase/api/testCaseApi';
import type {
  CreateRevisionPayload,
  DraftPayload,
  LifecycleActionPayload,
  TestCaseListParams,
} from '@/features/testcase/api/testCaseApi';
import type { ApiError } from '@/shared/api/apiError';
import type {
  Contributor,
  PagedResponse,
  ReviewRecord,
  TestCaseDetail,
  TestCaseSummary,
  TestCaseVersion,
  VersionSummary,
} from '@/shared/types/testCase';

export const testCasesQueryKey = ['testCases'] as const;
export const testCaseDetailQueryKey = (masterId: string) => ['testCaseDetail', masterId] as const;
export const testCaseVersionsQueryKey = (masterId: string) => ['testCaseVersions', masterId] as const;
export const testCaseVersionQueryKey = (masterId: string, versionId: string) => ['testCaseVersion', masterId, versionId] as const;
export const reviewRecordsQueryKey = (masterId: string, versionId: string) => ['reviewRecords', masterId, versionId] as const;
export const contributorsQueryKey = (masterId: string) => ['contributors', masterId] as const;

/** Invalidate every cache slice that a lifecycle action can change. */
function invalidateTestCaseCaches(client: ReturnType<typeof useQueryClient>, masterId: string) {
  void client.invalidateQueries({ queryKey: testCasesQueryKey });
  void client.invalidateQueries({ queryKey: testCaseDetailQueryKey(masterId) });
  void client.invalidateQueries({ queryKey: testCaseVersionsQueryKey(masterId) });
}

export function useTestCases(params?: TestCaseListParams): UseQueryResult<PagedResponse<TestCaseSummary>, ApiError> {
  return useQuery({ queryKey: [...testCasesQueryKey, params ?? {}], queryFn: () => listTestCases(params) });
}
export function useTestCase(masterId: string, enabled = true): UseQueryResult<TestCaseDetail, ApiError> {
  return useQuery({ queryKey: testCaseDetailQueryKey(masterId), queryFn: () => getTestCase(masterId), enabled });
}
export function useTestCaseVersions(masterId: string): UseQueryResult<VersionSummary[], ApiError> {
  return useQuery({ queryKey: testCaseVersionsQueryKey(masterId), queryFn: () => listTestCaseVersions(masterId) });
}
export function useTestCaseVersion(masterId: string, versionId: string, enabled = true): UseQueryResult<TestCaseVersion, ApiError> {
  return useQuery({ queryKey: testCaseVersionQueryKey(masterId, versionId), queryFn: () => getTestCaseVersion(masterId, versionId), enabled: enabled && Boolean(masterId) && Boolean(versionId) });
}
export function useCreateTestCase(): UseMutationResult<TestCaseDetail, ApiError, DraftPayload> {
  const client = useQueryClient();
  return useMutation({ mutationFn: createTestCase, onSuccess: (data) => {
    void client.invalidateQueries({ queryKey: testCasesQueryKey });
    void client.invalidateQueries({ queryKey: testCaseDetailQueryKey(data.id) });
    void client.invalidateQueries({ queryKey: testCaseVersionsQueryKey(data.id) });
  } });
}
export function useUpdateTestCaseDraft(): UseMutationResult<TestCaseDetail, ApiError, { masterId: string; payload: Omit<DraftPayload, 'caseCode' | 'categoryId'> }> {
  const client = useQueryClient();
  return useMutation({ mutationFn: ({ masterId, payload }) => updateTestCaseDraft(masterId, payload), onSuccess: (data) => {
    void client.invalidateQueries({ queryKey: testCasesQueryKey });
    void client.invalidateQueries({ queryKey: testCaseDetailQueryKey(data.id) });
    void client.invalidateQueries({ queryKey: testCaseVersionsQueryKey(data.id) });
  } });
}

// ---------------------------------------------------------------------------
// Phase 7 — Test Case Lifecycle hooks
// ---------------------------------------------------------------------------

export function useReviewRecords(masterId: string, versionId: string, enabled = true): UseQueryResult<ReviewRecord[], ApiError> {
  return useQuery({
    queryKey: reviewRecordsQueryKey(masterId, versionId),
    queryFn: () => getReviewRecords(masterId, versionId),
    enabled: enabled && Boolean(masterId) && Boolean(versionId),
  });
}
export function useContributors(masterId: string, enabled = true): UseQueryResult<Contributor[], ApiError> {
  return useQuery({
    queryKey: contributorsQueryKey(masterId),
    queryFn: () => listContributors(masterId),
    enabled: enabled && Boolean(masterId),
  });
}

export function useSubmitReview(): UseMutationResult<TestCaseDetail, ApiError, { masterId: string; payload?: LifecycleActionPayload }> {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ masterId, payload }) => submitReview(masterId, payload),
    onSuccess: (data) => invalidateTestCaseCaches(client, data.id),
  });
}
export function usePublish(): UseMutationResult<TestCaseDetail, ApiError, { masterId: string; versionId: string; payload?: LifecycleActionPayload }> {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ masterId, versionId, payload }) => publishVersion(masterId, versionId, payload),
    onSuccess: (data) => invalidateTestCaseCaches(client, data.id),
  });
}
export function useReturnReview(): UseMutationResult<TestCaseDetail, ApiError, { masterId: string; versionId: string; payload?: LifecycleActionPayload }> {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ masterId, versionId, payload }) => returnReview(masterId, versionId, payload),
    onSuccess: (data) => invalidateTestCaseCaches(client, data.id),
  });
}
export function useReject(): UseMutationResult<TestCaseDetail, ApiError, { masterId: string; versionId: string; payload?: LifecycleActionPayload }> {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ masterId, versionId, payload }) => rejectVersion(masterId, versionId, payload),
    onSuccess: (data) => invalidateTestCaseCaches(client, data.id),
  });
}
export function useDeprecate(): UseMutationResult<TestCaseDetail, ApiError, { masterId: string; versionId: string; payload?: LifecycleActionPayload }> {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ masterId, versionId, payload }) => deprecateVersion(masterId, versionId, payload),
    onSuccess: (data) => invalidateTestCaseCaches(client, data.id),
  });
}
export function useCreateRevision(): UseMutationResult<TestCaseDetail, ApiError, { masterId: string; payload?: CreateRevisionPayload }> {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ masterId, payload }) => createRevision(masterId, payload),
    onSuccess: (data) => invalidateTestCaseCaches(client, data.id),
  });
}
export function useAddContributor(): UseMutationResult<Contributor[], ApiError, { masterId: string; userId: string }> {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ masterId, userId }) => addContributor(masterId, userId),
    onSuccess: (_, vars) => void client.invalidateQueries({ queryKey: contributorsQueryKey(vars.masterId) }),
  });
}
export function useRemoveContributor(): UseMutationResult<Contributor[], ApiError, { masterId: string; userId: string }> {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ masterId, userId }) => removeContributor(masterId, userId),
    onSuccess: (_, vars) => void client.invalidateQueries({ queryKey: contributorsQueryKey(vars.masterId) }),
  });
}

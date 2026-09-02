import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { UseMutationResult, UseQueryResult } from '@tanstack/react-query';
import { createTestCase, getTestCase, getTestCaseVersion, listTestCaseVersions, listTestCases, updateTestCaseDraft } from '@/features/testcase/api/testCaseApi';
import type { DraftPayload, TestCaseListParams } from '@/features/testcase/api/testCaseApi';
import type { ApiError } from '@/shared/api/apiError';
import type { PagedResponse, TestCaseDetail, TestCaseSummary, TestCaseVersion, VersionSummary } from '@/shared/types/testCase';

export const testCasesQueryKey = ['testCases'] as const;
export const testCaseDetailQueryKey = (masterId: string) => ['testCaseDetail', masterId] as const;
export const testCaseVersionsQueryKey = (masterId: string) => ['testCaseVersions', masterId] as const;
export const testCaseVersionQueryKey = (masterId: string, versionId: string) => ['testCaseVersion', masterId, versionId] as const;

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

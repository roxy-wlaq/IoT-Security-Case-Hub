import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { UseMutationResult, UseQueryResult } from '@tanstack/react-query';
import {
  createCapability,
  disableCapability,
  enableCapability,
  fetchCapabilityTree,
  updateCapability,
} from '@/features/capability/api/capabilityApi';
import type { ApiError } from '@/shared/api/apiError';
import type { Capability, CapabilityCreatePayload, CapabilityTreeNode, CapabilityUpdatePayload } from '@/shared/types/capability';

export const capabilitiesQueryKey = ['capabilities'] as const;

export const capabilityTreeQueryKey = [...capabilitiesQueryKey, 'tree'] as const;

function invalidateCapabilityTree(queryClient: ReturnType<typeof useQueryClient>): Promise<unknown> {
  return queryClient.invalidateQueries({ queryKey: capabilitiesQueryKey });
}

/** GET /api/v1/capabilities/tree */
export function useCapabilityTree(): UseQueryResult<CapabilityTreeNode[], ApiError> {
  return useQuery<CapabilityTreeNode[], ApiError>({
    queryKey: capabilityTreeQueryKey,
    queryFn: fetchCapabilityTree,
    staleTime: 30_000,
  });
}

/** POST /api/v1/capabilities */
export function useCreateCapability(): UseMutationResult<Capability, ApiError, CapabilityCreatePayload> {
  const queryClient = useQueryClient();
  return useMutation<Capability, ApiError, CapabilityCreatePayload>({
    mutationFn: createCapability,
    onSuccess: () => invalidateCapabilityTree(queryClient),
  });
}

/** PUT /api/v1/capabilities/{id} */
export function useUpdateCapability(): UseMutationResult<
  Capability,
  ApiError,
  { id: string; payload: CapabilityUpdatePayload }
> {
  const queryClient = useQueryClient();
  return useMutation<Capability, ApiError, { id: string; payload: CapabilityUpdatePayload }>({
    mutationFn: ({ id, payload }) => updateCapability(id, payload),
    onSuccess: () => invalidateCapabilityTree(queryClient),
  });
}

/** POST /api/v1/capabilities/{id}/enable */
export function useEnableCapability(): UseMutationResult<Capability, ApiError, string> {
  const queryClient = useQueryClient();
  return useMutation<Capability, ApiError, string>({
    mutationFn: enableCapability,
    onSuccess: () => invalidateCapabilityTree(queryClient),
  });
}

/** POST /api/v1/capabilities/{id}/disable */
export function useDisableCapability(): UseMutationResult<Capability, ApiError, string> {
  const queryClient = useQueryClient();
  return useMutation<Capability, ApiError, string>({
    mutationFn: disableCapability,
    onSuccess: () => invalidateCapabilityTree(queryClient),
  });
}

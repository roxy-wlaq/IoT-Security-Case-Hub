import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { DecisionPointEditor } from '@/features/testcase/components/DecisionPointEditor';

Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: () => ({ matches: false, addListener: vi.fn(), removeListener: vi.fn(), addEventListener: vi.fn(), removeEventListener: vi.fn() }),
});

const createMutation = { isPending: false, mutate: vi.fn() };
const updateMutation = { isPending: false, mutate: vi.fn() };
const deleteMutation = { isPending: false, mutate: vi.fn() };

vi.mock('@xyflow/react', () => ({
  Background: () => <div />,
  Controls: () => <div />,
  MiniMap: () => <div />,
  ReactFlow: ({ nodes, edges, children }: { nodes: unknown[]; edges: unknown[]; children: React.ReactNode }) => <div data-testid="logic-graph">{nodes.length} nodes / {edges.length} edges{children}</div>,
}));
vi.mock('@/features/testcase/hooks/useTestCases', () => ({
  useDecisionPoints: vi.fn(() => ({ data: [{ id: 'dp-1', testCaseVersionId: 'v-1', displayOrder: 1, name: 'Reachable?', description: null, transition: { id: 't-1', type: 'NEXT_CASE', targets: [{ id: 'target-1', targetOrder: 1, masterTestCaseId: 'm-2', caseCode: 'TC-002' }] } }], isError: false })),
  useMasterLogicGraph: vi.fn(() => ({ data: { testCaseVersionId: 'v-1', rootMasterTestCaseId: 'm-1', nodes: [{ masterTestCaseId: 'm-1', caseCode: 'TC-001', label: 'TC-001' }, { masterTestCaseId: 'm-2', caseCode: 'TC-002', label: 'TC-002' }], edges: [{ id: 'e-1', sourceMasterTestCaseId: 'm-1', targetMasterTestCaseId: 'm-2', transitionType: 'NEXT_CASE', label: 'Reachable?' }] }, isError: false })),
  useCreateDecisionPoint: vi.fn(() => createMutation),
  useUpdateDecisionPoint: vi.fn(() => updateMutation),
  useDeleteDecisionPoint: vi.fn(() => deleteMutation),
}));

describe('DecisionPointEditor', () => {
  it('renders ordered decision points and the master graph', () => {
    render(<DecisionPointEditor masterId="m-1" versionId="v-1" readOnly />);
    expect(screen.getByText('1. Reachable?')).toBeInTheDocument();
    expect(screen.getByText('TC-002')).toBeInTheDocument();
    expect(screen.getByTestId('logic-graph')).toHaveTextContent('2 nodes / 1 edges');
    expect(screen.getByText('只读')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '添加 Decision Point' })).not.toBeInTheDocument();
  });

  it('opens the editor for an editable draft', () => {
    render(<DecisionPointEditor masterId="m-1" versionId="v-1" readOnly={false} />);
    fireEvent.click(screen.getByRole('button', { name: '添加 Decision Point' }));
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('名称')).toBeInTheDocument();
    expect(screen.getByText('PASS 不允许配置目标。')).toBeInTheDocument();
  });
});

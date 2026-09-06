export type E2EUserKey = 'admin' | 'coordinator' | 'testerA' | 'testerB' | 'unauthorizedTester';

export interface E2EUser {
  username: string;
  password: string;
  id?: string;
}

export interface E2ESessionCookie {
  name: string;
  value: string;
  domain?: string;
  path?: string;
  expires?: number;
  httpOnly?: boolean;
  secure?: boolean;
  sameSite?: 'Strict' | 'Lax' | 'None';
}

export interface ScenarioFixtures {
  standardTaskTypeId: string;
  categoryId: string;
  fullProfile?: { projectId?: string; masterTestCaseId?: string; projectTestCaseId?: string };
  progressiveBluetooth?: { projectId: string; entryProjectTestCaseId: string; nextProjectTestCaseId?: string };
  multiPredecessor?: { projectId: string; predecessorAProjectTestCaseId: string; predecessorBProjectTestCaseId: string; targetProjectTestCaseId: string };
  floating?: { projectId: string; sourceProjectTestCaseId: string; targetProjectTestCaseId: string };
  capabilityRequest?: { projectId: string; capabilityId: string };
  changeRequest?: { masterTestCaseId: string; sourceVersionId: string };
  versionUpgrade?: { projectTestCaseId: string; oldVersionId: string; newVersionId: string };
  customCase?: { projectId: string; customTestCaseId?: string };
}

export interface E2EState {
  users: Record<E2EUserKey, E2EUser>;
  fixtures: ScenarioFixtures;
  sessions?: Partial<Record<E2EUserKey, E2ESessionCookie[]>>;
}

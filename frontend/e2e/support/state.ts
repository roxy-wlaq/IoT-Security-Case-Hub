import fs from 'node:fs';
import path from 'node:path';
import type { E2EState } from './types';

const statePath = process.env.E2E_STATE_FILE ?? path.resolve(process.cwd(), 'e2e/.state.json');

export function loadE2EState(): E2EState {
  if (!fs.existsSync(statePath)) {
    throw new Error(`E2E state is missing at ${statePath}. Run npm run e2e:bootstrap and create the scenario fixture state.`);
  }
  return JSON.parse(fs.readFileSync(statePath, 'utf8')) as E2EState;
}

export function stateFilePath(): string {
  return statePath;
}

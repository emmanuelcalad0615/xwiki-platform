export function assertIncludes(actual: string, expected: string, msg: string) {
  if (!actual.includes(expected)) {
    throw new Error(`${msg}\n  esperado contener: "${expected}"\n  recibido: "${actual}"`);
  }
}

export function assertEquals<T>(actual: T, expected: T, msg: string) {
  if (actual !== expected) {
    throw new Error(`${msg}\n  esperado: ${expected}\n  recibido: ${actual}`);
  }
}

export function assertGreaterThan(actual: number, min: number, msg: string) {
  if (actual <= min) {
    throw new Error(`${msg}\n  esperado > ${min}\n  recibido: ${actual}`);
  }
}

export function assertTruthy(value: unknown, msg: string) {
  if (!value) throw new Error(msg);
}

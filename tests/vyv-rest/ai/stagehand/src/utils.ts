export async function waitForURL(page: { url: () => string }, pattern: RegExp, timeoutMs = 10000) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (pattern.test(page.url())) return;
    await new Promise((r) => setTimeout(r, 200));
  }
  throw new Error(`Timeout: URL "${page.url()}" no coincide con ${pattern}`);
}

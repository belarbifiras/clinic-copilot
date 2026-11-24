export function sessionEmail(): string {
  try {
    const raw = localStorage.getItem('clinic-copilot.session');
    return raw ? ((JSON.parse(raw).email as string) ?? 'demo') : 'demo';
  } catch {
    return 'demo';
  }
}

export function loadJson<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key);
    if (!raw) return fallback;

    const parsed: unknown = JSON.parse(raw);
    if (Array.isArray(fallback)) {
      return (Array.isArray(parsed) ? parsed : fallback) as T;
    }
    if (isRecord(fallback)) {
      return (isRecord(parsed) ? { ...fallback, ...parsed } : fallback) as T;
    }
    return parsed as T;
  } catch {
    return fallback;
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

export function saveJson(key: string, value: unknown): void {
  try { localStorage.setItem(key, JSON.stringify(value)); } catch { /* ignore */ }
}
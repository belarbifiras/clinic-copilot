import { afterEach, describe, expect, it } from 'vitest';
import { loadJson } from './patient-storage';

describe('loadJson', () => {
  const key = 'patient-storage-test';

  afterEach(() => localStorage.removeItem(key));

  it('preserves stored arrays as arrays', () => {
    const history = [{ id: 'conversation-1' }];
    localStorage.setItem(key, JSON.stringify(history));

    const loaded = loadJson(key, []);

    expect(Array.isArray(loaded)).toBe(true);
    expect(loaded).toEqual(history);
  });

  it('merges stored objects with their defaults', () => {
    localStorage.setItem(key, JSON.stringify({ enabled: false }));

    expect(loadJson(key, { enabled: true, reminders: true })).toEqual({
      enabled: false,
      reminders: true,
    });
  });
});

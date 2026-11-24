import { describe, expect, it } from 'vitest';
import { detectTriageLanguage } from './assistant-language';

describe('detectTriageLanguage', () => {
  it('detects Arabic written in Arabic script', () => {
    expect(detectTriageLanguage('عندي شهر قاعدين يضهرولي بقع سوداء في وجهي')).toBe('ar');
  });

  it('detects Tunisian Derja written in Arabizi', () => {
    expect(detectTriageLanguage('thahrouli bkayaa marron fi badni andhom chhar tw')).toBe('ar');
  });

  it('distinguishes English and French symptom descriptions', () => {
    expect(detectTriageLanguage('I have been so much tired this week')).toBe('en');
    expect(detectTriageLanguage('J’ai mal aux jambes depuis une semaine')).toBe('fr');
  });

  it('uses the previous language when a message is ambiguous', () => {
    expect(detectTriageLanguage('hmm', 'en')).toBe('en');
  });
});

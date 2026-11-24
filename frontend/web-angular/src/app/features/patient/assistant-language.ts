import type { TriageLanguage } from './assistant.service';

const ARABIC_SCRIPT = /[\u0600-\u06ff\u0750-\u077f\u08a0-\u08ff]/;

const TUNISIAN_DERJA_WORDS = new Set([
  '3andi', 'andhom', 'andi', 'badni', 'barcha', 'b9a3', 'bkayaa', 'chhar',
  'chnoa', 'chnowa', 'fama', 'famma', 'fi', 'mouch', 'thahrouli', 'taw',
  'tw', 'wejhi', 'yodhherouli',
]);

const STRONG_DERJA_WORDS = new Set([
  '3andi', 'andhom', 'barcha', 'b9a3', 'bkayaa', 'chhar', 'mouch', 'thahrouli',
  'wejhi',
]);

const FRENCH_WORDS = new Set([
  'ai', 'aux', 'avec', 'beaucoup', 'depuis', 'des', 'du', 'est', 'et', 'j',
  'la', 'le', 'les', 'mal', 'mes', 'mon', 'non', 'nous', 'ou', 'oui', 'pour',
  'sur', 'une', 'vous',
]);

const ENGLISH_WORDS = new Set([
  'a', 'am', 'and', 'are', 'been', 'can', 'cant', 'could', 'do', 'does',
  'dont', 'feel', 'for', 'have', 'has', 'i', 'im', 'is', 'it', 'legs', 'my',
  'no', 'not', 'of', 'ok', 'okay', 'on', 'pain', 'since', 'sleep', 'the',
  'this', 'to', 'tired', 'week', 'well', 'with', 'yes', 'yeah', 'yep', 'you',
]);

function tokensOf(text: string): string[] {
  return text
    .toLocaleLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .split(/[^a-z0-9]+/)
    .filter(Boolean);
}

function countMatches(tokens: string[], vocabulary: Set<string>): number {
  return new Set(tokens.filter((token) => vocabulary.has(token))).size;
}

export function detectTriageLanguage(
  text: string,
  fallback: TriageLanguage = 'fr',
): TriageLanguage {
  if (ARABIC_SCRIPT.test(text)) return 'ar';

  const tokens = tokensOf(text);
  const derjaMatches = countMatches(tokens, TUNISIAN_DERJA_WORDS);
  if (derjaMatches >= 2 || tokens.some((token) => STRONG_DERJA_WORDS.has(token))) {
    return 'ar';
  }

  const frenchMatches = countMatches(tokens, FRENCH_WORDS);
  const englishMatches = countMatches(tokens, ENGLISH_WORDS);
  if (englishMatches > frenchMatches && englishMatches > 0) return 'en';
  if (frenchMatches > englishMatches && frenchMatches > 0) return 'fr';
  return fallback;
}

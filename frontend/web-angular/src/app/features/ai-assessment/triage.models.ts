// Contrat avec ai-assessment-service (POST /triage/sessions, POST /triage/sessions/{id}/answers).
export type UrgencyLevel = 'N1' | 'N2' | 'N3' | 'N4' | 'N5';
export type Lang = 'fr' | 'en' | 'ar';

export interface StartTriageRequest {
  symptoms: string;
  language: Lang;
  age?: number;
  sex?: 'MALE' | 'FEMALE' | 'OTHER';
  latitude?: number; // facultatifs, avec l'accord du patient, non conservés côté serveur
  longitude?: number;
}

export interface AnswerRequest {
  answers: string[];
  language?: Lang;
  latitude?: number;
  longitude?: number;
}

export interface PossibleCondition { label: string; likelihood: number; }

export interface TriageAssessment {
  urgency_level: UrgencyLevel;
  consultation_recommended?: boolean;
  confidence: number;
  possible_conditions: PossibleCondition[];
  recommended_specialty: string;
  red_flags_detected: string[];
  patient_explanation: string;
  doctor_summary: string;
  followup_questions: string[];
}

export interface DoctorSuggestion {
  id: string;
  displayName: string;
  city: string | null;
  governorate: string | null;
  distanceKm: number | null;
}

export interface TriageSession {
  sessionId: string;
  status: 'NEEDS_CLARIFICATION' | 'COMPLETED';
  language: Lang;
  clarificationQuestions: string[];
  preliminaryExplanation: string | null;
  assessment: TriageAssessment | null;
  source: 'RULES' | 'LLM' | 'FALLBACK' | null;
  emergency: boolean;
  bookingAllowed: boolean;
  interimAdvice: string[];
  suggestedDoctors: DoctorSuggestion[];
  modelName: string | null;
  promptVersion: string;
  disclaimer: string;
}

export const URGENCY_META: Record<UrgencyLevel, { label: string; hint: string; color: string }> = {
  N1: { label: 'Urgence vitale', hint: 'Appelez les secours immédiatement.', color: '#dc2626' },
  N2: { label: 'Urgent', hint: 'Une consultation médicale dans les 24 heures est recommandée.', color: '#ea580c' },
  N3: { label: 'Semi-urgent', hint: 'Une consultation dans les 72 heures est conseillée.', color: '#d97706' },
  N4: { label: 'Non urgent', hint: 'Une consultation dans les deux semaines est conseillée.', color: '#0d9488' },
  N5: { label: 'Conseil', hint: 'Des conseils simples suffisent, avec un rendez-vous si besoin.', color: '#059669' },
};

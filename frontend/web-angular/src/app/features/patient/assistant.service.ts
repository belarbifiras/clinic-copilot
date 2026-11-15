import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type TriageLanguage = 'fr' | 'ar' | 'en';

export interface TriageStartRequest {
  symptoms: string;
  language: TriageLanguage;
  age?: number;
  sex?: 'MALE' | 'FEMALE' | 'OTHER';
  durationDays?: number;
  intensity?: number;
  latitude?: number;
  longitude?: number;
}

export interface TriageAnswerRequest {
  answers: string[];
  latitude?: number;
  longitude?: number;
}

export interface PossibleCondition {
  label: string;
  likelihood: number;
}

export interface TriageAssessment {
  urgency_level: 'N1' | 'N2' | 'N3' | 'N4' | 'N5';
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
  language: string;
  clarificationQuestions: string[];
  preliminaryExplanation: string | null;
  assessment: TriageAssessment | null;
  source: string;
  emergency: boolean;
  bookingAllowed: boolean;
  interimAdvice: string[];
  suggestedDoctors: DoctorSuggestion[];
  disclaimer: string;
}

@Injectable({ providedIn: 'root' })
export class AssistantService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/triage/sessions';

  start(req: TriageStartRequest): Observable<TriageSession> {
    return this.http.post<TriageSession>(this.base, req);
  }

  answer(sessionId: string, req: TriageAnswerRequest): Observable<TriageSession> {
    return this.http.post<TriageSession>(`${this.base}/${sessionId}/answers`, req);
  }
}
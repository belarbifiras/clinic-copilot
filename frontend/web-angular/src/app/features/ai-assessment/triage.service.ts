import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AnswerRequest, StartTriageRequest, TriageSession } from './triage.models';

// TEMPORAIRE : le service IA lit l'identité dans X-Patient-Id (falsifiable).
// À supprimer quand la gateway validera le JWT et fournira l'identité du patient.
const TEMP_PATIENT_REF = 'patient-demo-001';

@Injectable({ providedIn: 'root' })
export class TriageService {
  private readonly http = inject(HttpClient);
  // Chemin à convenir avec le responsable de l'api-gateway (route vers ai-assessment-service).
  private readonly base = '/api/triage/sessions';
  private readonly headers = { 'X-Patient-Id': TEMP_PATIENT_REF };

  start(req: StartTriageRequest): Observable<TriageSession> {
    return this.http.post<TriageSession>(this.base, req, { headers: this.headers });
  }

  answer(sessionId: string, req: AnswerRequest): Observable<TriageSession> {
    return this.http.post<TriageSession>(`${this.base}/${sessionId}/answers`, req, { headers: this.headers });
  }
}

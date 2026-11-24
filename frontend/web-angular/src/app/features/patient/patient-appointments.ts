import { Injectable, computed, inject, signal } from '@angular/core';
import { AuthService } from '../../core/auth/auth.service';
import { AppointmentService, AppointmentDTO } from './appointment.service';
import { DOCTORS, Doctor, PatientAppointment, AppointmentStatus } from './patient.models';
import {
  Result, addReview, availableSlots, bookAppointment, cancelAppointment, demoAppointments, parseLocal, rescheduleAppointment,
} from './appointment-utils';

export type Outcome = { ok: true } | { ok: false; error: string };

function sessionKey(): string {
  try {
    const raw = localStorage.getItem('clinic-copilot.session');
    const email = raw ? (JSON.parse(raw).email as string) : 'demo';
    return `cc_appointments_${email}`;
  } catch {
    return 'cc_appointments_demo';
  }
}

function backendToFrontendStatus(status: string): AppointmentStatus {
  switch (status?.toUpperCase()) {
    case 'CONFIRMED': return 'confirme';
    case 'PENDING': return 'attente';
    case 'COMPLETED': return 'termine';
    case 'CANCELLED': return 'annule';
    default: return 'confirme';
  }
}

function mapDtoToPatientAppointment(dto: AppointmentDTO): PatientAppointment {
  return {
    id: String(dto.id),
    // Keep the identifier used by the DOCTORS catalogue (real directory ids) so that
    // booked doctors from the assistant actually appear in the patient space lists.
    doctorId: String(dto.doctorId),
    start: dto.startTime ? dto.startTime.slice(0, 16) : new Date().toISOString().slice(0, 16),
    durationMin: 30,
    reason: dto.reason || 'Consultation médicale',
    status: backendToFrontendStatus(dto.status),
    createdAt: dto.startTime,
  };
}

/** Passe en "terminé" les rendez-vous confirmés dont l'heure est passée. */
function settle(list: PatientAppointment[], now = new Date()): PatientAppointment[] {
  return list.map((a) =>
    a.status === 'confirme' && parseLocal(a.start).getTime() + a.durationMin * 60000 < now.getTime() ? { ...a, status: 'termine' as const } : a,
  );
}

@Injectable({ providedIn: 'root' })
export class PatientAppointments {
  private readonly key = sessionKey();
  private readonly auth = inject(AuthService);
  private readonly apptHttp = inject(AppointmentService);

  readonly appointments = signal<PatientAppointment[]>([]);
  readonly ready = signal(false);
  readonly error = signal<string | null>(null);

  readonly groups = computed(() => {
    const by = (dir: 1 | -1) => (x: PatientAppointment, y: PatientAppointment) => dir * (parseLocal(x.start).getTime() - parseLocal(y.start).getTime());
    const all = this.appointments();
    return {
      avenir: all.filter((a) => a.status === 'confirme' || a.status === 'attente').sort(by(1)),
      termines: all.filter((a) => a.status === 'termine').sort(by(-1)),
      annules: all.filter((a) => a.status === 'annule').sort(by(-1)),
    };
  });

  constructor() {
    setTimeout(() => this.load(), 100);
  }

  load(): void {
    let localList: PatientAppointment[] = [];
    try {
      const raw = localStorage.getItem(this.key);
      localList = settle(raw ? (JSON.parse(raw) as PatientAppointment[]) : demoAppointments());
      this.appointments.set(localList);
    } catch {
      localList = demoAppointments();
      this.appointments.set(localList);
    }

    const session = this.auth.session();
    const patientId = session ? this.extractUserId(session.token) : null;

    if (patientId) {
      this.apptHttp.getByPatient(patientId).subscribe({
        next: (dtos) => {
          if (dtos && dtos.length > 0) {
            const remoteList = dtos.map(mapDtoToPatientAppointment);
            // Merge remote with local reviews/details
            const merged = remoteList.map((r) => {
              const local = localList.find((l) => l.id === r.id);
              return local ? { ...r, review: local.review, cancelReason: local.cancelReason } : r;
            });
            this.appointments.set(settle(merged));
            try { localStorage.setItem(this.key, JSON.stringify(merged)); } catch { /* ignore */ }
          }
          this.ready.set(true);
        },
        error: () => {
          this.ready.set(true);
        },
      });
    } else {
      this.ready.set(true);
    }
  }

  private extractUserId(token: string): number | null {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const id = payload.sub ?? payload.userId ?? payload.id;
      const n = Number(id);
      return isNaN(n) ? null : n;
    } catch {
      return null;
    }
  }

  getDoctor(id: string): Doctor | undefined {
    return DOCTORS.find((d) => d.id === id)
        ?? DOCTORS.find((d) => d.name.toLowerCase() === id.toLowerCase());
  }

  slotsFor(doctorId: string, day: Date, ignoreId?: string): string[] {
    return availableSlots(this.appointments(), doctorId, day, new Date(), ignoreId);
  }

  private async commit(fn: (list: PatientAppointment[]) => Result): Promise<Outcome> {
    await new Promise((r) => setTimeout(r, 200));
    const r = fn(this.appointments());
    if (!r.ok) return r;
    this.appointments.set(r.list);
    try { localStorage.setItem(this.key, JSON.stringify(r.list)); } catch { /* ignore */ }
    return { ok: true };
  }

  async book(doctorId: string, start: string, reason: string): Promise<Outcome> {
    const outcome = await this.commit((l) => bookAppointment(l, { id: crypto.randomUUID(), doctorId, start, reason }));
    if (outcome.ok) {
      // Tenter d'envoyer au backend en arrière-plan si possible
      const session = this.auth.session();
      const patientId = session ? this.extractUserId(session.token) : null;
      const docNum = parseInt(doctorId.replace(/\D/g, ''), 10) || 1;
      if (patientId) {
        this.apptHttp.book({ patientId, doctorId: docNum, timeSlotId: 1 }).subscribe({
          next: () => {},
          error: () => {},
        });
      }
    }
    return outcome;
  }

  async cancel(id: string, reason?: string): Promise<Outcome> {
    const outcome = await this.commit((l) => cancelAppointment(l, id, reason));
    if (outcome.ok) {
      const numId = parseInt(id, 10);
      if (!isNaN(numId)) {
        this.apptHttp.cancel(numId, reason || 'Annulation').subscribe({
          next: () => {},
          error: () => {},
        });
      }
    }
    return outcome;
  }

  reschedule(id: string, newStart: string): Promise<Outcome> {
    return this.commit((l) => rescheduleAppointment(l, id, newStart));
  }

  review(id: string, rating: number, comment: string): Promise<Outcome> {
    return this.commit((l) => addReview(l, id, rating, comment));
  }
}

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of } from 'rxjs';
import { API_BASE_URL } from '../../core/api.config';

export interface AppointmentDTO {
  id: number;
  patientId: number;
  doctorId: number;
  timeSlotId: number;
  startTime: string;
  endTime: string;
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED';
  reason: string;
}

export interface BookAppointmentDTO {
  patientId: number;
  doctorId: number;
  timeSlotId: number;
}

export interface RescheduleDTO {
  newTimeSlotId: number;
}

@Injectable({ providedIn: 'root' })
export class AppointmentService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE_URL}/api/appointments`;

  getByPatient(patientId: number): Observable<AppointmentDTO[]> {
    return this.http
      .get<AppointmentDTO[]>(`${this.base}/patient/${patientId}`)
      .pipe(catchError(() => of([])));
  }

  getByDoctor(doctorId: number, from: string, to: string): Observable<AppointmentDTO[]> {
    return this.http
      .get<AppointmentDTO[]>(`${this.base}/doctor/${doctorId}`, { params: { from, to } })
      .pipe(catchError(() => of([])));
  }

  book(dto: BookAppointmentDTO): Observable<AppointmentDTO> {
    return this.http.post<AppointmentDTO>(`${this.base}`, dto);
  }

  cancel(id: number, reason: string): Observable<AppointmentDTO> {
    return this.http.post<AppointmentDTO>(`${this.base}/${id}/cancel`, null, { params: { reason } });
  }

  reschedule(id: number, dto: RescheduleDTO): Observable<AppointmentDTO> {
    return this.http.post<AppointmentDTO>(`${this.base}/${id}/reschedule`, dto);
  }

  updateStatus(id: number, status: string): Observable<AppointmentDTO> {
    return this.http.patch<AppointmentDTO>(`${this.base}/${id}/status`, null, { params: { status } });
  }

  getById(id: number): Observable<AppointmentDTO> {
    return this.http.get<AppointmentDTO>(`${this.base}/${id}`);
  }
}

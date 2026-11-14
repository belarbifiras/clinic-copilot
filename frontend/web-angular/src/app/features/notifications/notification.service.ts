import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of } from 'rxjs';
import { API_BASE_URL } from '../../core/api.config';

export interface NotificationItem {
  id: string;
  type: string;
  title: string;
  message: string;
  seen: boolean;
  createdAt: string;
}

export interface NotificationPage {
  content: NotificationItem[];
  totalElements: number;
  totalPages: number;
  number: number;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE_URL}/api/notifications`;

  list(page = 0, size = 50, unreadOnly = false): Observable<NotificationPage> {
    return this.http
      .get<NotificationPage>(`${this.base}`, { params: { page, size, unreadOnly } })
      .pipe(catchError(() => of({ content: [], totalElements: 0, totalPages: 0, number: 0 })));
  }

  unreadCount(): Observable<{ count: number }> {
    return this.http
      .get<{ count: number }>(`${this.base}/unread-count`)
      .pipe(catchError(() => of({ count: 0 })));
  }

  markRead(id: string): Observable<void> {
    return this.http
      .patch<void>(`${this.base}/${id}/read`, {})
      .pipe(catchError(() => of(undefined as unknown as void)));
  }

  markAllRead(): Observable<{ updated: number }> {
    return this.http
      .patch<{ updated: number }>(`${this.base}/read-all`, {})
      .pipe(catchError(() => of({ updated: 0 })));
  }
}

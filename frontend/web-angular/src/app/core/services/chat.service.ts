import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of } from 'rxjs';
import { API_BASE_URL } from '../../core/api.config';

export interface ChatConversation {
  _id: string;
  participants: string[];
  contactName?: string;
  contactId?: string;
  lastMessage?: ChatMsg;
  updatedAt?: string;
}

export interface ChatMsg {
  _id: string;
  conversationId: string;
  senderId: string;
  text: string;
  createdAt: string;
}

export interface SendMessageDTO {
  conversationId: string;
  text: string;
}

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE_URL}/api/chat`;

  getConversations(): Observable<ChatConversation[]> {
    return this.http
      .get<ChatConversation[]>(`${this.base}/conversations`)
      .pipe(catchError(() => of([])));
  }

  getMessages(conversationId: string): Observable<ChatMsg[]> {
    return this.http
      .get<ChatMsg[]>(`${this.base}/conversations/${conversationId}/messages`)
      .pipe(catchError(() => of([])));
  }

  sendMessage(dto: SendMessageDTO): Observable<ChatMsg> {
    return this.http.post<ChatMsg>(`${this.base}/conversations/${dto.conversationId}/messages`, { text: dto.text });
  }

  createConversation(participantId: string): Observable<ChatConversation> {
    return this.http.post<ChatConversation>(`${this.base}/conversations`, { participantId });
  }
}

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Subject, BehaviorSubject, Observable } from 'rxjs';
import { Client, IFrame, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { ChatMessage, TypingEvent, UserStatus, ConversationSummary, PagedMessages } from '../models/chat.models';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private client!: Client;

  isConnected$ = new BehaviorSubject<boolean>(false);
  messages$ = new Subject<ChatMessage>();
  typing$ = new Subject<TypingEvent>();
  seen$ = new Subject<ChatMessage>();
  status$ = new Subject<UserStatus>();

  constructor(private readonly http: HttpClient) {}

  connect(token: string, userId: number): void {
    if (this.client?.active) return;

    this.client = new Client({
      webSocketFactory: () => new SockJS(`${environment.apiGatewayUrl}/chat/ws`),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
        'X-User-Id': String(userId),
      },
      reconnectDelay: 5000,
      onConnect: () => {
        this.isConnected$.next(true);
        this.client.subscribe('/user/queue/messages', (msg: IMessage) => {
          this.messages$.next(JSON.parse(msg.body) as ChatMessage);
        });
        this.client.subscribe('/user/queue/typing', (msg: IMessage) => {
          this.typing$.next(JSON.parse(msg.body) as TypingEvent);
        });
        this.client.subscribe('/user/queue/seen', (msg: IMessage) => {
          this.seen$.next(JSON.parse(msg.body) as ChatMessage);
        });
        this.client.subscribe('/topic/user-status', (msg: IMessage) => {
          this.status$.next(JSON.parse(msg.body) as UserStatus);
        });
      },
      onDisconnect: () => {
        this.isConnected$.next(false);
      },
      onStompError: (frame: IFrame) => {
        console.error('[ChatService] STOMP error', frame);
      },
    });

    this.client.activate();
  }

  disconnect(): void {
    this.client?.deactivate();
    this.isConnected$.next(false);
  }

  get isConnected(): boolean {
    return this.client?.connected === true;
  }

  sendMessage(receiverId: number, content: string): boolean {
    if (!this.isConnected) return false;
    try {
      this.client.publish({
        destination: '/app/chat',
        body: JSON.stringify({ receiverId, content }),
      });
      return true;
    } catch {
      return false;
    }
  }

  sendTyping(receiverId: number, typing: boolean): void {
    if (!this.isConnected) return;
    try {
      this.client.publish({
        destination: '/app/typing',
        body: JSON.stringify({ receiverId, typing }),
      });
    } catch { /* ignore typing errors */ }
  }

  markSeen(messageId: number): void {
    if (!this.isConnected) return;
    try {
      this.client.publish({
        destination: '/app/seen',
        body: JSON.stringify({ messageId }),
      });
    } catch { /* ignore */ }
  }

  getConversation(user1: number, user2: number, page = 0, size = 20): Observable<PagedMessages> {
    return this.http.get<PagedMessages>(
      `${environment.apiGatewayUrl}/chat/api/messages/conversation/${user1}/${user2}?page=${page}&size=${size}`
    );
  }

  getConversations(userId: number): Observable<ConversationSummary[]> {
    return this.http.get<ConversationSummary[]>(
      `${environment.apiGatewayUrl}/chat/api/messages/conversations/${userId}`
    );
  }

  getUnreadCount(userId: number): Observable<Record<string, number>> {
    return this.http.get<Record<string, number>>(
      `${environment.apiGatewayUrl}/chat/api/messages/unread/count/${userId}`
    );
  }

  markSeenRest(messageId: number): Observable<ChatMessage> {
    return this.http.put<ChatMessage>(
      `${environment.apiGatewayUrl}/chat/api/messages/seen/${messageId}`,
      {}
    );
  }
}

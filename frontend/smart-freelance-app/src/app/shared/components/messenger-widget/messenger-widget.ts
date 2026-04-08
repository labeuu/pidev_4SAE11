import {
  Component,
  OnInit,
  OnDestroy,
  HostListener,
  ElementRef,
  ViewChild,
  AfterViewChecked,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';

import { ChatService } from '../../../core/services/chat.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import {
  ChatMessage,
  ConversationSummary,
  TypingEvent,
  UserStatus,
} from '../../../core/models/chat.models';

type PanelView = 'closed' | 'list' | 'chat';

@Component({
  selector: 'app-messenger-widget',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './messenger-widget.html',
  styleUrls: ['./messenger-widget.scss'],
})
export class MessengerWidget implements OnInit, AfterViewChecked, OnDestroy {
  @ViewChild('messagesEl') messagesEl!: ElementRef<HTMLDivElement>;
  @ViewChild('inputEl') inputEl!: ElementRef<HTMLInputElement>;

  // ── Panel state ──────────────────────────────────────────────────────────
  view = signal<PanelView>('closed');
  panelOpen = computed(() => this.view() !== 'closed');

  // ── Auth ─────────────────────────────────────────────────────────────────
  currentUserId: number | null = null;

  // ── Conversations ─────────────────────────────────────────────────────────
  conversations: ConversationSummary[] = [];
  userNames: Record<number, string> = {};
  userAvatars: Record<number, string | null> = {};
  loadingConvs = false;
  searchQuery = '';

  get filteredConvs(): ConversationSummary[] {
    const q = this.searchQuery.toLowerCase().trim();
    if (!q) return this.conversations;
    return this.conversations.filter((c) =>
      this.getName(c.partnerId).toLowerCase().includes(q),
    );
  }

  // ── Chat window ────────────────────────────────────────────────────────────
  selectedPartnerId: number | null = null;
  partnerName = '';
  isPartnerOnline = false;
  isPartnerTyping = false;
  messages: ChatMessage[] = [];
  newMessage = '';
  loadingMsgs = false;
  currentPage = 0;
  totalPages = 1;
  loadingMore = false;
  private shouldScroll = false;
  private typingTimer: ReturnType<typeof setTimeout> | null = null;

  // ── Unread total ──────────────────────────────────────────────────────────
  unreadTotal = 0;

  private subs: Subscription[] = [];

  constructor(
    public chatService: ChatService,
    private auth: AuthService,
    private userService: UserService,
  ) {}

  ngOnInit(): void {
    this.currentUserId = this.auth.getUserId();
    const token = this.auth.getToken();
    if (token && this.currentUserId) {
      this.chatService.connect(token, this.currentUserId);
    }
    this.loadConversations();
    this.initSubscriptions();
  }

  // ── Open / close ──────────────────────────────────────────────────────────

  toggle(): void {
    if (this.view() === 'closed') {
      this.view.set('list');
      if (!this.conversations.length) this.loadConversations();
    } else {
      this.view.set('closed');
    }
  }

  openChat(partnerId: number): void {
    this.selectedPartnerId = partnerId;
    this.partnerName = this.getName(partnerId);
    this.messages = [];
    this.currentPage = 0;
    this.totalPages = 1;
    this.isPartnerTyping = false;
    this.view.set('chat');
    this.loadMessages();

    // Zero out unread for this conversation
    const conv = this.conversations.find((c) => c.partnerId === partnerId);
    if (conv) {
      conv.unreadCount = 0;
      this.recalcUnread();
    }
    setTimeout(() => this.inputEl?.nativeElement.focus(), 80);
  }

  backToList(): void {
    this.view.set('list');
    this.selectedPartnerId = null;
    this.isPartnerTyping = false;
  }

  // ── Conversations ─────────────────────────────────────────────────────────

  loadConversations(): void {
    if (!this.currentUserId) return;
    this.loadingConvs = true;
    this.chatService.getConversations(this.currentUserId).subscribe({
      next: (convs) => {
        this.conversations = convs;
        this.recalcUnread();
        this.loadingConvs = false;
        convs.forEach((c) => this.resolveUser(c.partnerId));
      },
      error: () => (this.loadingConvs = false),
    });
  }

  private resolveUser(id: number): void {
    if (this.userNames[id] !== undefined) return;
    this.userNames[id] = `User #${id}`;
    this.userAvatars[id] = null;
    this.userService.getById(id).subscribe({
      next: (u) => {
        if (u) {
          this.userNames[id] = `${u.firstName} ${u.lastName}`.trim();
          this.userAvatars[id] = (u as any).avatarUrl ?? null;
        }
      },
    });
  }

  getName(id: number): string {
    return this.userNames[id] || `User #${id}`;
  }

  getInitial(id: number): string {
    return this.getName(id).charAt(0).toUpperCase();
  }

  // ── Messages ───────────────────────────────────────────────────────────────

  loadMessages(): void {
    if (!this.currentUserId || !this.selectedPartnerId) return;
    this.loadingMsgs = true;
    this.chatService
      .getConversation(this.currentUserId, this.selectedPartnerId, 0, 30)
      .subscribe({
        next: (paged) => {
          this.messages = [...paged.content].reverse();
          this.totalPages = paged.totalPages;
          this.loadingMsgs = false;
          this.shouldScroll = true;
          this.markSeen();
        },
        error: () => (this.loadingMsgs = false),
      });
  }

  loadMore(): void {
    if (this.loadingMore || this.currentPage >= this.totalPages - 1) return;
    this.loadingMore = true;
    const el = this.messagesEl?.nativeElement;
    const prevH = el ? el.scrollHeight : 0;

    this.chatService
      .getConversation(
        this.currentUserId!,
        this.selectedPartnerId!,
        this.currentPage + 1,
        30,
      )
      .subscribe({
        next: (paged) => {
          const older = [...paged.content].reverse();
          this.messages = [...older, ...this.messages];
          this.currentPage++;
          this.loadingMore = false;
          setTimeout(() => {
            if (el) el.scrollTop = el.scrollHeight - prevH;
          }, 0);
        },
        error: () => (this.loadingMore = false),
      });
  }

  send(): void {
    const content = this.newMessage.trim();
    if (!content || !this.selectedPartnerId || !this.currentUserId) return;

    this.chatService.sendMessage(this.selectedPartnerId, content);
    this.messages.push({
      senderId: this.currentUserId,
      receiverId: this.selectedPartnerId,
      content,
      timestamp: new Date().toISOString(),
      status: 'SENT',
    });
    this.newMessage = '';
    this.shouldScroll = true;
  }

  sendThumbsUp(): void {
    this.newMessage = '👍';
    this.send();
  }

  onKeydown(e: KeyboardEvent): void {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      this.send();
    }
  }

  onTyping(): void {
    if (!this.selectedPartnerId) return;
    this.chatService.sendTyping(this.selectedPartnerId, true);
    if (this.typingTimer) clearTimeout(this.typingTimer);
    this.typingTimer = setTimeout(() => {
      this.chatService.sendTyping(this.selectedPartnerId!, false);
    }, 2000);
  }

  isMine(msg: ChatMessage): boolean {
    return msg.senderId === this.currentUserId;
  }

  trackById(_i: number, m: ChatMessage): number | undefined {
    return m.id;
  }

  trackByPartnerId(_i: number, c: ConversationSummary): number {
    return c.partnerId;
  }

  // ── Seen / scroll ──────────────────────────────────────────────────────────

  private markSeen(): void {
    this.messages
      .filter(
        (m) =>
          m.senderId === this.selectedPartnerId &&
          m.status !== 'SEEN' &&
          m.id,
      )
      .forEach((m) => this.chatService.markSeen(m.id!));
  }

  private scrollToBottom(): void {
    try {
      const el = this.messagesEl?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    } catch {
      // ignore
    }
  }

  ngAfterViewChecked(): void {
    if (this.shouldScroll) {
      this.scrollToBottom();
      this.shouldScroll = false;
    }
  }

  // ── Subscriptions ──────────────────────────────────────────────────────────

  private initSubscriptions(): void {
    // Incoming messages
    this.subs.push(
      this.chatService.messages$.subscribe((msg: ChatMessage) => {
        const partnerId =
          msg.senderId === this.currentUserId
            ? msg.receiverId
            : msg.senderId;

        // If chat window open with this partner, append
        if (
          this.view() === 'chat' &&
          this.selectedPartnerId === partnerId
        ) {
          this.messages.push(msg);
          this.shouldScroll = true;
          if (msg.senderId === partnerId && msg.id) {
            this.chatService.markSeen(msg.id);
          }
        }

        // Update conversation list
        this.resolveUser(partnerId);
        const existing = this.conversations.find(
          (c) => c.partnerId === partnerId,
        );
        if (existing) {
          existing.lastMessage = msg.content;
          existing.lastMessageTime = msg.timestamp;
          existing.lastMessageStatus = msg.status;
          if (
            msg.senderId !== this.currentUserId &&
            !(this.view() === 'chat' && this.selectedPartnerId === partnerId)
          ) {
            existing.unreadCount = (existing.unreadCount || 0) + 1;
            this.recalcUnread();
          }
          // bubble to top
          this.conversations = [
            existing,
            ...this.conversations.filter((c) => c.partnerId !== partnerId),
          ];
        } else {
          this.loadConversations();
        }
      }),
    );

    // Typing indicator
    this.subs.push(
      this.chatService.typing$
        .pipe(
          filter(
            (e: TypingEvent) => e.senderId === this.selectedPartnerId,
          ),
        )
        .subscribe((e: TypingEvent) => {
          this.isPartnerTyping = e.typing;
          if (e.typing) setTimeout(() => (this.isPartnerTyping = false), 3000);
        }),
    );

    // Seen receipts
    this.subs.push(
      this.chatService.seen$.subscribe((updated: ChatMessage) => {
        const msg = this.messages.find((m) => m.id === updated.id);
        if (msg) msg.status = updated.status;
      }),
    );

    // Online status
    this.subs.push(
      this.chatService.status$.subscribe((ev: UserStatus) => {
        const conv = this.conversations.find(
          (c) => c.partnerId === ev.userId,
        );
        if (conv) conv.isOnline = ev.status === 'ONLINE';
        if (this.selectedPartnerId === ev.userId) {
          this.isPartnerOnline = ev.status === 'ONLINE';
        }
      }),
    );
  }

  private recalcUnread(): void {
    this.unreadTotal = this.conversations.reduce(
      (s, c) => s + (c.unreadCount || 0),
      0,
    );
  }

  // ── Close on Escape ──────────────────────────────────────────────────────

  @HostListener('document:keydown.escape')
  onEsc(): void {
    if (this.view() !== 'closed') this.view.set('closed');
  }

  ngOnDestroy(): void {
    this.subs.forEach((s) => s.unsubscribe());
    if (this.typingTimer) clearTimeout(this.typingTimer);
  }
}

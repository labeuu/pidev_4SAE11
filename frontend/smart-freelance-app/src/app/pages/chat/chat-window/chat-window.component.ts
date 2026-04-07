import {
  AfterViewChecked,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { ChatService } from '../../../core/services/chat.service';
import { UserService } from '../../../core/services/user.service';
import { ChatMessage, TypingEvent, UserStatus } from '../../../core/models/chat.models';

@Component({
  selector: 'app-chat-window',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat-window.component.html',
  styleUrls: ['./chat-window.component.scss'],
})
export class ChatWindowComponent implements OnInit, OnChanges, AfterViewChecked, OnDestroy {
  @Input({ required: true }) currentUserId!: number;
  @Input({ required: true }) partnerId!: number;
  @Output() back = new EventEmitter<void>();

  @ViewChild('messagesContainer') messagesContainer!: ElementRef<HTMLDivElement>;

  messages: ChatMessage[] = [];
  newMessage = '';
  isLoading = false;
  partnerName = '';
  partnerAvatar: string | null = null;
  isPartnerOnline = false;
  isPartnerTyping = false;

  currentPage = 0;
  totalPages = 1;
  isLoadingMore = false;

  private typingTimer: ReturnType<typeof setTimeout> | null = null;
  private shouldScrollToBottom = false;
  private subscriptions: Subscription[] = [];

  constructor(
    private chatService: ChatService,
    private userService: UserService,
  ) {}

  ngOnInit(): void {
    this.initSubscriptions();
    this.loadPartnerInfo();
    this.loadMessages();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['partnerId'] && !changes['partnerId'].firstChange) {
      this.messages = [];
      this.currentPage = 0;
      this.totalPages = 1;
      this.isPartnerTyping = false;
      this.loadPartnerInfo();
      this.loadMessages();
    }
  }

  ngAfterViewChecked(): void {
    if (this.shouldScrollToBottom) {
      this.scrollToBottom();
      this.shouldScrollToBottom = false;
    }
  }

  private initSubscriptions(): void {
    const msgSub = this.chatService.messages$
      .pipe(
        filter(
          (msg: ChatMessage) =>
            (msg.senderId === this.partnerId && msg.receiverId === this.currentUserId) ||
            (msg.senderId === this.currentUserId && msg.receiverId === this.partnerId),
        ),
      )
      .subscribe((msg: ChatMessage) => {
        this.messages.push(msg);
        this.shouldScrollToBottom = true;
        if (msg.senderId === this.partnerId && msg.id) {
          this.chatService.markSeen(msg.id);
        }
      });
    this.subscriptions.push(msgSub);

    const typingSub = this.chatService.typing$
      .pipe(filter((e: TypingEvent) => e.senderId === this.partnerId))
      .subscribe((e: TypingEvent) => {
        this.isPartnerTyping = e.typing;
        if (e.typing) setTimeout(() => (this.isPartnerTyping = false), 3000);
      });
    this.subscriptions.push(typingSub);

    const seenSub = this.chatService.seen$.subscribe((updated: ChatMessage) => {
      const existing = this.messages.find((m) => m.id === updated.id);
      if (existing) existing.status = updated.status;
    });
    this.subscriptions.push(seenSub);

    const statusSub = this.chatService.status$
      .pipe(filter((s: UserStatus) => s.userId === this.partnerId))
      .subscribe((s: UserStatus) => {
        this.isPartnerOnline = s.status === 'ONLINE';
      });
    this.subscriptions.push(statusSub);
  }

  loadMessages(): void {
    if (!this.currentUserId || !this.partnerId) return;
    this.isLoading = true;

    this.chatService.getConversation(this.currentUserId, this.partnerId, 0, 20).subscribe({
      next: (paged) => {
        this.messages = [...paged.content].reverse();
        this.totalPages = paged.totalPages;
        this.currentPage = 0;
        this.isLoading = false;
        this.shouldScrollToBottom = true;
        this.markMessagesAsSeen();
      },
      error: () => {
        this.isLoading = false;
      },
    });
  }

  loadMoreMessages(): void {
    if (this.isLoadingMore || this.currentPage >= this.totalPages - 1) return;
    this.isLoadingMore = true;
    const nextPage = this.currentPage + 1;
    const container = this.messagesContainer?.nativeElement;
    const prevScrollHeight = container ? container.scrollHeight : 0;

    this.chatService.getConversation(this.currentUserId, this.partnerId, nextPage, 20).subscribe({
      next: (paged) => {
        const older = [...paged.content].reverse();
        this.messages = [...older, ...this.messages];
        this.currentPage = nextPage;
        this.isLoadingMore = false;
        setTimeout(() => {
          if (container) {
            container.scrollTop = container.scrollHeight - prevScrollHeight;
          }
        }, 0);
      },
      error: () => {
        this.isLoadingMore = false;
      },
    });
  }

  sendMessage(): void {
    const content = this.newMessage.trim();
    if (!content || !this.currentUserId) return;

    this.chatService.sendMessage(this.partnerId, content);
    this.messages.push({
      senderId: this.currentUserId,
      receiverId: this.partnerId,
      content,
      timestamp: new Date().toISOString(),
      status: 'SENT',
    });
    this.newMessage = '';
    this.shouldScrollToBottom = true;
  }

  sendLike(): void {
    this.newMessage = '👍';
    this.sendMessage();
  }

  onTyping(): void {
    this.chatService.sendTyping(this.partnerId, true);
    if (this.typingTimer) clearTimeout(this.typingTimer);
    this.typingTimer = setTimeout(() => {
      this.chatService.sendTyping(this.partnerId, false);
    }, 2000);
  }

  onKeydown(e: KeyboardEvent): void {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      this.sendMessage();
    }
  }

  autoResize(event: Event): void {
    const el = event.target as HTMLTextAreaElement;
    el.style.height = 'auto';
    el.style.height = Math.min(el.scrollHeight, 120) + 'px';
  }

  scrollToBottom(): void {
    try {
      const el = this.messagesContainer?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    } catch {
      // ignore
    }
  }

  markMessagesAsSeen(): void {
    this.messages
      .filter((m) => m.senderId === this.partnerId && m.status !== 'SEEN' && m.id)
      .forEach((m) => this.chatService.markSeen(m.id!));
  }

  isMine(msg: ChatMessage): boolean {
    return msg.senderId === this.currentUserId;
  }

  isDifferentDay(ts1: string, ts2: string): boolean {
    const d1 = new Date(ts1);
    const d2 = new Date(ts2);
    return (
      d1.getFullYear() !== d2.getFullYear() ||
      d1.getMonth() !== d2.getMonth() ||
      d1.getDate() !== d2.getDate()
    );
  }

  trackById(_index: number, msg: ChatMessage): number | undefined {
    return msg.id;
  }

  private loadPartnerInfo(): void {
    this.partnerName = `User #${this.partnerId}`;
    this.partnerAvatar = null;
    this.userService.getById(this.partnerId).subscribe({
      next: (user) => {
        if (user) {
          this.partnerName = `${user.firstName} ${user.lastName}`.trim();
          this.partnerAvatar = (user as any).avatarUrl ?? null;
        }
      },
    });
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach((s) => s.unsubscribe());
    if (this.typingTimer) clearTimeout(this.typingTimer);
  }
}

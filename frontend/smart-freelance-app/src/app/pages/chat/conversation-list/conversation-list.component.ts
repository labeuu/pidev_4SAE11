import {
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { ChatService } from '../../../core/services/chat.service';
import { UserService } from '../../../core/services/user.service';
import { ConversationSummary, ChatMessage, UserStatus } from '../../../core/models/chat.models';

@Component({
  selector: 'app-conversation-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './conversation-list.component.html',
  styleUrls: ['./conversation-list.component.scss'],
})
export class ConversationListComponent implements OnInit, OnDestroy {
  @Input() currentUserId: number | null = null;
  @Input() selectedPartnerId: number | null = null;
  @Output() conversationSelected = new EventEmitter<number>();

  conversations: ConversationSummary[] = [];
  userNames: Record<number, string> = {};
  userAvatars: Record<number, string | null> = {};
  isLoading = false;
  unreadTotal = 0;
  searchQuery = '';

  private subscriptions: Subscription[] = [];

  constructor(
    private chatService: ChatService,
    private userService: UserService,
  ) {}

  get filteredConvs(): ConversationSummary[] {
    const q = this.searchQuery.toLowerCase().trim();
    if (!q) return this.conversations;
    return this.conversations.filter((c) =>
      this.getDisplayName(c.partnerId).toLowerCase().includes(q),
    );
  }

  ngOnInit(): void {
    this.loadConversations();

    const msgSub = this.chatService.messages$.subscribe((msg: ChatMessage) => {
      const partnerId =
        msg.senderId === this.currentUserId ? msg.receiverId : msg.senderId;
      const existing = this.conversations.find((c) => c.partnerId === partnerId);
      if (existing) {
        existing.lastMessage = msg.content;
        existing.lastMessageTime = msg.timestamp;
        existing.lastMessageStatus = msg.status;
        if (msg.senderId !== this.currentUserId && partnerId !== this.selectedPartnerId) {
          existing.unreadCount = (existing.unreadCount || 0) + 1;
          this.recalcUnreadTotal();
        }
        this.conversations = [
          existing,
          ...this.conversations.filter((c) => c.partnerId !== partnerId),
        ];
      } else {
        this.loadConversations();
      }
    });
    this.subscriptions.push(msgSub);

    const statusSub = this.chatService.status$.subscribe((event: UserStatus) => {
      const conv = this.conversations.find((c) => c.partnerId === event.userId);
      if (conv) conv.isOnline = event.status === 'ONLINE';
    });
    this.subscriptions.push(statusSub);
  }

  loadConversations(): void {
    if (!this.currentUserId) return;
    this.isLoading = true;

    this.chatService.getConversations(this.currentUserId).subscribe({
      next: (convs: ConversationSummary[]) => {
        this.conversations = convs;
        this.recalcUnreadTotal();
        this.isLoading = false;
        convs.forEach((conv) => this.resolveUser(conv.partnerId));
      },
      error: () => {
        this.isLoading = false;
      },
    });
  }

  private resolveUser(id: number): void {
    if (this.userNames[id] !== undefined) return;
    this.userNames[id] = `User #${id}`;
    this.userAvatars[id] = null;
    this.userService.getById(id).subscribe({
      next: (user) => {
        if (user) {
          this.userNames[id] = `${user.firstName} ${user.lastName}`.trim();
          this.userAvatars[id] = (user as any).avatarUrl ?? null;
        }
      },
    });
  }

  getDisplayName(partnerId: number): string {
    return this.userNames[partnerId] || `User #${partnerId}`;
  }

  selectConversation(partnerId: number): void {
    this.conversationSelected.emit(partnerId);
    const conv = this.conversations.find((c) => c.partnerId === partnerId);
    if (conv) {
      conv.unreadCount = 0;
      this.recalcUnreadTotal();
    }
  }

  trackByPartnerId(_index: number, conv: ConversationSummary): number {
    return conv.partnerId;
  }

  private recalcUnreadTotal(): void {
    this.unreadTotal = this.conversations.reduce(
      (sum, c) => sum + (c.unreadCount || 0),
      0,
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach((s) => s.unsubscribe());
  }
}

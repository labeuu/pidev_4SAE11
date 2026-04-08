import {
  Component,
  HostListener,
  OnDestroy,
  OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ChatService } from '../../../core/services/chat.service';
import { ConversationListComponent } from '../conversation-list/conversation-list.component';
import { ChatWindowComponent } from '../chat-window/chat-window.component';

@Component({
  selector: 'app-chat-page',
  standalone: true,
  imports: [CommonModule, RouterModule, ConversationListComponent, ChatWindowComponent],
  templateUrl: './chat-page.component.html',
  styleUrls: ['./chat-page.component.scss'],
})
export class ChatPageComponent implements OnInit, OnDestroy {
  selectedPartnerId: number | null = null;
  currentUserId: number | null = null;
  isMobile = false;

  constructor(
    private authService: AuthService,
    private chatService: ChatService,
  ) {}

  ngOnInit(): void {
    this.currentUserId = this.authService.getUserId();
    const token = this.authService.getToken();
    if (token && this.currentUserId) {
      this.chatService.connect(token, this.currentUserId);
    }
    this.updateMobile();
  }

  @HostListener('window:resize')
  updateMobile(): void {
    this.isMobile = window.innerWidth < 768;
  }

  ngOnDestroy(): void {
    this.chatService.disconnect();
  }

  onConversationSelect(partnerId: number): void {
    this.selectedPartnerId = partnerId;
  }

  onBack(): void {
    this.selectedPartnerId = null;
  }
}

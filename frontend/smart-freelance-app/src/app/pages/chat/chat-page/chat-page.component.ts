import {
  Component,
  HostListener,
  OnDestroy,
  OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
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
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.currentUserId = this.authService.getUserId();
    const token = this.authService.getToken();
    if (token && this.currentUserId) {
      this.chatService.connect(token, this.currentUserId);
    }
    this.updateMobile();
    // Auto-open conversation when navigated from e.g. a job application
    const partnerId = this.route.snapshot.queryParamMap.get('partnerId');
    if (partnerId) {
      this.selectedPartnerId = Number(partnerId);
    }
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

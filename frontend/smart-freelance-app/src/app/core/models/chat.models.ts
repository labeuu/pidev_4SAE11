export interface ChatMessage {
  id?: number;
  senderId: number;
  receiverId: number;
  content: string;
  timestamp: string;
  status: 'SENT' | 'DELIVERED' | 'SEEN';
}

export interface TypingEvent {
  senderId: number;
  receiverId: number;
  typing: boolean;
}

export interface UserStatus {
  userId: number;
  status: 'ONLINE' | 'OFFLINE';
}

export interface ConversationSummary {
  partnerId: number;
  lastMessage: string;
  lastMessageTime: string;
  lastMessageStatus: string;
  unreadCount: number;
  isOnline: boolean;
}

export interface PagedMessages {
  content: ChatMessage[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
}

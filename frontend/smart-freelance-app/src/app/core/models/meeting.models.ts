export type MeetingStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'CANCELLED' | 'COMPLETED';
export type MeetingType = 'VIDEO_CALL' | 'VOICE_CALL' | 'IN_PERSON';

export interface Meeting {
  id: number;
  clientId: number;
  freelancerId: number;
  clientName: string;
  freelancerName: string;
  title: string;
  agenda?: string;
  startTime: string;
  endTime: string;
  meetingType: MeetingType;
  status: MeetingStatus;
  meetLink?: string;
  googleEventId?: string;
  projectId?: number;
  contractId?: number;
  cancellationReason?: string;
  createdAt: string;
  updatedAt: string;
  canJoinNow: boolean;
}

export interface CreateMeetingRequest {
  freelancerId: number;
  title: string;
  agenda?: string;
  startTime: string;
  endTime: string;
  meetingType?: MeetingType;
  projectId?: number;
  contractId?: number;
}

export interface UpdateMeetingRequest {
  title?: string;
  agenda?: string;
  startTime?: string;
  endTime?: string;
  meetingType?: MeetingType;
}

export interface StatusUpdateRequest {
  status: MeetingStatus;
  reason?: string;
}

export interface MeetingStats {
  total: number;
  pending: number;
  accepted: number;
  declined: number;
  cancelled: number;
  completed: number;
}

export interface MeetingTranscript {
  id: number;
  meetingId: number;
  userId: number;
  userName: string;
  content: string;
  savedAt: string;
}

export interface MeetingSummary {
  id: number;
  meetingId: number;
  summaryText: string;
  generatedAt: string;
}

export interface ProjectDto {
  id: number;
  title: string;
  clientId: number;
}

export interface ContractDto {
  id: number;
  title: string;
  clientId: number;
  freelancerId: number;
}

export interface MeetingComment {
  id: number;
  meetingId: number;
  userId: number;
  userName: string;
  content: string;
  createdAt: string;
  updatedAt?: string;
}

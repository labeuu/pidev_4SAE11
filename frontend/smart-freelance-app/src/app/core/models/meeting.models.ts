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

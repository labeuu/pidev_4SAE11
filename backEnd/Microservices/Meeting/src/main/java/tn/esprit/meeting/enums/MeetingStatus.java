package tn.esprit.meeting.enums;

/**
 * Lifecycle of a meeting.
 *
 * PENDING   – CLIENT created the meeting, waiting for FREELANCER to accept/decline.
 * ACCEPTED  – FREELANCER accepted; Google Meet link is generated at this point.
 * DECLINED  – FREELANCER declined the proposal.
 * CANCELLED – Either party cancelled (before or after acceptance).
 * COMPLETED – The meeting end time has passed and it ran successfully.
 */
public enum MeetingStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    CANCELLED,
    COMPLETED
}

package com.esprit.ticket;

/**
 * System-generated replies (welcome message) use this author id; excluded from SLA first-response timing.
 */
public final class TicketConstants {
    public static final long SYSTEM_AUTHOR_USER_ID = 0L;
    public static final String WELCOME_REPLY_MESSAGE =
            "Your request has been received. Admin will respond as soon as possible.";

    private TicketConstants() {}
}

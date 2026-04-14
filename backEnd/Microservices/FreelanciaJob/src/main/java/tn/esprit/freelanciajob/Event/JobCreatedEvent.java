package tn.esprit.freelanciajob.Event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import tn.esprit.freelanciajob.Entity.Job;

/**
 * Published by JobService after a new job is persisted.
 * The listener will notify all registered freelancers by email.
 */
@Getter
public class JobCreatedEvent extends ApplicationEvent {

    private final Job job;
    private final String clientName; // display name of the client who posted

    public JobCreatedEvent(Object source, Job job, String clientName) {
        super(source);
        this.job = job;
        this.clientName = clientName;
    }
}

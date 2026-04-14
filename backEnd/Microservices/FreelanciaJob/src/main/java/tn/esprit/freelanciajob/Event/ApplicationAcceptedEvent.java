package tn.esprit.freelanciajob.Event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import tn.esprit.freelanciajob.Entity.JobApplication;

/**
 * Published by JobApplicationServiceImpl when a client accepts an application.
 * The listener congratulates the freelancer by email.
 */
@Getter
public class ApplicationAcceptedEvent extends ApplicationEvent {

    private final JobApplication application;
    private final Long freelancerId;

    public ApplicationAcceptedEvent(Object source, JobApplication application, Long freelancerId) {
        super(source);
        this.application = application;
        this.freelancerId = freelancerId;
    }
}

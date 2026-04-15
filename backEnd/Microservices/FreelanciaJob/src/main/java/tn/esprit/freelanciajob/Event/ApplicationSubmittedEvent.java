package tn.esprit.freelanciajob.Event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import tn.esprit.freelanciajob.Entity.JobApplication;

/**
 * Published by JobApplicationServiceImpl after a freelancer submits an application.
 * The listener sends a confirmation email to the freelancer.
 */
@Getter
public class ApplicationSubmittedEvent extends ApplicationEvent {

    private final JobApplication application;
    private final Long freelancerId;

    public ApplicationSubmittedEvent(Object source, JobApplication application, Long freelancerId) {
        super(source);
        this.application = application;
        this.freelancerId = freelancerId;
    }
}

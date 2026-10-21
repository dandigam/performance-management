package com.rit.performance.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

/** Spring supplies the publisher independently of the service's business dependencies. */
public abstract class WorkflowEventSource implements ApplicationEventPublisherAware {
    private ApplicationEventPublisher workflowEvents;
    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        this.workflowEvents = publisher;
    }
    protected void publishWorkflow(Object event) {
        // Standalone callers outside the application context have no event infrastructure.
        if (workflowEvents != null) workflowEvents.publishEvent(event);
    }
}

package com.saumik.photovault.event;

public interface DomainEventPublisher {

    void publish(Object event);

}
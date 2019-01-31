package com.p2plib2.entity;

public class EventPair {
    int idPair;
    public Event request;
    public Event responce;

    public EventPair(int i, Event event1, Event event2) {
        this.idPair = i;
        this.request = event1;
        this.responce = event2;
    }
}

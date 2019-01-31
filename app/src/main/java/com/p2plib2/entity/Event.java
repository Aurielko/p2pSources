package com.p2plib2.entity;

public class Event {
    public  EventType type;
    public  Body body;

    public Event(EventType type, Body body) {
        this.type = type;
        this.body = body;
    }

    enum EventType{
        Send, Receive
    }

}

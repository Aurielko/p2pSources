package com.p2plib2.entity;

public class MessageEntity {
    String number;
    String msgBody;
    String currentOutputMsg;

    public MessageEntity(String number, String msgBody, String currentOutputMsg) {
        this.currentOutputMsg = currentOutputMsg;
        this.msgBody = msgBody;
        this.number = number;
    }
}

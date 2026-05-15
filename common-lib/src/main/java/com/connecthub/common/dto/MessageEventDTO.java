package com.connecthub.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class MessageEventDTO implements Serializable {

    private String messageId;
    private String senderId;
    private String receiverId;
    private String roomId;
    private String content;
    private String messageType;   // TEXT, IMAGE, FILE
    private LocalDateTime sentAt;

    public MessageEventDTO() {}

    public MessageEventDTO(String messageId, String senderId, String receiverId,
                           String roomId, String content, String messageType) {
        this.messageId   = messageId;
        this.senderId    = senderId;
        this.receiverId  = receiverId;
        this.roomId      = roomId;
        this.content     = content;
        this.messageType = messageType;
        this.sentAt      = LocalDateTime.now();
    }

    // Getters & Setters
    public String getMessageId()                  { return messageId; }
    public void setMessageId(String messageId)    { this.messageId = messageId; }
    public String getSenderId()                   { return senderId; }
    public void setSenderId(String senderId)      { this.senderId = senderId; }
    public String getReceiverId()                 { return receiverId; }
    public void setReceiverId(String receiverId)  { this.receiverId = receiverId; }
    public String getRoomId()                     { return roomId; }
    public void setRoomId(String roomId)          { this.roomId = roomId; }
    public String getContent()                    { return content; }
    public void setContent(String content)        { this.content = content; }
    public String getMessageType()                { return messageType; }
    public void setMessageType(String t)          { this.messageType = t; }
    public LocalDateTime getSentAt()              { return sentAt; }
    public void setSentAt(LocalDateTime sentAt)   { this.sentAt = sentAt; }
}
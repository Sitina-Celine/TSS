package com.example.tss_api.model;

public class Message {
    private int id;
    private String type;
    private String to;
    private String message;
    private boolean sent;

    public Message(int id, String type, String to, String message, boolean sent) {
        this.id = id;
        this.type = type;
        this.to = to;
        this.message = message;
        this.sent = sent;
    }

    public int getId() { return id; }
    public String getType() { return type; }
    public String getTo() { return to; }
    public String getMessage() { return message; }
    public boolean isSent() { return sent; }

    public void setSent(boolean sent) {
        this.sent = sent;
    }
}

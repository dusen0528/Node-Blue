package com.samsa.core;

public class Pipe {
    private final String id;
    private final Node node;
    private Pipe connectedPipe;

    public Pipe(String id, Node node) {
        this.id = id;
        this.node = node;
    }

    public String getId() {
        return id;
    }

    public Node getNode() {
        return node;
    }

    public void connect(Pipe pipe) {
        this.connectedPipe = pipe;
    }

    public void disconnect() {
        this.connectedPipe = null;
    }

    public boolean isConnected() {
        return connectedPipe != null;
    }

    public void send(Message message) {
        if (isConnected()) {
            connectedPipe.getNode().onMessage(message);
        }
    }
}
package com.polidea.reactnativeble;

public enum Event {

    ScanEvent("ScanEvent"),
    ReadEvent("ReadEvent"),
    StateChangeEvent("StateChangeEvent"),
    RestoreStateEvent("RestoreStateEvent"),
    NullAdapterEvent("NullAdapterEvent"),
    DisconnectionEvent("DisconnectionEvent");

    public String name;

    Event(String name) {
        this.name = name;
    }
}

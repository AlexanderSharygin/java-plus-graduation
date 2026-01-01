package ru.practicum.ewm.main.request.model;

public enum RequestStatus {
    PENDING("PENDING"),
    CONFIRMED("CONFIRMED"),
    CANCELED("CANCELED"),
    REJECTED("REJECTED");

    private final String state;

    RequestStatus(String state) {
        this.state = state;
    }

    public String toString() {
        return state;
    }
}

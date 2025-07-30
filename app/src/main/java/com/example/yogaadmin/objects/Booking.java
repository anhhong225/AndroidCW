package com.example.yogaadmin.objects;

import java.util.Map;

public class Booking {
    private  String bookingId;
    private String bookingDate;
    private String customerEmail;
    private String customerName;
    private String className;
    private String yogaType;

    private Map<String, String> scheduleId;

    public Booking() {}

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(String bookingDate) {
        this.bookingDate = bookingDate;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Map<String, String> getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Map<String, String> scheduleId) {
        this.scheduleId = scheduleId;
    }

    public String getClassName() { return className; }
    public String getYogaType() { return yogaType; }
}

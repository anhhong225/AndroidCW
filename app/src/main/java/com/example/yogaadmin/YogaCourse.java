package com.example.yogaadmin;

public class YogaCourse {
    private int id;
    private String dayofweek;
    private String time;
    private int capacity;
    private String duration;
    private float price;
    private String type;
    private String description;

    public YogaCourse() {}  // Required for Firebase

    public YogaCourse(int id, String dayofweek, String time, int capacity,
                      String duration, float price, String type, String description) {
        this.id = id;
        this.dayofweek = dayofweek;
        this.time = time;
        this.capacity = capacity;
        this.duration = duration;
        this.price = price;
        this.type = type;
        this.description = description;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDayofweek() { return dayofweek; }
    public void setDayofweek(String dayofweek) { this.dayofweek = dayofweek; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public float getPrice() { return price; }
    public void setPrice(float price) { this.price = price; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

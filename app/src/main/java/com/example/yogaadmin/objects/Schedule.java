package com.example.yogaadmin.objects;

public class Schedule {
    private int id;
    private String date;
    private String teacher;
    private String comment;
    private int yogaCourseId; // Optional: if you want to relate to YogaCourse
    private int isSynced;
    private int isDeleted;

    public Schedule() {}  // Required for Firebase

    public Schedule(int id, String date, String teacher, String comment) {
        this.id = id;
        this.date = date;
        this.teacher = teacher;
        this.comment = comment;
    }

    // Optionally include this constructor if you use yogaCourseId
    public Schedule(int id, String date, String teacher, String comment, int yogaCourseId) {
        this.id = id;
        this.date = date;
        this.teacher = teacher;
        this.comment = comment;
        this.yogaCourseId = yogaCourseId;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTeacher() { return teacher; }
    public void setTeacher(String teacher) { this.teacher = teacher; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public int getYogaCourseId() { return yogaCourseId; }
    public void setYogaCourseId(int yogaCourseId) { this.yogaCourseId = yogaCourseId; }

    public void setIsSynced(int isSynced) { this.isSynced = isSynced; }
    public void setIsDeleted(int isDeleted) { this.isDeleted = isDeleted; }
    public int getIsSynced() { return isSynced; }
    public int getIsDeleted() { return isDeleted; }
}

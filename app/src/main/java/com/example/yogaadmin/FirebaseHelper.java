package com.example.yogaadmin;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class FirebaseHelper {
    private DatabaseReference courseRef, scheduleRef;
    private FirebaseDatabase firebaseDatabase;
    private Context context;
    private DatabaseHelper dbHelper;

    public FirebaseHelper(Context context){
        this.context = context;
        this.firebaseDatabase = FirebaseDatabase.getInstance("https://yogaadminfirebase-default-rtdb.asia-southeast1.firebasedatabase.app/");
        this.courseRef = firebaseDatabase.getReference("YogaCourse");
        this.scheduleRef = firebaseDatabase.getReference("Schedule");
        this.dbHelper = new DatabaseHelper(context);
    }

    public DatabaseReference getScheduleRef() {
        return scheduleRef;
    }
    public DatabaseReference getCourseRef() {
        return courseRef;
    }
    public void createAYogaCourse(YogaCourse course) {
        if (course == null || course.getId() == 0) return;

        String key = "course_" + course.getId();
        courseRef.child(key).setValue(course).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                dbHelper.updateYogaCourseSyncStatus(course.getId(), true);
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "Course uploaded to Firebase.", Toast.LENGTH_SHORT).show());
            } else {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "Failed to upload course to Firebase.", Toast.LENGTH_SHORT).show());
            }
        });
    }
    public void createASchedule(Schedule schedule) {
        if (schedule == null || schedule.getId() == 0) return;

        String key = "schedule_" + schedule.getId();
        scheduleRef.child(key).setValue(schedule).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                dbHelper.updateScheduleSyncStatus(schedule.getId(), true);
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "Schedule uploaded to Firebase.", Toast.LENGTH_SHORT).show());
            } else {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "Failed to upload schedule.", Toast.LENGTH_SHORT).show());
            }
        });
    }
    public void uploadUpdatedYogaCourse(int courseId) {
        Cursor cursor = dbHelper.getYogaCourseById(courseId);
        if (cursor != null && cursor.moveToFirst()) {
            YogaCourse course = new YogaCourse();
            course.setId(courseId);
            course.setDayofweek(cursor.getString(cursor.getColumnIndexOrThrow("dayofweek")));
            course.setTime(cursor.getString(cursor.getColumnIndexOrThrow("time")));
            course.setCapacity(cursor.getInt(cursor.getColumnIndexOrThrow("capacity")));
            course.setDuration(cursor.getString(cursor.getColumnIndexOrThrow("duration")));
            course.setPrice(cursor.getFloat(cursor.getColumnIndexOrThrow("price")));
            course.setType(cursor.getString(cursor.getColumnIndexOrThrow("type")));
            course.setDescription(cursor.getString(cursor.getColumnIndexOrThrow("description")));
            course.setIsSynced(0); // still mark unsynced
            course.setIsDeleted(0);

            cursor.close();

            // Upload to Firebase
            String key = "course_" + courseId;
            courseRef.child(key).setValue(course).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    dbHelper.updateYogaCourseSyncStatus(courseId, true);
                }
            });
        }
    }
}

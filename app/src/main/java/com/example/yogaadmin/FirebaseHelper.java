package com.example.yogaadmin;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.yogaadmin.objects.Schedule;
import com.example.yogaadmin.objects.YogaCourse;
import com.example.yogaadmin.objects.Booking;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class FirebaseHelper {
    private DatabaseReference courseRef, scheduleRef, bookingRef;
    private FirebaseDatabase firebaseDatabase;
    private Context context;
    private DatabaseHelper dbHelper;

    public FirebaseHelper(Context context){
        this.context = context;
        this.firebaseDatabase = FirebaseDatabase.getInstance("https://yogaadminfirebase-default-rtdb.asia-southeast1.firebasedatabase.app/");
        this.courseRef = firebaseDatabase.getReference("YogaCourse");
        this.scheduleRef = firebaseDatabase.getReference("Schedule");
        this.bookingRef = firebaseDatabase.getReference("Bookings");
        this.dbHelper = new DatabaseHelper(context);
    }

    public DatabaseReference getScheduleRef() {
        return scheduleRef;
    }
    public DatabaseReference getCourseRef() {
        return courseRef;
    }
    public DatabaseReference getBookingRef() {return bookingRef;}

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
            course.setIsSynced(0);
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
    public void loadBooking(BookingLoadCallback callback) {
        DatabaseReference bookingsRef = firebaseDatabase.getReference("Bookings");

        bookingsRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                List<Booking> bookingList = new ArrayList<>();

                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                    callback.onBookingLoaded(bookingList); // return empty list
                } else {
                    for (DataSnapshot data : task.getResult().getChildren()) {
                        String key = data.getKey();
                        HashMap r = (HashMap) data.getValue();

                        Booking b = new Booking();
                        b.setBookingId(key);
                        b.setCustomerEmail(r.get("customerEmail").toString());
                        b.setBookingDate(r.get("bookingDate").toString());

                        // You can parse the schedule list manually too, if needed:
//                        List<HashMap> scheduleMaps = (List<HashMap>) r.get("schedules");
//                        if (scheduleMaps != null) {
//                            List<Schedule> schedules = new ArrayList<>();
//                            for (HashMap map : scheduleMaps) {
//                                Schedule s = new Schedule();
//                                s.setCourseName(map.get("courseName").toString());
//                                s.setDayOfWeek(map.get("dayOfWeek").toString());
//                                s.setTime(map.get("time").toString());
//                                schedules.add(s);
//                            }
//                            b.setSchedules(schedules);
//                        }

                        bookingList.add(b);
                    }

                    // Sort if needed
                    Collections.sort(bookingList, Comparator.comparing(Booking::getCustomerEmail));
                    callback.onBookingLoaded(bookingList);
                }
            }
        });
    }
    public interface BookingLoadCallback {
        void onBookingLoaded(List<Booking> bookings);
    }
}

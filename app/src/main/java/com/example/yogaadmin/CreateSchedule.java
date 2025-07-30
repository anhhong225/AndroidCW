package com.example.yogaadmin;

import android.database.Cursor;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.yogaadmin.objects.Schedule;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CreateSchedule extends AppCompatActivity {
    private DatePicker datePicker;
    private TextView tvDayOfWeek, tvYogaCourse;
    private String expectedDayOfWeek = "";
    private int yogaCourseId = -1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_schedule);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        datePicker = findViewById(R.id.dpDate);
        tvDayOfWeek = findViewById(R.id.tvDayPreview);
        tvYogaCourse = findViewById(R.id.tvYogaCourse);

        yogaCourseId= getIntent().getIntExtra("yoga_course_id", -1);
        if (yogaCourseId != -1) {
            DatabaseHelper helper = MainActivity.helper;
            Cursor c = helper.getYogaCourseById(yogaCourseId);
            if (c != null && c.moveToFirst()) {
                String type = c.getString(c.getColumnIndexOrThrow("type"));
                expectedDayOfWeek = c.getString(c.getColumnIndexOrThrow("dayofweek"));
                tvYogaCourse.setText(type);
                c.close();
            }
        }
        Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH);
        int d = c.get(Calendar.DAY_OF_MONTH);
        datePicker.init(y, m, d, (view, year, monthOfYear, dayOfMonth)
                -> updateDayPreview(year, monthOfYear, dayOfMonth));

        updateDayPreview(y, m, d);
    }
    private void updateDayPreview(int year, int month, int day) {
        Calendar selectedDate = Calendar.getInstance();
        selectedDate.set(year, month, day);
        String dayOfWeek = new SimpleDateFormat("EEEE", Locale.ENGLISH).format(selectedDate.getTime());
        tvDayOfWeek.setText("Day: " + dayOfWeek);
    }
    public void onClickCreateSchedule(View v){
        int year = datePicker.getYear();
        int month = datePicker.getMonth();
        int day = datePicker.getDayOfMonth();

        Calendar selectedDate = Calendar.getInstance();
        selectedDate.set(year, month, day);
        String selectedDay = new SimpleDateFormat("EEE", Locale.ENGLISH).format(selectedDate.getTime());

        if (!selectedDay.equalsIgnoreCase(expectedDayOfWeek)) {
            Toast.makeText(this, "Selected date must be a " + expectedDayOfWeek, Toast.LENGTH_LONG).show();
            return;
        }

        String scheduleDate = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH).format(selectedDate.getTime());
        String teacher = ((EditText)findViewById(R.id.etTeacher)).getText().toString().trim();
        String comment = ((EditText)findViewById(R.id.etmComment)).getText().toString().trim();

        if (teacher.isEmpty()) {
            Toast.makeText(this, "Please enter a teacher name", Toast.LENGTH_SHORT).show();
            return;
        }

        Schedule schedule = new Schedule();
        schedule.setDate(scheduleDate);
        schedule.setTeacher(teacher);
        schedule.setComment(comment);
        schedule.setYogaCourseId(yogaCourseId);
        schedule.setIsSynced(0);
        schedule.setIsDeleted(0);

        // Insert and get new local _id
        long newId = MainActivity.helper.createSchedule(schedule);
        if (newId != -1) {
            schedule.setId((int) newId);
            // Upload to Firebase
            FirebaseHelper firebaseHelper = new FirebaseHelper(this);
            firebaseHelper.createASchedule(schedule);

            Toast.makeText(this, "Schedule saved and uploaded!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Error saving schedule.", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
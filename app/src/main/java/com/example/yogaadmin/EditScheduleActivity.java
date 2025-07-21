package com.example.yogaadmin;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EditScheduleActivity extends AppCompatActivity {
    private long scheduleId;
    private int yogaCourseId;
    private DatePicker dpDate;
    private EditText etTeacher, etComment;
    private TextView tvYogaCourse, tvDayPreview;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
    private Schedule currentSchedule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_schedule);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dpDate = findViewById(R.id.dpEditDate);
        etTeacher = findViewById(R.id.etEditTeacher);
        etComment = findViewById(R.id.etmEditComment);
        tvYogaCourse = findViewById(R.id.tvEditYogaCourse);
        tvDayPreview = findViewById(R.id.tvDayPreview);

        scheduleId = getIntent().getLongExtra("schedule_id", -1L);
        yogaCourseId = getIntent().getIntExtra("yoga_course_id", -1);
//        Log.d("DEBUG_INTENT", "schedule_id = " + getIntent().getIntExtra("schedule_id", -1));
//        Log.d("DEBUG_INTENT", "extras = " + getIntent().getExtras());
        if (scheduleId == -1 || yogaCourseId == -1) {
            Toast.makeText(this, "Invalid schedule or course", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        loadScheduleData();
        loadYogaCourseInfo();

        dpDate.init(dpDate.getYear(), dpDate.getMonth(), dpDate.getDayOfMonth(), new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, monthOfYear, dayOfMonth);

                // Format day of week (e.g., Monday)
                SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.ENGLISH);
                String dayOfWeek = dayFormat.format(calendar.getTime());
                tvDayPreview.setText(dayOfWeek);
            }
        });
    }
    private void loadScheduleData() {
        Cursor c = MainActivity.helper.getScheduleById(scheduleId);
        if (c.moveToFirst()) {
            currentSchedule = new Schedule(
                    (int) scheduleId,
                    c.getString(c.getColumnIndexOrThrow("schedule_date")),
                    c.getString(c.getColumnIndexOrThrow("teacher")),
                    c.getString(c.getColumnIndexOrThrow("comment")),
                    yogaCourseId
            );

            etTeacher.setText(currentSchedule.getTeacher());
            etComment.setText(currentSchedule.getComment());

            try {
                Date date = sdf.parse(currentSchedule.getDate());
                Calendar cal = Calendar.getInstance();
                cal.setTime(sdf.parse(currentSchedule.getDate()));
                dpDate.updateDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

                SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.ENGLISH);
                String dayOfWeek = dayFormat.format(date);
                tvDayPreview.setText(dayOfWeek);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        c.close();
    }
    private void loadYogaCourseInfo() {
        Cursor cursor = MainActivity.helper.getYogaCourseById(yogaCourseId);
        if (cursor.moveToFirst()) {
            String courseType = cursor.getString(cursor.getColumnIndexOrThrow("type"));
            tvYogaCourse.setText(courseType);
        }
        cursor.close();
    }
    public void onClickSaveSchedule(View v){
        String teacher = etTeacher.getText().toString().trim();
        String comment = etComment.getText().toString().trim();

        if (teacher.isEmpty()) {
            Toast.makeText(this, "Teacher is required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Format date from DatePicker
        int day = dpDate.getDayOfMonth();
        int month = dpDate.getMonth();
        int year = dpDate.getYear();
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day);
        String dateStr = sdf.format(cal.getTime());

        // Check if selected date matches yoga course's day
        Cursor cursor = MainActivity.helper.getYogaCourseById(yogaCourseId);
        if (cursor.moveToFirst()) {
            String expectedDay = cursor.getString(cursor.getColumnIndexOrThrow("dayofweek"));
            String selectedDay = new SimpleDateFormat("EEE", Locale.ENGLISH).format(cal.getTime());

            if (!selectedDay.equalsIgnoreCase(expectedDay)) {
                Toast.makeText(this, "Selected date must be a " + expectedDay, Toast.LENGTH_SHORT).show();
                cursor.close();
                return;
            }
        }
        cursor.close();

        // Update local DB and mark unsynced
        int updated = MainActivity.helper.updateSchedule(scheduleId, dateStr, teacher, comment);
        if (updated > 0) {

            Schedule updatedSchedule = new Schedule();
            updatedSchedule.setId((int) scheduleId);
            updatedSchedule.setYogaCourseId(yogaCourseId);
            updatedSchedule.setDate(dateStr);
            updatedSchedule.setTeacher(teacher);
            updatedSchedule.setComment(comment);
            updatedSchedule.setIsSynced(0);
            updatedSchedule.setIsDeleted(0);

            // Upload to Firebase
            FirebaseHelper firebaseHelper = new FirebaseHelper(this);
            firebaseHelper.createASchedule(updatedSchedule); // Uses same ID

            Toast.makeText(this, "Schedule updated successfully", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
        }
    }

    public void onClickReset(View v) {
        loadScheduleData(); // Reset fields to original
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
// ViewEditCourseActivity.java
package com.example.yogaadmin;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.yogaadmin.objects.Schedule;

import java.util.ArrayList;
import java.util.List;


public class ViewEditCourseActivity extends AppCompatActivity {
    private int yogaCourseId;
    private TextView tvType, tvDay, tvTime, tvCapacity, tvDuration, tvPrice, tvDescription;
    private ListView lvSchedules;
    private ScheduleAdapter scheduleAdapter;
    private long scheduleId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_edit_course);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        tvType = findViewById(R.id.tvType);
        tvDay = findViewById(R.id.tvDay);
        tvTime = findViewById(R.id.tvTime);
        tvCapacity = findViewById(R.id.tvCapacity);
        tvDuration = findViewById(R.id.tvDuration);
        tvPrice = findViewById(R.id.tvPrice);
        tvDescription = findViewById(R.id.tvDescription);
        RecyclerView rvSchedules = findViewById(R.id.rvSchedules);
        rvSchedules.setLayoutManager(new LinearLayoutManager(this));

        yogaCourseId = getIntent().getIntExtra("yoga_course_id", -1);
        scheduleId = getIntent().getIntExtra("schedule_id", -1);
        Log.d("DEBUG_SCHEDULE_ID", "Received ID: " + scheduleId);

        if (yogaCourseId == -1) {
            Toast.makeText(this, "Invalid course selected", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        findViewById(R.id.btnEditCourse).setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), EditYogaCourseActivity.class);
            i.putExtra("yoga_course_id", yogaCourseId);
            startActivity(i);
        });

        findViewById(R.id.btnAddSchedule).setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), CreateSchedule.class);
            i.putExtra("yoga_course_id", yogaCourseId);
            startActivity(i);
        });

        findViewById(R.id.btnDeleteCourse).setOnClickListener(v -> showDeleteConfirmation());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCourseDetails();
        loadSchedules();
    }

    private void loadCourseDetails() {
        Cursor c = MainActivity.helper.getYogaCourseById(yogaCourseId);
        if (c.moveToFirst()) {
            tvType.setText(c.getString(c.getColumnIndexOrThrow("type")));
            tvDay.setText("Day: " + c.getString(c.getColumnIndexOrThrow("dayofweek")));
            tvTime.setText("Time: " + c.getString(c.getColumnIndexOrThrow("time")));
            tvCapacity.setText("Capacity: " + c.getInt(c.getColumnIndexOrThrow("capacity")));
            tvDuration.setText("Duration: " + c.getString(c.getColumnIndexOrThrow("duration")));
            tvPrice.setText("Price: $" + c.getFloat(c.getColumnIndexOrThrow("price")));
            tvDescription.setText("Description: " + c.getString(c.getColumnIndexOrThrow("description")));
        }
        c.close();
    }
    private void loadSchedules() {
        Cursor c = MainActivity.helper.getSchedulesByCourseId(yogaCourseId);
        ArrayList<Schedule> scheduleList = new ArrayList<>();

        while (c.moveToNext()) {
            Schedule schedule = new Schedule(
                    c.getInt(c.getColumnIndexOrThrow("_id")),
                    c.getString(c.getColumnIndexOrThrow("schedule_date")),
                    c.getString(c.getColumnIndexOrThrow("teacher")),
                    c.getString(c.getColumnIndexOrThrow("comment")),
                    yogaCourseId
            );
            scheduleList.add(schedule);
        }
        c.close();

        ScheduleAdapter adapter = new ScheduleAdapter(scheduleList, new ScheduleAdapter.ScheduleActionListener() {
            @Override
            public void onEditClicked(long scheduleId) {
                Intent i = new Intent(ViewEditCourseActivity.this, EditScheduleActivity.class);
                i.putExtra("schedule_id", scheduleId);
                i.putExtra("yoga_course_id", yogaCourseId);
                startActivity(i);
            }

            @Override
            public void onDeleteClicked(long scheduleId) {
                FirebaseHelper firebaseHelper = new FirebaseHelper(ViewEditCourseActivity.this);
                DatabaseHelper dbHelper = MainActivity.helper;
                Schedule schedule = dbHelper.getScheduleObjectById(scheduleId);
                if (schedule != null) {
                    String key = "schedule_" + schedule.getId();

                    //Remove from Firebase immediately
                    firebaseHelper.getScheduleRef().child(key).removeValue().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // 3. If Firebase deletion successful → delete from local DB
                            dbHelper.deleteScheduleById((int) scheduleId);
                            Toast.makeText(ViewEditCourseActivity.this, "Deleted from Firebase and local DB.", Toast.LENGTH_SHORT).show();
                            loadSchedules();
                        } else {
                            Toast.makeText(ViewEditCourseActivity.this, "Failed to delete from Firebase.", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(ViewEditCourseActivity.this, "Schedule not found.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        RecyclerView rv = findViewById(R.id.rvSchedules);
        rv.setAdapter(adapter);
    }
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Course")
                .setMessage("Are you sure you want to delete this course and all its schedules?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Delete YogaCourse from Firebase
                    FirebaseHelper firebaseHelper = new FirebaseHelper(this);
                    String courseKey = "course_" + yogaCourseId;
                    firebaseHelper.getCourseRef().child(courseKey).removeValue().addOnCompleteListener(courseTask -> {
                        if (courseTask.isSuccessful()) {
                            // Delete all associated schedules from Firebase
                            Cursor scheduleCursor = MainActivity.helper.getSchedulesByCourseId(yogaCourseId);
                            while (scheduleCursor.moveToNext()) {
                                int scheduleId = scheduleCursor.getInt(scheduleCursor.getColumnIndexOrThrow("_id"));
                                String scheduleKey = "schedule_" + scheduleId;
                                firebaseHelper.getScheduleRef().child(scheduleKey).removeValue();
                            }
                            scheduleCursor.close();
                            //Mark as deleted locally
                            MainActivity.helper.deleteScheduleByCourseId(yogaCourseId); // <- implement if needed
                            MainActivity.helper.deleteYogaCourse(yogaCourseId);

                            Toast.makeText(this, "Course and schedules deleted from Firebase and marked locally.", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this, "Failed to delete course from Firebase.", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {
    private final List<Schedule> scheduleList;
    private final ScheduleActionListener listener;

    public interface ScheduleActionListener {
        void onEditClicked(long scheduleId);
        void onDeleteClicked(long scheduleId);
    }

    public ScheduleAdapter(List<Schedule> scheduleList, ScheduleActionListener listener) {
        this.scheduleList = scheduleList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.schedule_item, parent, false);
        return new ScheduleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        Schedule schedule = scheduleList.get(position);
        holder.tvDate.setText("Date: " + schedule.getDate());
        holder.tvTeacher.setText("Teacher: " + schedule.getTeacher());
        holder.tvComment.setText("Comment: " + schedule.getComment());

        holder.btnEdit.setOnClickListener(v -> listener.onEditClicked(schedule.getId()));
        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(holder.itemView.getContext())
                    .setTitle("Delete Schedule")
                    .setMessage("Are you sure you want to delete this schedule?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        listener.onDeleteClicked(schedule.getId());
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), BookedCustomers.class);
            intent.putExtra("scheduleId", schedule.getId());
            intent.putExtra("scheduleDate", schedule.getDate());
            holder.itemView.getContext().startActivity(intent);
        });

    }

    @Override
    public int getItemCount() {
        return scheduleList.size();
    }

    public static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvTeacher, tvComment;
        ImageButton btnEdit, btnDelete;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvScheduleDate);
            tvTeacher = itemView.findViewById(R.id.tvScheduleTeacher);
            tvComment = itemView.findViewById(R.id.tvScheduleComment);
            btnEdit = itemView.findViewById(R.id.btnEditSchedule);
            btnDelete = itemView.findViewById(R.id.btnDeleteSchedule);
        }
    }
}

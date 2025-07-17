package com.example.yogaadmin;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SearchClassActivity extends AppCompatActivity {
    private EditText etSearchTeacher;
    private DatePicker dpSearchDate;
    private Spinner spnDayOfWeek;
    private TextView tvNoResults;
    private Button btnSearch, btnClear;
    private ScheduleListAdapter adapter;
    private RecyclerView rvResults;
    private String selectedDayOfWeek = "";
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
    private int selectedFilterType = 0, yogaCourseId = -1;
    private long scheduleId = -1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search_class);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        etSearchTeacher = findViewById(R.id.etSearchTeacher);
        dpSearchDate = findViewById(R.id.dpSearchDate);
        spnDayOfWeek = findViewById(R.id.spSearchDay);
        btnSearch = findViewById(R.id.btnSearch);
        btnClear = findViewById(R.id.btnClear);
        tvNoResults = findViewById(R.id.tvNoResults);
        rvResults = findViewById(R.id.rvSearchResults);
        rvResults.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ScheduleListAdapter(new ArrayList<>(), (scheduleId, yogaCourseId) -> {
            Log.d("DEBUG_SCHEDULE_ID", "Passing ID: " + scheduleId);
            Log.d("DEBUG_SCHEDULE_ID", "Yoga ID: " + yogaCourseId);
            Intent intent = new Intent(SearchClassActivity.this, ViewEditCourseActivity.class);
            intent.putExtra("schedule_id", scheduleId);
            intent.putExtra("yoga_course_id", yogaCourseId);
            startActivity(intent);
        });
        rvResults.setAdapter(adapter);

        Spinner spnFilterType = findViewById(R.id.spnFilterType);

        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.day_of_week)
        );
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnDayOfWeek.setAdapter(dayAdapter);

        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.filter_options)
        );
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnFilterType.setAdapter(filterAdapter);
        spnFilterType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedFilterType = position;
                View layoutDateFilter = findViewById(R.id.layoutDateFilter);
                View layoutDayFilter = findViewById(R.id.layoutDayFilter);

                switch (position) {
                    case 0: // "Search by Name"
                        etSearchTeacher.setVisibility(View.VISIBLE);
                        layoutDateFilter.setVisibility(View.GONE);
                        layoutDayFilter.setVisibility(View.GONE);
                        break;
                    case 1: // "Search by Date"
                        etSearchTeacher.setVisibility(View.GONE);
                        layoutDateFilter.setVisibility(View.VISIBLE);
                        layoutDayFilter.setVisibility(View.GONE);
                        break;
                    case 2: // "Search by Day of Week"
                        etSearchTeacher.setVisibility(View.GONE);
                        layoutDateFilter.setVisibility(View.GONE);
                        layoutDayFilter.setVisibility(View.VISIBLE);
                        break;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        btnSearch.setOnClickListener(v -> performSearch());
        btnClear.setOnClickListener(v -> clearFilters());
    }
    private void performSearch() {
        String teacher = "";
        String selectedDate = "";
        String dayOfWeek = "";

        switch (selectedFilterType) {
            case 0:
                teacher = etSearchTeacher.getText().toString().trim();
                break;
            case 1:
                selectedDate = getDateFromPicker();
                break;
            case 2:
                dayOfWeek = spnDayOfWeek.getSelectedItem().toString();
                break;
        }

        Cursor cursor = MainActivity.helper.searchClass(teacher, selectedDate, dayOfWeek);

        ArrayList<Schedule> schedules = new ArrayList<>();
        while (cursor.moveToNext()) {
            Schedule s = new Schedule(
                    cursor.getInt(cursor.getColumnIndexOrThrow("_id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("schedule_date")),
                    cursor.getString(cursor.getColumnIndexOrThrow("teacher")),
                    cursor.getString(cursor.getColumnIndexOrThrow("comment"))
            );
            schedules.add(s);
        }
        cursor.close();

        adapter.updateData(schedules);
        tvNoResults.setVisibility(schedules.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void clearFilters() {
        etSearchTeacher.setText("");
        spnDayOfWeek.setSelection(0);
        Calendar today = Calendar.getInstance();
        dpSearchDate.updateDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
        tvNoResults.setVisibility(View.GONE);
        adapter.updateData(new ArrayList<>());
    }

    private String getDateFromPicker() {
        int day = dpSearchDate.getDayOfMonth();
        int month = dpSearchDate.getMonth();
        int year = dpSearchDate.getYear();
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day);
        return sdf.format(cal.getTime());
    }
}

class ScheduleListAdapter extends RecyclerView.Adapter<ScheduleListAdapter.ItemViewHolder> {
    private List<Schedule> scheduleList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int scheduleId, int yogaCourseId);
    }

    public ScheduleListAdapter(List<Schedule> scheduleList, OnItemClickListener listener) {
        this.scheduleList = scheduleList;
        this.listener = listener;
    }

    public void updateData(List<Schedule> newSchedules) {
        this.scheduleList.clear();
        this.scheduleList.addAll(newSchedules);
        notifyDataSetChanged();
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.schedule_item, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        Schedule schedule = scheduleList.get(position);
        holder.bind(schedule, listener);
    }

    @Override
    public int getItemCount() {
        return scheduleList.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDate, tvTeacher, tvComment;

        public ItemViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvScheduleDate);
            tvTeacher = itemView.findViewById(R.id.tvScheduleTeacher);
            tvComment = itemView.findViewById(R.id.tvScheduleComment);
        }

        public void bind(Schedule schedule, OnItemClickListener listener) {
            tvDate.setText("Date: " + schedule.getDate());
            tvTeacher.setText("Teacher: " + schedule.getTeacher());
            tvComment.setText("Comment: " + schedule.getComment());
            itemView.setOnClickListener(v -> listener.onItemClick(schedule.getId(), schedule.getYogaCourseId()));
        }
    }
}



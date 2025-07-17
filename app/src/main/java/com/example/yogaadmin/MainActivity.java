package com.example.yogaadmin;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static DatabaseHelper helper;
    private long selectedCourseId = -1; // -1 means none selected
    private int selectedPosition = -1;
    private Cursor courseCursor;
    private YogaCourseCursorAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        helper = new DatabaseHelper(getApplicationContext());
    }
    public void onCreateYogaCourse(View v){
        Intent i = new Intent(getApplicationContext(), CreateYogaCourse.class);
        startActivity(i);
    }
    public void onViewCourseDetail(View v){
        if (selectedCourseId == -1) {
            Toast.makeText(this, "Please select a yoga course first.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(getApplicationContext(), ViewEditCourseActivity.class);
        i.putExtra("yoga_course_id", (int) selectedCourseId);
        startActivity(i);
    }
    public void onSearchClass(View v){
        Intent i = new Intent(getApplicationContext(), SearchClassActivity.class);
        startActivity(i);
    }
    public void onUploadAllData(View view) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);

        if (!isNetworkAvailable() || !isInternetAccessible()) {
            Toast.makeText(this, "No internet access available.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<YogaCourse> unsyncedCourses = dbHelper.getUnsyncedYogaCourses();
        List<Schedule> unsyncedSchedules = dbHelper.getUnsyncedSchedules();

        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance("https://yogaadminfirebase-default-rtdb.asia-southeast1.firebasedatabase.app/");
        DatabaseReference courseRef = firebaseDatabase.getReference("YogaCourse");
        DatabaseReference scheduleRef = firebaseDatabase.getReference("Schedule");

        for (YogaCourse course : unsyncedCourses) {
            String key = courseRef.push().getKey();
            if (key != null) {
                courseRef.child(key).setValue(course).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        dbHelper.updateYogaCourseSyncStatus(course.getId(), true);
                    } else {
                        Log.e("FirebaseUpload", "Course upload failed: " + task.getException());
                    }
                });
            }
        }

        for (Schedule schedule : unsyncedSchedules) {
            String key = scheduleRef.push().getKey();
            if (key != null) {
                scheduleRef.child(key).setValue(schedule).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        dbHelper.updateYogaCourseSyncStatus(schedule.getId(), true);
                    } else {
                        Log.e("FirebaseUpload", "Schedule upload failed: " + task.getException());
                    }
                });
            }
        }
        Toast.makeText(this, "No data to upload.", Toast.LENGTH_SHORT).show();
    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
        return false;
    }
    private boolean isInternetAccessible() {
        try {
            HttpURLConnection urlc = (HttpURLConnection)
                    (new URL("https://clients3.google.com/generate_204").openConnection());
            urlc.setRequestProperty("User-Agent", "Android");
            urlc.setRequestProperty("Connection", "close");
            urlc.setConnectTimeout(1500); // 1.5 seconds timeout
            urlc.connect();
            return (urlc.getResponseCode() == 204);
        } catch (IOException e) {
            return false;
        }
    }
    protected void onStart(){
        super.onStart();
        if (courseCursor != null) {
            courseCursor.close(); // prevent memory leaks
        }
        courseCursor = helper.readAllYogaCourse();
        ListView lv = findViewById(R.id.lvCourse);

        if(adapter == null){
            adapter = new YogaCourseCursorAdapter(this, R.layout.yoga_course_item, courseCursor, 0);
            lv.setAdapter(adapter);
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    selectedCourseId = id;
                    selectedPosition = position;
                    adapter.setSelectedPosition(position); // tell adapter to highlight
                    adapter.notifyDataSetChanged();
                }
            });
        }else{
            adapter.changeCursor(courseCursor);
            adapter.notifyDataSetChanged();
        }
    }
}
class YogaCourseCursorAdapter extends ResourceCursorAdapter{
    private int selectedPosition = -1;
    public YogaCourseCursorAdapter(Context context, int layout, Cursor cursor, int flags){
        super(context,layout,cursor,flags);
    }
    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
    }
    @SuppressLint("Range")
    public void bindView(View view, Context context, Cursor cursor){
        TextView dow = view.findViewById(R.id.tvDayOfWeek);
        TextView type = view.findViewById(R.id.tvYogaType);
        TextView time = view.findViewById(R.id.tvTime);
        TextView des = view.findViewById(R.id.tvDescription);
        TextView price = view.findViewById(R.id.tvPrice);

        dow.setText(cursor.getString(cursor.getColumnIndex("dayofweek")));
        type.setText(cursor.getString(cursor.getColumnIndex("type")));
        time.setText(cursor.getString(cursor.getColumnIndex("time")));
        des.setText(cursor.getString(cursor.getColumnIndex("description")));
        price.setText(cursor.getString(cursor.getColumnIndex("price")));

        // Highlight selected row
        int position = cursor.getPosition();
        if (position == selectedPosition) {
            view.setBackgroundColor(context.getResources().getColor(android.R.color.holo_blue_light));
        } else {
            view.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
        }
    }
}
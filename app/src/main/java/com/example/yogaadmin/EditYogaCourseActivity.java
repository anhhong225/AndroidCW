package com.example.yogaadmin;

import android.database.Cursor;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class EditYogaCourseActivity extends AppCompatActivity {
    private int yogaCourseId;
    private Spinner spDayOfWeek, spTime, spType;
    private EditText etCapacity, etDuration, etPrice, etDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_yoga_course);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        yogaCourseId = getIntent().getIntExtra("yoga_course_id", -1);
        if(yogaCourseId == -1){
            Toast.makeText(this,"Invalid course ID", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        spDayOfWeek = findViewById(R.id.spEditDayOfWeek);
        spTime = findViewById(R.id.spEditTime);
        spType = findViewById(R.id.spEditType);
        etCapacity = findViewById(R.id.etCapacity);
        etDuration = findViewById(R.id.etDuration);
        etPrice = findViewById(R.id.edEditPrice);
        etDescription = findViewById(R.id.edmEditDes);

        loadCourseData();
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void loadCourseData(){
        Cursor c = MainActivity.helper.getYogaCourseById(yogaCourseId);
        if (c != null && c.moveToFirst()){
            String dayOfWeek = c.getString(c.getColumnIndexOrThrow("dayofweek"));
            String time = c.getString(c.getColumnIndexOrThrow("time"));
            int capacity = c.getInt(c.getColumnIndexOrThrow("capacity"));
            String duration = c.getString(c.getColumnIndexOrThrow("duration"));
            float price = c.getFloat(c.getColumnIndexOrThrow("price"));
            String type = c.getString(c.getColumnIndexOrThrow("type"));
            String description = c.getString(c.getColumnIndexOrThrow("description"));

            setSpinnerSelection(spDayOfWeek, dayOfWeek);
            setSpinnerSelection(spTime, time);
            setSpinnerSelection(spType, type);

            etCapacity.setText(String.valueOf(capacity));
            etDuration.setText(duration);
            etPrice.setText(String.valueOf(price));
            etDescription.setText(description);
        }
        if (c != null) c.close();
    }
    private void setSpinnerSelection(Spinner spinner, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        if (adapter == null) return; // Prevent crash
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }
    public void onClickSaveYogaCourse(View v){
        if (spDayOfWeek.getAdapter() == null || spTime.getAdapter() == null || spType.getAdapter() == null) {
            Toast.makeText(this, "Spinners not initialized properly.", Toast.LENGTH_SHORT).show();
            return;
        }
        String dayOfWeek = spDayOfWeek.getSelectedItem().toString();
        String time = spTime.getSelectedItem().toString();
        String type = spType.getSelectedItem().toString();

        String capacityStr = etCapacity.getText().toString().trim();
        String duration = etDuration.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (capacityStr.isEmpty() || duration.isEmpty() || priceStr.isEmpty()
                || dayOfWeek.isEmpty() || time.isEmpty() || type.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        int capacity;
        float price;

        try {
            capacity = Integer.parseInt(capacityStr);
            price = Float.parseFloat(priceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid capacity or price format.", Toast.LENGTH_SHORT).show();
            return;
        }

        long result = MainActivity.helper.updateYogaCourse(
                yogaCourseId, dayOfWeek, time, capacity, duration, price, type, description
        );

        if (result > 0) {
            // Sync to Firebase
            FirebaseHelper firebaseHelper = new FirebaseHelper(this);
            firebaseHelper.uploadUpdatedYogaCourse(yogaCourseId);

            Toast.makeText(this, "Course updated successfully", Toast.LENGTH_SHORT).show();
            finish(); // Go back to previous screen
        } else {
            Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
        }
    }
    public void onClickReset(View v){
        loadCourseData();
    }
}
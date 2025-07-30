package com.example.yogaadmin;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.yogaadmin.objects.YogaCourse;

public class CreateYogaCourse extends AppCompatActivity {
    private Spinner spDayOfWeek, spTime, spType;
    private EditText etCapacity, etDuration, edPrice, edmDes;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_yoga_course);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        spDayOfWeek = findViewById(R.id.spDayOfWeek);
        spTime = findViewById(R.id.spTime);
        spType = findViewById(R.id.spType);
        etCapacity = findViewById(R.id.etCapacity);
        etDuration = findViewById(R.id.etDuration);
        edPrice = findViewById(R.id.edPrice);
        edmDes = findViewById(R.id.edmDes);
    }
    public void onClickCreateYogaCourse(View v) {
        String dayOfWeek = spDayOfWeek.getSelectedItem().toString();
        String time = spTime.getSelectedItem().toString();
        String type = spType.getSelectedItem().toString();
        String capacityStr = etCapacity.getText().toString().trim();
        String duration = etDuration.getText().toString().trim();
        String priceStr = edPrice.getText().toString().trim();
        String des = edmDes.getText().toString();

        if (dayOfWeek.isEmpty() || time.isEmpty() || type.isEmpty() ||
                capacityStr.isEmpty() || duration.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        int capacity;
        float price;
        try {
            capacity = Integer.parseInt(capacityStr);
            price = Float.parseFloat(priceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid capacity or price.", Toast.LENGTH_SHORT).show();
            return;
        }

        YogaCourse course = new YogaCourse();
        course.setDayofweek(dayOfWeek);
        course.setTime(time);
        course.setType(type);
        course.setCapacity(capacity);
        course.setDuration(duration);
        course.setPrice(price);
        course.setDescription(des);
        course.setIsSynced(0);
        course.setIsDeleted(0);

        // Insert into local DB and retrieve generated ID
        long id = MainActivity.helper.createNewYogaCourse(course);
        course.setId((int) id);  // Save the generated ID for Firebase

        // Upload to Firebase using the same ID
        FirebaseHelper firebaseHelper = new FirebaseHelper(this);
        firebaseHelper.createAYogaCourse(course);

        Toast.makeText(this, "A yoga class is just created.", Toast.LENGTH_SHORT).show();
        finish();
    }

    public void onClickClear(View v){
        etCapacity.setText("");
        etDuration.setText("");
        edPrice.setText("");
        edmDes.setText("");
        spDayOfWeek.setSelection(0);
        spTime.setSelection(0);
        spType.setSelection(0);
        Toast.makeText(this, "Fields reset.", Toast.LENGTH_SHORT).show();
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
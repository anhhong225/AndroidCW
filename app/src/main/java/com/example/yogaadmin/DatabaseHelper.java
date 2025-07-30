package com.example.yogaadmin;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.yogaadmin.objects.Schedule;
import com.example.yogaadmin.objects.YogaCourse;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private SQLiteDatabase database;
    public DatabaseHelper(Context context){
        super(context, "YogaDB", null, 4);
        database = getWritableDatabase();
    }
    public void onCreate(SQLiteDatabase db){
        try{
            String CREATE_TABLE_YOGACOURSES = "CREATE TABLE YogaCourse(" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "dayofweek TEXT, time TEXT, capacity INTEGER, duration TEXT, price FLOAT, " +
                    "type TEXT, description TEXT, " +
                    "isSynced INTEGER DEFAULT 0, isDeleted INTEGER DEFAULT 0)";
            db.execSQL(CREATE_TABLE_YOGACOURSES);

            String CREATE_TABLE_SCHEDULE = "CREATE TABLE Schedule(" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "yoga_course_id INTEGER NOT NULL, " +
                    "schedule_date TEXT NOT NULL, teacher TEXT NOT NULL, comment TEXT, " +
                    "isSynced INTEGER DEFAULT 0, isDeleted INTEGER DEFAULT 0, " +
                    "FOREIGN KEY(yoga_course_id) REFERENCES YogaCourse(_id) ON DELETE CASCADE)";
            db.execSQL(CREATE_TABLE_SCHEDULE);
        }catch (Exception e){
            Log.e("DB_ERROR", "onCreate: " + e.getMessage());
        }
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE YogaCourse ADD COLUMN isSynced INTEGER DEFAULT 0");
                db.execSQL("ALTER TABLE Schedule ADD COLUMN isSynced INTEGER DEFAULT 0");

                Log.d("DB_UPGRADE", "Columns isSynced successfully.");
            } catch (Exception e) {
                Log.e("DB_UPGRADE", "Error upgrading DB: " + e.getMessage());
            }
        }
        if(oldVersion < 4){
            try{
                db.execSQL("ALTER TABLE YogaCourse ADD COLUMN isDeleted INTEGER DEFAULT 0");
                db.execSQL("ALTER TABLE Schedule ADD COLUMN isDeleted INTEGER DEFAULT 0");
                Log.d("DB_UPGRADE", "Columns isDeleted successfully.");
            }catch (Exception e) {
                Log.e("DB_UPGRADE", "Error upgrading DB: " + e.getMessage());
            }
        }
    }
    public long createNewYogaCourse(YogaCourse course){
        ContentValues rowValues = new ContentValues();
        rowValues.put("dayofweek", course.getDayofweek());
        rowValues.put("time", course.getTime());
        rowValues.put("capacity", course.getCapacity());
        rowValues.put("duration", course.getDuration());
        rowValues.put("price", course.getPrice());
        rowValues.put("type", course.getType());
        rowValues.put("description", course.getDescription());
        rowValues.put("isSynced", 0);
        rowValues.put("isDeleted", 0);
        long rowId = database.insert("YogaCourse", null,rowValues);
        return rowId;
    }
    public Cursor readAllYogaCourse(){
        Cursor results = database.query("YogaCourse",
                new String[] {"_id","dayofweek","time","duration","capacity","price","type","description"},
                null,null,null,null,null);
        results.moveToFirst();
        return results;
    }
    public long createSchedule(Schedule schedule) {
        ContentValues rowValues = new ContentValues();
        rowValues.put("yoga_course_id", schedule.getYogaCourseId());
        rowValues.put("schedule_date", schedule.getDate());
        rowValues.put("teacher", schedule.getTeacher());
        rowValues.put("comment", schedule.getComment());
        rowValues.put("isSynced", 0);
        rowValues.put("isDeleted", 0);
        return database.insertOrThrow("Schedule", null, rowValues);
    }
    public Cursor getYogaCourseById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM YogaCourse WHERE _id = ?", new String[]{String.valueOf(id)});
    }
    public Cursor getSchedulesByCourseId(int courseId) {
        return database.query("Schedule", null, "yoga_course_id=?", new String[]{String.valueOf(courseId)}, null, null, "schedule_date");
    }
    public void deleteYogaCourse(int courseId) {
        database.delete("YogaCourse", "_id=?", new String[]{String.valueOf(courseId)});
    }
    public boolean deleteScheduleById(int id) {
        int rows = database.delete("Schedule", "_id = ?", new String[]{String.valueOf(id)});
        return rows > 0;
    }
    public void deleteScheduleByCourseId(int courseId) {
        database.delete("Schedule", "yoga_course_id=?", new String[]{String.valueOf(courseId)});
    }
    public int updateYogaCourse(int id, String dow, String time, int capacity, String duration, float p, String type, String des){
        ContentValues rowValues = new ContentValues();
        rowValues.put("dayofweek", dow);
        rowValues.put("time", time);
        rowValues.put("capacity", capacity);
        rowValues.put("duration", duration);
        rowValues.put("price", p);
        rowValues.put("type", type);
        rowValues.put("description", des);
        rowValues.put("isSynced", 0);
        return database.update("YogaCourse", rowValues, "_id = ?", new String[]{String.valueOf(id)});
    }
    public Cursor getScheduleById(long id){
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM Schedule WHERE _id = ?", new String[]{String.valueOf(id)});
    }

    public Schedule getScheduleObjectById(long id) {
        Schedule schedule = null;
        Cursor c = getScheduleById(id);  // reuse your existing cursor method
        if (c != null && c.moveToFirst()) {
            schedule = new Schedule();
            schedule.setId((int) id);
            schedule.setDate(c.getString(c.getColumnIndexOrThrow("schedule_date")));
            schedule.setTeacher(c.getString(c.getColumnIndexOrThrow("teacher")));
            schedule.setComment(c.getString(c.getColumnIndexOrThrow("comment")));
            schedule.setYogaCourseId(c.getInt(c.getColumnIndexOrThrow("yoga_course_id")));
            c.close();
        }
        return schedule;
    }

    public int updateSchedule(long scheduleId, String scheduleDate, String teacher, String comment) {
        ContentValues rowValues = new ContentValues();
        rowValues.put("schedule_date", scheduleDate);
        rowValues.put("teacher", teacher);
        rowValues.put("comment", comment);
        rowValues.put("isSynced", 0);
        return database.update("Schedule", rowValues, "_id = ?", new String[]{String.valueOf(scheduleId)});
    }

    public Cursor searchClass(String teacher, String date, String dayOfWeek) {
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder query = new StringBuilder("SELECT s.* FROM Schedule s JOIN YogaCourse y ON s.yoga_course_id = y._id WHERE 1=1");
        ArrayList<String> args = new ArrayList<>();

        if (!teacher.isEmpty()) {
            query.append(" AND s.teacher LIKE ?");
            args.add("%" + teacher + "%");
        }
        if (!date.isEmpty()) {
            query.append(" AND s.schedule_date = ?");
            args.add(date);
        }
        if (!dayOfWeek.isEmpty()) {
            query.append(" AND y.dayofweek = ?");
            args.add(dayOfWeek);
        }

        return db.rawQuery(query.toString(), args.toArray(new String[0]));
    }

    public void updateYogaCourseSyncStatus(int id, boolean isSynced) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("isSynced", isSynced ? 1 : 0);
        db.update("YogaCourse", values, "_id = ?", new String[]{String.valueOf(id)});
    }
    public void updateScheduleSyncStatus(int id, boolean isSynced) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("isSynced", isSynced ? 1 : 0);
        db.update("Schedule", values, "_id = ?", new String[]{String.valueOf(id)});
    }
    public void markYogaCourseAsDeleted(int courseId) {
        ContentValues values = new ContentValues();
        values.put("isDeleted", 1);
        values.put("isSynced", 0); // So it will sync to Firebase
        database.update("YogaCourse", values, "_id = ?", new String[]{String.valueOf(courseId)});
    }
    public boolean markScheduleAsDeleted(int id) {
        ContentValues values = new ContentValues();
        values.put("isDeleted", 1);
        values.put("isSynced", 0);
        int rows = database.update("Schedule", values, "_id = ?", new String[]{String.valueOf(id)});
        return rows > 0;
    }
    public List<YogaCourse> getDeletedUnsyncedYogaCourses() {
        List<YogaCourse> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT * FROM YogaCourse WHERE isDeleted = 1 AND isSynced = 0", null)) {
            if (cursor.moveToFirst()) {
                do {
                    YogaCourse course = new YogaCourse();
                    course.setId(cursor.getInt(cursor.getColumnIndexOrThrow("_id")));
                    list.add(course);
                } while (cursor.moveToNext());
            }
        }
        return list;
    }

    public List<Schedule> getDeletedUnsyncedSchedules() {
        List<Schedule> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT * FROM Schedule WHERE isDeleted = 1 AND isSynced = 0", null)) {
            if (cursor.moveToFirst()) {
                do {
                    Schedule schedule = new Schedule();
                    schedule.setId(cursor.getInt(cursor.getColumnIndexOrThrow("_id")));
                    list.add(schedule);
                } while (cursor.moveToNext());
            }
        }
        return list;
    }
    public void resetAllSyncFlags() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL("UPDATE YogaCourse SET isSynced = 0 WHERE isDeleted = 0");
            db.execSQL("UPDATE Schedule SET isSynced = 0 WHERE isDeleted = 0");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
    public List<YogaCourse> getAllYogaCourses() {
        List<YogaCourse> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM YogaCourse WHERE isDeleted = 0";

        try (Cursor cursor = db.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                YogaCourse course = new YogaCourse();
                course.setId(cursor.getInt(cursor.getColumnIndexOrThrow("_id")));
                course.setDayofweek(cursor.getString(cursor.getColumnIndexOrThrow("dayofweek")));
                course.setTime(cursor.getString(cursor.getColumnIndexOrThrow("time")));
                course.setCapacity(cursor.getInt(cursor.getColumnIndexOrThrow("capacity")));
                course.setDuration(cursor.getString(cursor.getColumnIndexOrThrow("duration")));
                course.setPrice(cursor.getFloat(cursor.getColumnIndexOrThrow("price")));
                course.setType(cursor.getString(cursor.getColumnIndexOrThrow("type")));
                course.setDescription(cursor.getString(cursor.getColumnIndexOrThrow("description")));
                // Optionally read flags
//                course.setIsSynced(cursor.getInt(cursor.getColumnIndexOrThrow("isSynced")));
//                course.setIsDeleted(cursor.getInt(cursor.getColumnIndexOrThrow("isDeleted")));
                list.add(course);
            }
        }
        return list;
    }
    public List<Schedule> getAllSchedules() {
        List<Schedule> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM Schedule WHERE isDeleted = 0";

        try (Cursor cursor = db.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                Schedule schedule = new Schedule();
                schedule.setId(cursor.getInt(cursor.getColumnIndexOrThrow("_id")));
                schedule.setYogaCourseId(cursor.getInt(cursor.getColumnIndexOrThrow("yoga_course_id")));
                schedule.setDate(cursor.getString(cursor.getColumnIndexOrThrow("schedule_date")));
                schedule.setTeacher(cursor.getString(cursor.getColumnIndexOrThrow("teacher")));
                schedule.setComment(cursor.getString(cursor.getColumnIndexOrThrow("comment")));
                // Optionally read flags
//                schedule.setIsSynced(cursor.getInt(cursor.getColumnIndexOrThrow("isSynced")));
//                schedule.setIsDeleted(cursor.getInt(cursor.getColumnIndexOrThrow("isDeleted")));
                list.add(schedule);
            }
        }
        return list;
    }
}

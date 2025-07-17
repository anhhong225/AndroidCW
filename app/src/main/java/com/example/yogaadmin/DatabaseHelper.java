package com.example.yogaadmin;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private SQLiteDatabase database;
    public DatabaseHelper(Context context){
        super(context, "YogaDB", null, 3);
        database = getWritableDatabase();
    }
    public void onCreate(SQLiteDatabase db){
        try{
            String CREATE_TABLE_YOGACOURSES = "create table YogaCourse(_id integer primary key autoincrement," +
                    "dayofweek text, time text, capacity integer, duration text, price float, type text, description text)";
            db.execSQL(CREATE_TABLE_YOGACOURSES);

            String CREATE_TABLE_SCHEDULE = "CREATE TABLE Schedule(_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "yoga_course_id INTEGER NOT NULL," +
                    "schedule_date TEXT NOT NULL," +
                    "teacher TEXT NOT NULL," +
                    "comment TEXT,"+
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
                Log.d("DB_UPGRADE", "Columns isSynced added successfully.");
            } catch (Exception e) {
                Log.e("DB_UPGRADE", "Error upgrading DB: " + e.getMessage());
            }
        }
    }
    public long createNewYogaCourse(String dow, String time, int capacity, String duration, float p, String type, String des){
        ContentValues rowValues = new ContentValues();
        rowValues.put("dayofweek", dow);
        rowValues.put("time", time);
        rowValues.put("capacity", capacity);
        rowValues.put("duration", duration);
        rowValues.put("price", p);
        rowValues.put("type", type);
        rowValues.put("description", des);
        rowValues.put("isSynced", 0);
        return database.insertOrThrow("YogaCourse", null, rowValues);
    }
    public Cursor readAllYogaCourse(){
        Cursor results = database.query("YogaCourse",
                new String[] {"_id","dayofweek","time","duration","capacity","price","type","description"},
                null,null,null,null,null);
        results.moveToFirst();
        return results;
    }
    public long createSchedule(int yogaCourseId, String scheduleDate, String teacher, String comment) {
        ContentValues rowValues = new ContentValues();
        rowValues.put("yoga_course_id", yogaCourseId);
        rowValues.put("schedule_date", scheduleDate);
        rowValues.put("teacher", teacher);
        rowValues.put("comment", comment);
        rowValues.put("isSynced", 0);
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
    public int updateSchedule(long scheduleId, String scheduleDate, String teacher, String comment) {
        ContentValues rowValues = new ContentValues();
        rowValues.put("schedule_date", scheduleDate);
        rowValues.put("teacher", teacher);
        rowValues.put("comment", comment);
        rowValues.put("isSynced", 0);
        return database.update("Schedule", rowValues, "_id = ?", new String[]{String.valueOf(scheduleId)});
    }
    /* for searching */
    public Cursor searchClassByTeacher(String partialName){
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM Schedule WHERE teacher LIKE ?",
                new String[]{"%" + partialName + "%"});
    }
    public Cursor searchClassByDate(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM Schedule WHERE schedule_date = ?", new String[]{date});
    }
    public Cursor searchClassByDay(String day) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT s.* FROM Schedule s " +
                "JOIN YogaCourse y ON s.yoga_course_id = y._id WHERE y.dayofweek = ?", new String[]{day});
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

    public List<YogaCourse> getUnsyncedYogaCourses() {
        List<YogaCourse> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        try(Cursor cursor = db.rawQuery("SELECT * FROM YogaCourse WHERE isSynced = 0", null)){
            if (cursor.moveToFirst()) {
                do {
                    YogaCourse course = new YogaCourse();
                    course.setId(cursor.getInt(cursor.getColumnIndexOrThrow("_id")));
                    course.setDayofweek(cursor.getString(cursor.getColumnIndexOrThrow("dayofweek")));
                    course.setTime(cursor.getString(cursor.getColumnIndexOrThrow("time")));
                    course.setCapacity(cursor.getInt(cursor.getColumnIndexOrThrow("capacity")));
                    course.setDuration(cursor.getString(cursor.getColumnIndexOrThrow("duration")));
                    course.setPrice(cursor.getFloat(cursor.getColumnIndexOrThrow("price")));
                    course.setType(cursor.getString(cursor.getColumnIndexOrThrow("type")));
                    course.setDescription(cursor.getString(cursor.getColumnIndexOrThrow("description")));
                    list.add(course);
                } while (cursor.moveToNext());
            }
        }
        return list;
    }

    public List<Schedule> getUnsyncedSchedules() {
        List<Schedule> listSchedules = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT * FROM Schedule WHERE isSynced = 0", null)) {
            if (cursor.moveToFirst()) {
                do {
                    Schedule schedule = new Schedule();
                    schedule.setId(cursor.getInt(cursor.getColumnIndexOrThrow("_id")));
                    schedule.setYogaCourseId(cursor.getInt(cursor.getColumnIndexOrThrow("yoga_course_id")));
                    schedule.setDate(cursor.getString(cursor.getColumnIndexOrThrow("schedule_date")));
                    listSchedules.add(schedule);
                } while (cursor.moveToNext());
            }
        }
        return listSchedules;
    }

    public void markYogaCourseAsSynced(int id) {
        ContentValues values = new ContentValues();
        values.put("isSynced", 1);
        database.update("YogaCourse", values, "_id = ?", new String[]{String.valueOf(id)});
    }

    public void markScheduleAsSynced(int id) {
        ContentValues values = new ContentValues();
        values.put("isSynced", 1);
        database.update("Schedule", values, "_id = ?", new String[]{String.valueOf(id)});
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
}

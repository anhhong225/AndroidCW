package com.example.yogaadmin;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {
    private SQLiteDatabase database;
    public DatabaseHelper(Context context){
        super(context, "YogaDB", null, 2);
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
        db.execSQL("DROP TABLE IF EXISTS YogaCourse");
        db.execSQL("DROP TABLE IF EXISTS Schedule");
        Log.w(this.getClass().getName(), "Upgrading DB to version " + newVersion + ", old data lost");
        onCreate(db);
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
        return database.update("YogaCourse", rowValues, "_id = ?", new String[]{String.valueOf(id)});
    }
    public Cursor getScheduleById(long id){
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM Schedule WHERE _id = ?", new String[]{String.valueOf(id)});
    }
    public int updateSchedule(long scheduleId, String scheduleDate, String teacher, String comment) {
        ContentValues values = new ContentValues();
        values.put("schedule_date", scheduleDate);
        values.put("teacher", teacher);
        values.put("comment", comment);

        return database.update("Schedule", values, "_id = ?", new String[]{String.valueOf(scheduleId)});
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
                "JOIN YogaCourse y ON s.course_id = y._id WHERE y.dayofweek = ?", new String[]{day});
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
}

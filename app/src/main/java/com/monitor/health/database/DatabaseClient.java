package com.monitor.health.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.monitor.health.dao.AppDatabase;

public class DatabaseClient {
    private static DatabaseClient instance;
    private AppDatabase appDatabase;

    // Adds the 'steps' table introduced in version 11, preserving all existing data.
    static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `steps` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`delta` INTEGER NOT NULL, " +
                "`epoch_millis` INTEGER NOT NULL, " +
                "`created_at` INTEGER, " +
                "`status` INTEGER NOT NULL DEFAULT 0)"
            );
        }
    };

    private DatabaseClient(Context context) {
        appDatabase = Room.databaseBuilder(context.getApplicationContext(),
                AppDatabase.class, "vital_watch_database")
                .addMigrations(MIGRATION_10_11)
                .fallbackToDestructiveMigration() // fallback for any other version gap
                .allowMainThreadQueries()
                .build();
    }

    public static synchronized DatabaseClient getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseClient(context);
        }
        return instance;
    }

    public AppDatabase getAppDatabase() {
        return appDatabase;
    }
}

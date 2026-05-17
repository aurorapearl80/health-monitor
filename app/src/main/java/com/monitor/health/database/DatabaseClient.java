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

    // Adds BLE user profile columns to user_drwatch table introduced in version 12.
    static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE user_drwatch ADD COLUMN firstName TEXT");
            database.execSQL("ALTER TABLE user_drwatch ADD COLUMN lastName TEXT");
            database.execSQL("ALTER TABLE user_drwatch ADD COLUMN state TEXT");
            database.execSQL("ALTER TABLE user_drwatch ADD COLUMN country TEXT");
            database.execSQL("ALTER TABLE user_drwatch ADD COLUMN zipCode TEXT");
            database.execSQL("ALTER TABLE user_drwatch ADD COLUMN completeAddress TEXT");
            database.execSQL("ALTER TABLE user_drwatch ADD COLUMN height TEXT");
            database.execSQL("ALTER TABLE user_drwatch ADD COLUMN weight TEXT");
            database.execSQL("ALTER TABLE user_drwatch ADD COLUMN profileImageUrl TEXT");
            database.execSQL("ALTER TABLE user_drwatch ADD COLUMN status TEXT");
            database.execSQL("ALTER TABLE user_drwatch ADD COLUMN generalPractitioner TEXT");
            database.execSQL("ALTER TABLE user_drwatch ADD COLUMN primaryInsuranceName TEXT");
            database.execSQL("ALTER TABLE user_drwatch ADD COLUMN homeNumber TEXT");
            database.execSQL("ALTER TABLE user_drwatch ADD COLUMN angelSupport TEXT");
        }
    };

    private DatabaseClient(Context context) {
        appDatabase = Room.databaseBuilder(context.getApplicationContext(),
                AppDatabase.class, "vital_watch_database")
                .addMigrations(MIGRATION_10_11, MIGRATION_11_12)
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

package com.stupidbeauty.joyman.data.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.stupidbeauty.joyman.data.database.dao.ProjectDao;
import com.stupidbeauty.joyman.data.database.dao.TaskDao;
import com.stupidbeauty.joyman.data.database.entity.Project;
import com.stupidbeauty.joyman.data.database.entity.Task;

/**
 * JoyMan 应用数据库
 * 
 * 数据库信息：
 * - 名称：joyman-db
 * - 版本：2（支持任务关联项目）
 * - 实体类：Task, Project
 * 
 * @author 太极美术工程狮狮长
 * @version 2.0.0
 * @since 2026-03-31
 */
@Database(
    entities = {
        Task.class,
        Project.class
    },
    version = 2,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "joyman-db";
    private static volatile AppDatabase INSTANCE;
    
    public abstract TaskDao taskDao();
    public abstract ProjectDao projectDao();
    
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = buildDatabase(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }
    
    @NonNull
    private static AppDatabase buildDatabase(Context context) {
        return Room.databaseBuilder(
                context,
                AppDatabase.class,
                DATABASE_NAME
            )
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .addCallback(new Callback() {
                @Override
                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                    super.onCreate(db);
                    android.util.Log.d("AppDatabase", "Database created successfully");
                }
                
                @Override
                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                    super.onOpen(db);
                    android.util.Log.d("AppDatabase", "Database opened");
                }
            })
            .build();
    }
    
    /**
     * 数据库迁移：版本 1 → 版本 2
     * 
     * 变更内容：
     * - 为 tasks 表添加 project_id 字段（可选，外键）
     * - 创建索引优化按项目查询的性能
     */
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 添加 project_id 字段（允许 NULL，默认值 NULL）
            database.execSQL("ALTER TABLE tasks ADD COLUMN project_id INTEGER DEFAULT NULL");
            
            // 创建索引优化查询性能
            database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_project_id ON tasks(project_id)");
            
            android.util.Log.d("AppDatabase", "Migrated from version 1 to version 2: Added project_id column");
        }
    };
    
    public static void closeInstance() {
        if (INSTANCE != null && INSTANCE.isOpen()) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}
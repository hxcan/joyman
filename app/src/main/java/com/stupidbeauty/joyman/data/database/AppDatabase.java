package com.stupidbeauty.joyman.data.database;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.stupidbeauty.joyman.data.database.dao.CommentDao;
import com.stupidbeauty.joyman.data.database.dao.ProjectDao;
import com.stupidbeauty.joyman.data.database.dao.TaskDao;
import com.stupidbeauty.joyman.data.database.entity.Comment;
import com.stupidbeauty.joyman.data.database.entity.Project;
import com.stupidbeauty.joyman.data.database.entity.Task;
import com.stupidbeauty.joyman.util.LogUtils;


/**
 * JoyMan 应用数据库
 */
@Database(entities = {Task.class, Project.class, Comment.class}, version = 5, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "joyman-db";
    private static final String TAG = "AppDatabase";
    private static volatile AppDatabase INSTANCE;
    
    public abstract TaskDao taskDao();
    public abstract ProjectDao projectDao();
    public abstract CommentDao commentDao();
    
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
        return Room.databaseBuilder(context, AppDatabase.class, DATABASE_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .fallbackToDestructiveMigration()
            .addCallback(new Callback() {
                @Override
                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                    LogUtils.getInstance().d(TAG, "Database created");
                }
                @Override
                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                    LogUtils.getInstance().d(TAG, "Database opened, version: " + db.getVersion());
                }
            })
            .build();
    }
    
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            if (!checkColumnExists(database, "tasks", "project_id")) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN project_id INTEGER DEFAULT NULL");
            }
            database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_project_id ON tasks(project_id)");
        }
    };
    
    /**
     * 数据库迁移：版本 2 → 版本 3
     * 重新创建表以修改 status 默认值，并在复制数据时更新 status (0→1)
     */
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            LogUtils.getInstance().d(TAG, "MIGRATION_2_3: START");
            
            // 步骤 1: 创建临时表（status 默认值为 1）
            database.execSQL(
                "CREATE TABLE tasks_temp (" +
                "id INTEGER PRIMARY KEY NOT NULL, " +
                "title TEXT DEFAULT '', " +
                "description TEXT DEFAULT '', " +
                "status INTEGER NOT NULL DEFAULT 1, " +
                "priority INTEGER NOT NULL DEFAULT 2, " +
                "project_id INTEGER DEFAULT NULL, " +
                "created_at INTEGER NOT NULL, " +
                "updated_at INTEGER NOT NULL, " +
                "due_date INTEGER DEFAULT NULL, " +
                "tags TEXT DEFAULT '')"
            );
            
            // 步骤 2: 复制数据，并使用 CASE 语句更新 status (0→1)
            database.execSQL(
                "INSERT INTO tasks_temp (id, title, description, status, priority, project_id, created_at, updated_at, due_date, tags) " +
                "SELECT id, title, description, " +
                "       CASE WHEN status = 0 THEN 1 ELSE status END, " +  // ← 关键：转换 status
                "       priority, project_id, created_at, updated_at, due_date, tags " +
                "FROM tasks"
            );
            
            // 步骤 3: 删除旧表
            database.execSQL("DROP TABLE tasks");
            
            // 步骤 4: 重命名
            database.execSQL("ALTER TABLE tasks_temp RENAME TO tasks");
            
            // 步骤 5: 创建索引
            database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_project_id ON tasks(project_id)");
            
            LogUtils.getInstance().i(TAG, "MIGRATION_2_3: ✅ Schema and data updated successfully");
        }
    };
    
    /**
     * 数据库迁移：版本 3 → 版本 4
     * 为 tasks 表添加 parent_id 列以支持子任务功能
     */
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            LogUtils.getInstance().d(TAG, "MIGRATION_3_4: START - Adding parent_id column for subtask support");
            
            // 检查 parent_id 列是否已存在（防止重复迁移）
            if (!checkColumnExists(database, "tasks", "parent_id")) {
                // 添加 parent_id 列（允许 NULL，不影响现有任务）
                database.execSQL("ALTER TABLE tasks ADD COLUMN parent_id INTEGER DEFAULT NULL");
                
                // 创建索引优化子任务查询性能
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_parent_id ON tasks(parent_id)");
                
                LogUtils.getInstance().i(TAG, "MIGRATION_3_4: ✅ parent_id column added successfully");
            } else {
                LogUtils.getInstance().w(TAG, "MIGRATION_3_4: ⚠️ parent_id column already exists, skipping");
            }
        }
    };
    
    /**
     * 数据库迁移：版本 4 → 版本 5
     * 创建 comments 表以支持任务评论功能
     */
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            LogUtils.getInstance().d(TAG, "MIGRATION_4_5: START - Creating comments table for comment feature");
            
            // 创建 comments 表
            // 注意：content 和 author 允许为 NULL，与 Comment 实体类保持一致
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `comments` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`issue_id` INTEGER NOT NULL, " +
                "`content` TEXT, " +
                "`author` TEXT, " +
                "`created_on` INTEGER NOT NULL, " +
                "FOREIGN KEY(`issue_id`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
            );
            
            // 创建 issue_id 索引优化查询性能
            database.execSQL("CREATE INDEX IF NOT EXISTS index_comments_issue_id ON comments(issue_id)");
            
            LogUtils.getInstance().i(TAG, "MIGRATION_4_5: ✅ comments table created successfully");
        }
    };
    
    private static boolean checkColumnExists(SupportSQLiteDatabase db, String tableName, String columnName) {
        Cursor cursor = null;
        try {
            cursor = db.query("PRAGMA table_info(" + tableName + ")");
            while (cursor.moveToNext()) {
                if (cursor.getString(cursor.getColumnIndexOrThrow("name")).equals(columnName)) {
                    return true;
                }
            }
            return false;
        } finally {
            if (cursor != null) cursor.close();
        }
    }
    
    public static void closeInstance() {
        if (INSTANCE != null && INSTANCE.isOpen()) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}
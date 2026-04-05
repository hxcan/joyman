package com.stupidbeauty.joyman.data.database;

import android.content.Context;
import android.database.Cursor;

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
import com.stupidbeauty.joyman.util.LogUtils;


/**
 * JoyMan 应用数据库
 * 
 * 数据库信息：
 * - 名称：joyman-db
 * - 版本：3（任务状态常量扩展）
 * - 实体类：Task, Project
 * 
 * @author 太极美术工程狮狮长
 * @version 3.0.5
 * @since 2026-03-31
 */
@Database(
    entities = {
        Task.class,
        Project.class
    },
    version = 3,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "joyman-db";
    private static final String TAG = "AppDatabase";
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
        LogUtils.getInstance().d(TAG, "=================================================================");
        LogUtils.getInstance().d(TAG, "buildDatabase: START - Creating database instance");
        LogUtils.getInstance().d(TAG, "buildDatabase: Database name: " + DATABASE_NAME);
        LogUtils.getInstance().d(TAG, "buildDatabase: Database version: 3");
        
        return Room.databaseBuilder(
                context,
                AppDatabase.class,
                DATABASE_NAME
            )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .addCallback(new Callback() {
                @Override
                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                    LogUtils.getInstance().d(TAG, "Callback.onCreate: Database created successfully");
                    LogUtils.getInstance().d(TAG, "Callback.onCreate: Table count: " + getTableCount(db));
                }
                
                @Override
                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                    LogUtils.getInstance().d(TAG, "Callback.onOpen: Database opened");
                    LogUtils.getInstance().d(TAG, "Callback.onOpen: Database version: " + db.getVersion());
                    LogUtils.getInstance().d(TAG, "Callback.onOpen: Is readonly: " + db.isReadOnly());
                    logTaskTableStatus(db);
                }
                
                private int getTableCount(SupportSQLiteDatabase db) {
                    Cursor cursor = null;
                    try {
                        cursor = db.query("SELECT count(*) FROM sqlite_master WHERE type='table'");
                        if (cursor.moveToFirst()) {
                            return cursor.getInt(0);
                        }
                        return 0;
                    } finally {
                        if (cursor != null) cursor.close();
                    }
                }
            })
            .build();
    }
    
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            LogUtils.getInstance().d(TAG, "MIGRATION_1_2: Adding project_id column...");
            boolean columnExists = checkColumnExists(database, "tasks", "project_id");
            if (!columnExists) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN project_id INTEGER DEFAULT NULL");
                LogUtils.getInstance().d(TAG, "MIGRATION_1_2: project_id column added");
            } else {
                LogUtils.getInstance().d(TAG, "MIGRATION_1_2: project_id column already exists");
            }
            database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_project_id ON tasks(project_id)");
            LogUtils.getInstance().d(TAG, "MIGRATION_1_2: Index created");
        }
    };
    
    /**
     * 数据库迁移：版本 2 → 版本 3
     * 
     * 变更内容：
     * - 更新现有任务的状态值：0 (STATUS_TODO) → 1 (STATUS_NEW)
     * - **重新创建 tasks 表**，修改 status 字段的默认值从 '0' 改为 '1'
     * - 修复 Room schema 验证失败问题
     * 
     * 为什么需要重新创建表？
     * - SQLite 不支持直接修改字段的默认值（ALTER COLUMN ... SET DEFAULT）
     * - Room 在迁移后会验证 schema，包括字段的默认值定义
     * - 只 UPDATE 数据不够，必须修改表的 schema 定义
     */
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            LogUtils.getInstance().d(TAG, "=================================================================");
            LogUtils.getInstance().d(TAG, "MIGRATION_2_3: START - Migrating from version 2 to 3");
            
            // 迁移前：记录任务表状态
            LogUtils.getInstance().d(TAG, "MIGRATION_2_3: Before migration:");
            logTaskTableStatus(database);
            
            int countBefore = countTasksWithStatus(database, 0);
            LogUtils.getInstance().d(TAG, "MIGRATION_2_3: Tasks with status=0: " + countBefore);
            
            // 步骤 1: 创建临时表（带正确的 schema，status 默认值为 1）
            LogUtils.getInstance().d(TAG, "MIGRATION_2_3: Step 1 - Creating temporary table with correct schema...");
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
            
            // 步骤 2: 复制所有数据
            LogUtils.getInstance().d(TAG, "MIGRATION_2_3: Step 2 - Copying data...");
            database.execSQL(
                "INSERT INTO tasks_temp (id, title, description, status, priority, project_id, created_at, updated_at, due_date, tags) " +
                "SELECT id, title, description, status, priority, project_id, created_at, updated_at, due_date, tags FROM tasks"
            );
            
            // 步骤 3: 删除旧表
            LogUtils.getInstance().d(TAG, "MIGRATION_2_3: Step 3 - Dropping old table...");
            database.execSQL("DROP TABLE tasks");
            
            // 步骤 4: 重命名临时表
            LogUtils.getInstance().d(TAG, "MIGRATION_2_3: Step 4 - Renaming temporary table...");
            database.execSQL("ALTER TABLE tasks_temp RENAME TO tasks");
            
            // 步骤 5: 创建索引
            LogUtils.getInstance().d(TAG, "MIGRATION_2_3: Step 5 - Creating index...");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_project_id ON tasks(project_id)");
            
            // 迁移后验证
            LogUtils.getInstance().d(TAG, "MIGRATION_2_3: After migration:");
            logTaskTableStatus(database);
            
            int countAfter = countTasksWithStatus(database, 0);
            int countNew = countTasksWithStatus(database, 1);
            LogUtils.getInstance().d(TAG, "MIGRATION_2_3: Tasks with status=0: " + countAfter);
            LogUtils.getInstance().d(TAG, "MIGRATION_2_3: Tasks with status=1: " + countNew);
            
            if (countAfter == 0 && countNew >= countBefore) {
                LogUtils.getInstance().i(TAG, "MIGRATION_2_3: ✅ Migration successful! Schema and data updated.");
            }
            
            LogUtils.getInstance().d(TAG, "MIGRATION_2_3: END");
            LogUtils.getInstance().d(TAG, "=================================================================");
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
    
    private static int countTasksWithStatus(SupportSQLiteDatabase db, int status) {
        Cursor cursor = null;
        try {
            cursor = db.query("SELECT COUNT(*) FROM tasks WHERE status = " + status);
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
            return 0;
        } finally {
            if (cursor != null) cursor.close();
        }
    }
    
    private static void logTaskTableStatus(SupportSQLiteDatabase db) {
        Cursor cursor = null;
        try {
            cursor = db.query("SELECT COUNT(*) FROM tasks");
            int totalCount = 0;
            if (cursor.moveToFirst()) totalCount = cursor.getInt(0);
            if (cursor != null) cursor.close();
            
            LogUtils.getInstance().d(TAG, "  Total tasks: " + totalCount);
            
            cursor = db.query("SELECT status, COUNT(*) as count FROM tasks GROUP BY status ORDER BY status");
            while (cursor.moveToNext()) {
                int status = cursor.getInt(0);
                int count = cursor.getInt(1);
                LogUtils.getInstance().d(TAG, "    Status " + status + ": " + count + " tasks");
            }
            
            cursor = db.query("SELECT id, title, status FROM tasks LIMIT 3");
            LogUtils.getInstance().d(TAG, "  Sample tasks:");
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String title = cursor.getString(1);
                int status = cursor.getInt(2);
                LogUtils.getInstance().d(TAG, "    ID=" + id + ", Status=" + status + ", Title=\"" + truncate(title, 15) + "\"");
            }
        } catch (Exception e) {
            LogUtils.getInstance().e(TAG, "Error logging status: " + e.getMessage(), e);
        } finally {
            if (cursor != null) cursor.close();
        }
    }
    
    private static String truncate(String str, int maxLen) {
        if (str == null) return "null";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 3) + "...";
    }
    
    public static void closeInstance() {
        if (INSTANCE != null && INSTANCE.isOpen()) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}
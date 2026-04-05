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
 * @version 3.0.4
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
            // 容错机制：当 migration 失败时，允许重建数据库
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
                    
                    // 记录任务表的状态
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
            LogUtils.getInstance().d(TAG, "=================================================================");
            LogUtils.getInstance().d(TAG, "MIGRATION_1_2: START - Migrating from version 1 to 2");
            
            // 迁移前检查
            LogUtils.getInstance().d(TAG, "MIGRATION_1_2: Before migration - Checking if project_id column exists...");
            boolean columnExists = checkColumnExists(database, "tasks", "project_id");
            LogUtils.getInstance().d(TAG, "MIGRATION_1_2: project_id column exists: " + columnExists);
            
            if (!columnExists) {
                // 添加 project_id 字段（允许 NULL，默认值 NULL）
                LogUtils.getInstance().d(TAG, "MIGRATION_1_2: Executing: ALTER TABLE tasks ADD COLUMN project_id INTEGER DEFAULT NULL");
                database.execSQL("ALTER TABLE tasks ADD COLUMN project_id INTEGER DEFAULT NULL");
                LogUtils.getInstance().d(TAG, "MIGRATION_1_2: project_id column added successfully");
            } else {
                LogUtils.getInstance().d(TAG, "MIGRATION_1_2: project_id column already exists, skipping");
            }
            
            // 创建索引优化查询性能
            LogUtils.getInstance().d(TAG, "MIGRATION_1_2: Executing: CREATE INDEX IF NOT EXISTS index_tasks_project_id ON tasks(project_id)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_project_id ON tasks(project_id)");
            LogUtils.getInstance().d(TAG, "MIGRATION_1_2: Index created successfully");
            
            // 迁移后验证
            LogUtils.getInstance().d(TAG, "MIGRATION_1_2: After migration - Verifying changes...");
            columnExists = checkColumnExists(database, "tasks", "project_id");
            LogUtils.getInstance().d(TAG, "MIGRATION_1_2: project_id column exists after migration: " + columnExists);
            
            LogUtils.getInstance().d(TAG, "MIGRATION_1_2: END - Migration from version 1 to 2 completed");
            LogUtils.getInstance().d(TAG, "=================================================================");
        }
    };
    
    /**
     * 数据库迁移：版本 2 → 版本 3
     * 
     * 变更内容：
     * - Task 实体类状态常量扩展（STATUS_NEW=1, STATUS_IN_PROGRESS=2, STATUS_RESOLVED=3, STATUS_FEEDBACK=4, STATUS_CLOSED=5）
     * - 更新现有任务的状态值：0 (STATUS_TODO) → 1 (STATUS_NEW)
     * - 修复 Room schema 验证失败问题
     */
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            LogUtils.getInstance().d(TAG, "=================================================================");
            LogUtils.getInstance().d(TAG, "MIGRATION_2_3: START - Migrating from version 2 to 3");
            
            // 迁移前：记录任务表状态
            LogUtils.getInstance().d(TAG, "MIGRATION_2_3: Before migration - Task table status:");
            logTaskTableStatus(database);
            
            // 统计需要更新的任务数量
            int countBefore = countTasksWithStatus(database, 0);
            LogUtils.getInstance().d(TAG, "MIGRATION_2_3: Tasks with status=0 (old STATUS_TODO): " + countBefore);
            
            if (countBefore > 0) {
                // 将旧版本 status=0 (STATUS_TODO) 更新为 status=1 (STATUS_NEW)
                LogUtils.getInstance().d(TAG, "MIGRATION_2_3: Executing: UPDATE tasks SET status = 1 WHERE status = 0");
                database.execSQL("UPDATE tasks SET status = 1 WHERE status = 0");
                LogUtils.getInstance().d(TAG, "MIGRATION_2_3: UPDATE executed successfully");
                
                // 验证更新结果
                int countAfter = countTasksWithStatus(database, 0);
                LogUtils.getInstance().d(TAG, "MIGRATION_2_3: Tasks with status=0 after migration: " + countAfter);
                
                int countNew = countTasksWithStatus(database, 1);
                LogUtils.getInstance().d(TAG, "MIGRATION_2_3: Tasks with status=1 (new STATUS_NEW): " + countNew);
                
                if (countAfter == 0 && countNew >= countBefore) {
                    LogUtils.getInstance().i(TAG, "MIGRATION_2_3: ✅ Migration successful! All tasks updated from status 0 to 1");
                } else {
                    LogUtils.getInstance().e(TAG, "MIGRATION_2_3: ⚠️ Warning: Migration may not have completed successfully");
                    LogUtils.getInstance().e(TAG, "MIGRATION_2_3: Expected " + countBefore + " tasks to be updated");
                }
            } else {
                LogUtils.getInstance().d(TAG, "MIGRATION_2_3: No tasks with status=0 found, skipping UPDATE");
            }
            
            // 迁移后：再次记录任务表状态
            LogUtils.getInstance().d(TAG, "MIGRATION_2_3: After migration - Task table status:");
            logTaskTableStatus(database);
            
            LogUtils.getInstance().d(TAG, "MIGRATION_2_3: END - Migration from version 2 to 3 completed");
            LogUtils.getInstance().d(TAG, "=================================================================");
        }
    };
    
    /**
     * 检查表中是否存在指定列
     */
    private static boolean checkColumnExists(SupportSQLiteDatabase db, String tableName, String columnName) {
        Cursor cursor = null;
        try {
            cursor = db.query("PRAGMA table_info(" + tableName + ")");
            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                if (name.equals(columnName)) {
                    return true;
                }
            }
            return false;
        } finally {
            if (cursor != null) cursor.close();
        }
    }
    
    /**
     * 统计指定状态的任务数量
     */
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
    
    /**
     * 记录任务表的详细状态
     */
    private static void logTaskTableStatus(SupportSQLiteDatabase db) {
        Cursor cursor = null;
        try {
            // 获取总任务数
            cursor = db.query("SELECT COUNT(*) FROM tasks");
            int totalCount = 0;
            if (cursor.moveToFirst()) {
                totalCount = cursor.getInt(0);
            }
            if (cursor != null) cursor.close();
            
            LogUtils.getInstance().d(TAG, "  Task table total count: " + totalCount);
            
            // 按状态分组统计
            cursor = db.query("SELECT status, COUNT(*) as count FROM tasks GROUP BY status ORDER BY status");
            while (cursor.moveToNext()) {
                int status = cursor.getInt(0);
                int count = cursor.getInt(1);
                String statusText = getStatusText(status);
                LogUtils.getInstance().d(TAG, "    Status " + status + " (" + statusText + "): " + count + " tasks");
            }
            
            // 显示前 5 个任务的详细信息
            LogUtils.getInstance().d(TAG, "  Sample tasks (first 5):");
            cursor = db.query("SELECT id, title, status, priority FROM tasks LIMIT 5");
            int idx = 0;
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String title = cursor.getString(1);
                int status = cursor.getInt(2);
                int priority = cursor.getInt(3);
                LogUtils.getInstance().d(TAG, "    [" + (++idx) + "] ID=" + id + ", Title=\"" + truncate(title, 20) + 
                      "\", Status=" + status + ", Priority=" + priority);
            }
            
        } catch (Exception e) {
            LogUtils.getInstance().e(TAG, "  Error logging task table status: " + e.getMessage(), e);
        } finally {
            if (cursor != null) cursor.close();
        }
    }
    
    /**
     * 获取状态的文本描述
     */
    private static String getStatusText(int status) {
        switch (status) {
            case 0: return "STATUS_TODO (旧)";
            case 1: return "STATUS_NEW (新)";
            case 2: return "STATUS_IN_PROGRESS";
            case 3: return "STATUS_RESOLVED";
            case 4: return "STATUS_FEEDBACK";
            case 5: return "STATUS_CLOSED";
            default: return "UNKNOWN";
        }
    }
    
    /**
     * 截断字符串
     */
    private static String truncate(String str, int maxLen) {
        if (str == null) return "null";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 3) + "...";
    }
    
    public static void closeInstance() {
        if (INSTANCE != null && INSTANCE.isOpen()) {
            LogUtils.getInstance().d(TAG, "closeInstance: Closing database instance");
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}
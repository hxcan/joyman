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
import com.stupidbeauty.joyman.data.database.dao.RelationDao;
import com.stupidbeauty.joyman.data.database.dao.TaskDao;
import com.stupidbeauty.joyman.data.database.entity.Comment;
import com.stupidbeauty.joyman.data.database.entity.Project;
import com.stupidbeauty.joyman.data.database.entity.Relation;
import com.stupidbeauty.joyman.data.database.entity.Task;
import com.stupidbeauty.joyman.util.LogUtils;



/**
 * JoyMan 应用数据库
 */
@Database(entities = {Task.class, Project.class, Comment.class, Relation.class}, version = 8, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "joyman-db";
    private static final String TAG = "AppDatabase";
    private static volatile AppDatabase INSTANCE;
    
    public abstract TaskDao taskDao();
    public abstract ProjectDao projectDao();
    public abstract CommentDao commentDao();
    public abstract RelationDao relationDao();
    
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
            .fallbackToDestructiveMigration()
            .addCallback(new Callback() {
                @Override
                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                    LogUtils.getInstance().d(TAG, "Database created");
                }
                @Override
                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                    LogUtils.getInstance().d(TAG, "Database opened, version: " + db.getVersion());
                    // 打印 relations 表结构
                    printTableSchema(db, "relations");
                }
            })
            .build();
    }
    
    /**
     * 打印表结构调试信息
     */
    private static void printTableSchema(SupportSQLiteDatabase db, String tableName) {
        LogUtils.getInstance().d(TAG, "=== Table Schema: " + tableName + " ===");
        Cursor cursor = null;
        try {
            cursor = db.query("PRAGMA table_info(" + tableName + ")");
            while (cursor.moveToNext()) {
                String cid = cursor.getString(cursor.getColumnIndexOrThrow("cid"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                String notnull = cursor.getString(cursor.getColumnIndexOrThrow("notnull"));
                String dfltValue = cursor.getString(cursor.getColumnIndexOrThrow("dflt_value"));
                String pk = cursor.getString(cursor.getColumnIndexOrThrow("pk"));
                LogUtils.getInstance().d(TAG, "  Column: " + name + ", Type: " + type + ", NotNull: " + notnull + ", Default: " + dfltValue + ", PK: " + pk);
            }
            LogUtils.getInstance().d(TAG, "=== End Schema ===");
        } catch (Exception e) {
            LogUtils.getInstance().e(TAG, "Error printing table schema", e);
        } finally {
            if (cursor != null) cursor.close();
        }
    }
    
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            LogUtils.getInstance().d(TAG, "MIGRATION_1_2: START");
            if (!checkColumnExists(database, "tasks", "project_id")) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN project_id INTEGER DEFAULT NULL");
                LogUtils.getInstance().d(TAG, "MIGRATION_1_2: Added project_id column");
            } else {
                LogUtils.getInstance().w(TAG, "MIGRATION_1_2: project_id column already exists");
            }
            database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_project_id ON tasks(project_id)");
            LogUtils.getInstance().i(TAG, "MIGRATION_1_2: ✅ Completed");
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
            LogUtils.getInstance().d(TAG, "MIGRATION_2_3: Creating temporary table...");
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
            LogUtils.getInstance().d(TAG, "MIGRATION_2_3: Temporary table created");
            
            // 步骤 2: 复制数据，并使用 CASE 语句更新 status (0→1)
            LogUtils.getInstance().d(TAG, "MIGRATION_2_3: Copying data...");
            database.execSQL(
                "INSERT INTO tasks_temp (id, title, description, status, priority, project_id, created_at, updated_at, due_date, tags) " +
                "SELECT id, title, description, " +
                "       CASE WHEN status = 0 THEN 1 ELSE status END, " +  // ← 关键：转换 status
                "       priority, project_id, created_at, updated_at, due_date, tags " +
                "FROM tasks"
            );
            LogUtils.getInstance().d(TAG, "MIGRATION_2_3: Data copied");
            
            // 步骤 3: 删除旧表
            LogUtils.getInstance().d(TAG, "MIGRATION_2_3: Dropping old table...");
            database.execSQL("DROP TABLE tasks");
            LogUtils.getInstance().d(TAG, "MIGRATION_2_3: Old table dropped");
            
            // 步骤 4: 重命名
            LogUtils.getInstance().d(TAG, "MIGRATION_2_3: Renaming table...");
            database.execSQL("ALTER TABLE tasks_temp RENAME TO tasks");
            LogUtils.getInstance().d(TAG, "MIGRATION_2_3: Table renamed");
            
            // 步骤 5: 创建索引
            LogUtils.getInstance().d(TAG, "MIGRATION_2_3: Creating index...");
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
                LogUtils.getInstance().d(TAG, "MIGRATION_3_4: Adding parent_id column...");
                database.execSQL("ALTER TABLE tasks ADD COLUMN parent_id INTEGER DEFAULT NULL");
                
                // 创建索引优化子任务查询性能
                LogUtils.getInstance().d(TAG, "MIGRATION_3_4: Creating index...");
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
            LogUtils.getInstance().d(TAG, "MIGRATION_4_5: Creating comments table...");
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
            LogUtils.getInstance().d(TAG, "MIGRATION_4_5: Creating index...");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_comments_issue_id ON comments(issue_id)");
            
            LogUtils.getInstance().i(TAG, "MIGRATION_4_5: ✅ comments table created successfully");
        }
    };
    
    /**
     * 数据库迁移：版本 5 → 版本 6
     * 创建 relations 表以支持任务阻塞关系功能
     */
    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            LogUtils.getInstance().d(TAG, "MIGRATION_5_6: START - Creating relations table for task blocking feature");
            
            // 创建 relations 表（type 字段定义为 TEXT NOT NULL，与 Relation.java 实体类 String 类型一致）
            LogUtils.getInstance().d(TAG, "MIGRATION_5_6: Creating relations table with type TEXT NOT NULL...");
            try {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `relations` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`issue_id` INTEGER NOT NULL, " +
                    "`related_issue_id` INTEGER NOT NULL, " +
                    "`type` TEXT NOT NULL, " +
                    "`created_at` INTEGER NOT NULL)"
                );
                LogUtils.getInstance().d(TAG, "MIGRATION_5_6: Relations table created successfully");
                
                // 打印表结构
                printTableSchema(database, "relations");
            } catch (Exception e) {
                LogUtils.getInstance().e(TAG, "MIGRATION_5_6: ❌ Failed to create relations table", e);
                throw e;
            }
            
            // 创建索引优化查询性能
            LogUtils.getInstance().d(TAG, "MIGRATION_5_6: Creating indexes...");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_relations_issue_id ON relations(issue_id)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_relations_related_issue_id ON relations(related_issue_id)");
            LogUtils.getInstance().d(TAG, "MIGRATION_5_6: Indexes created");
            
            LogUtils.getInstance().i(TAG, "MIGRATION_5_6: ✅ relations table created successfully");
        }
    };
    
    /**
     * 数据库迁移：版本 6 → 版本 7
     * 为 relations 表添加外键约束
     */
    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            LogUtils.getInstance().d(TAG, "MIGRATION_6_7: START - Adding foreign key constraints to relations table");
            
            // 由于 SQLite 不支持直接添加外键约束，需要重建表
            // 步骤 1: 创建临时表（包含外键约束，type 字段定义为 TEXT NOT NULL）
            LogUtils.getInstance().d(TAG, "MIGRATION_6_7: Creating temporary table with foreign keys...");
            try {
                database.execSQL(
                    "CREATE TABLE relations_temp (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`issue_id` INTEGER NOT NULL, " +
                    "`related_issue_id` INTEGER NOT NULL, " +
                    "`type` TEXT NOT NULL, " +
                    "`created_at` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`issue_id`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(`related_issue_id`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                );
                LogUtils.getInstance().d(TAG, "MIGRATION_6_7: Temporary table created");
            } catch (Exception e) {
                LogUtils.getInstance().e(TAG, "MIGRATION_6_7: ❌ Failed to create temporary table", e);
                throw e;
            }
            
            // 步骤 2: 复制数据
            LogUtils.getInstance().d(TAG, "MIGRATION_6_7: Copying data from relations to relations_temp...");
            try {
                database.execSQL(
                    "INSERT INTO relations_temp (id, issue_id, related_issue_id, type, created_at) " +
                    "SELECT id, issue_id, related_issue_id, type, created_at FROM relations"
                );
                LogUtils.getInstance().d(TAG, "MIGRATION_6_7: Data copied successfully");
            } catch (Exception e) {
                LogUtils.getInstance().e(TAG, "MIGRATION_6_7: ❌ Failed to copy data", e);
                throw e;
            }
            
            // 步骤 3: 删除旧表
            LogUtils.getInstance().d(TAG, "MIGRATION_6_7: Dropping old relations table...");
            database.execSQL("DROP TABLE relations");
            LogUtils.getInstance().d(TAG, "MIGRATION_6_7: Old table dropped");
            
            // 步骤 4: 重命名
            LogUtils.getInstance().d(TAG, "MIGRATION_6_7: Renaming relations_temp to relations...");
            database.execSQL("ALTER TABLE relations_temp RENAME TO relations");
            LogUtils.getInstance().d(TAG, "MIGRATION_6_7: Table renamed");
            
            // 步骤 5: 创建索引
            LogUtils.getInstance().d(TAG, "MIGRATION_6_7: Creating indexes...");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_relations_issue_id ON relations(issue_id)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_relations_related_issue_id ON relations(related_issue_id)");
            LogUtils.getInstance().d(TAG, "MIGRATION_6_7: Indexes created");
            
            // 打印最终表结构
            LogUtils.getInstance().d(TAG, "MIGRATION_6_7: Final table schema:");
            printTableSchema(database, "relations");
            
            LogUtils.getInstance().i(TAG, "MIGRATION_6_7: ✅ Foreign key constraints added successfully");
        }
    };
    
    /**
     * 数据库迁移：版本 7 → 版本 8
     * 将 relations 表 type 字段从 NOT NULL 改为允许 NULL
     */
    static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            LogUtils.getInstance().d(TAG, "MIGRATION_7_8: START - Modifying type column to allow NULL");
            
            // 由于 SQLite 不支持直接修改列的 nullable 属性，需要重建表
            // 步骤 1: 创建临时表（type 字段允许 NULL）
            LogUtils.getInstance().d(TAG, "MIGRATION_7_8: Creating temporary table with type TEXT (nullable)...");
            try {
                database.execSQL(
                    "CREATE TABLE relations_temp (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`issue_id` INTEGER NOT NULL, " +
                    "`related_issue_id` INTEGER NOT NULL, " +
                    "`type` TEXT, " +
                    "`created_at` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`issue_id`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(`related_issue_id`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                );
                LogUtils.getInstance().d(TAG, "MIGRATION_7_8: Temporary table created");
            } catch (Exception e) {
                LogUtils.getInstance().e(TAG, "MIGRATION_7_8: ❌ Failed to create temporary table", e);
                throw e;
            }
            
            // 步骤 2: 复制数据
            LogUtils.getInstance().d(TAG, "MIGRATION_7_8: Copying data from relations to relations_temp...");
            try {
                database.execSQL(
                    "INSERT INTO relations_temp (id, issue_id, related_issue_id, type, created_at) " +
                    "SELECT id, issue_id, related_issue_id, type, created_at FROM relations"
                );
                LogUtils.getInstance().d(TAG, "MIGRATION_7_8: Data copied successfully");
            } catch (Exception e) {
                LogUtils.getInstance().e(TAG, "MIGRATION_7_8: ❌ Failed to copy data", e);
                throw e;
            }
            
            // 步骤 3: 删除旧表
            LogUtils.getInstance().d(TAG, "MIGRATION_7_8: Dropping old relations table...");
            database.execSQL("DROP TABLE relations");
            LogUtils.getInstance().d(TAG, "MIGRATION_7_8: Old table dropped");
            
            // 步骤 4: 重命名
            LogUtils.getInstance().d(TAG, "MIGRATION_7_8: Renaming relations_temp to relations...");
            database.execSQL("ALTER TABLE relations_temp RENAME TO relations");
            LogUtils.getInstance().d(TAG, "MIGRATION_7_8: Table renamed");
            
            // 步骤 5: 创建索引
            LogUtils.getInstance().d(TAG, "MIGRATION_7_8: Creating indexes...");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_relations_issue_id ON relations(issue_id)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_relations_related_issue_id ON relations(related_issue_id)");
            LogUtils.getInstance().d(TAG, "MIGRATION_7_8: Indexes created");
            
            // 打印最终表结构
            LogUtils.getInstance().d(TAG, "MIGRATION_7_8: Final table schema:");
            printTableSchema(database, "relations");
            
            LogUtils.getInstance().i(TAG, "MIGRATION_7_8: ✅ Type column modified to allow NULL successfully");
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
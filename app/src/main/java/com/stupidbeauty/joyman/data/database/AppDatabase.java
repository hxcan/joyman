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
 * 使用 Room 持久化库管理的 SQLite 数据库
 * 采用单例模式，确保全局唯一数据库实例
 * 
 * 数据库信息：
 * - 名称：joyman-db
 * - 版本：1（初始版本）
 * - 实体类：Task, Project
 * - DAO 接口：TaskDao, ProjectDao
 * 
 * 迁移策略：
 * - 支持增量迁移（addMigrations）
 * - 破坏性迁移回退（fallbackToDestructiveMigration）
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.0
 * @since 2026-03-31
 */
@Database(
    entities = {
        Task.class,
        Project.class
        // 未来添加：Tag.class, Comment.class, Attachment.class, etc.
    },
    version = 1,
    exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {
    
    /**
     * 数据库名称
     */
    private static final String DATABASE_NAME = "joyman-db";
    
    /**
     * 单例实例
     */
    private static volatile AppDatabase INSTANCE;
    
    /**
     * 获取 TaskDao
     * 
     * @return Task 数据访问对象
     */
    public abstract TaskDao taskDao();
    
    /**
     * 获取 ProjectDao
     * 
     * @return Project 数据访问对象
     */
    public abstract ProjectDao projectDao();
    
    /**
     * 获取数据库实例（单例模式，双重检查锁定）
     * 
     * @param context 应用上下文
     * @return 数据库实例
     */
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
    
    /**
     * 构建数据库实例
     * 
     * @param context 应用上下文
     * @return 数据库实例
     */
    @NonNull
    private static AppDatabase buildDatabase(Context context) {
        return Room.databaseBuilder(
                context,
                AppDatabase.class,
                DATABASE_NAME
            )
            // 添加迁移策略（从版本 1 到版本 2）
            .addMigrations(MIGRATION_1_2)
            // 如果没有找到迁移路径，则降级为破坏性迁移（删除所有数据重建）
            // 生产环境建议移除该行，强制要求提供迁移策略
            .fallbackToDestructiveMigration()
            // 允许在主线程查询（仅用于调试，生产环境应使用异步查询）
            // .allowMainThreadQueries()
            // 设置数据库回调
            .addCallback(new Callback() {
                @Override
                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                    super.onCreate(db);
                    // 数据库首次创建时的初始化逻辑
                    // 例如：预填充一些默认项目
                    android.util.Log.d("AppDatabase", "Database created successfully with Task and Project tables");
                }
                
                @Override
                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                    super.onOpen(db);
                    // 数据库打开时的逻辑
                    android.util.Log.d("AppDatabase", "Database opened");
                }
            })
            .build();
    }
    
    /**
     * 数据库迁移策略：版本 1 -> 版本 2
     * 
     * 示例迁移框架，实际使用时需要填写具体的 SQL 语句
     * 
     * 可能的迁移操作：
     * - 添加新表（如 tags, comments）
     * - 添加新字段（如 tasks.project_id）
     * - 修改现有表结构
     * - 数据转换和清理
     */
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 示例 1: 创建新表
            // database.execSQL("CREATE TABLE IF NOT EXISTS tags (" +
            //     "id INTEGER PRIMARY KEY NOT NULL, " +
            //     "name TEXT NOT NULL, " +
            //     "color TEXT, " +
            //     "created_at INTEGER NOT NULL" +
            // ")");
            
            // 示例 2: 添加新字段到现有表
            // database.execSQL("ALTER TABLE tasks ADD COLUMN project_id INTEGER DEFAULT NULL");
            // database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_project_id ON tasks(project_id)");
            
            // 示例 3: 数据迁移
            // database.execSQL("UPDATE tasks SET status = 0 WHERE status IS NULL");
            
            android.util.Log.d("AppDatabase", "Migrated from version 1 to version 2");
        }
    };
    
    /**
     * 关闭数据库
     * 
     * 通常在应用退出时调用
     */
    public static void closeInstance() {
        if (INSTANCE != null && INSTANCE.isOpen()) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}
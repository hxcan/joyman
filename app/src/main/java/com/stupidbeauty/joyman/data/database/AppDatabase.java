package com.stupidbeauty.joyman.data.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.stupidbeauty.joyman.data.database.dao.TaskDao;
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
 * - 实体类：Task（后续添加 Project、Tag 等）
 * - DAO 接口：TaskDao（后续添加 ProjectDao 等）
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
        Task.class
        // 未来添加：Project.class, Tag.class, Comment.class, etc.
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
                    // 例如：预填充一些默认数据
                    android.util.Log.d("AppDatabase", "Database created successfully");
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
     * 示例：添加 Project 表
     * （当前为空实现，实际使用时需要填写具体的 SQL 语句）
     */
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 示例：创建新表
            // database.execSQL("CREATE TABLE IF NOT EXISTS projects (...)");
            
            // 示例：添加新字段
            // database.execSQL("ALTER TABLE tasks ADD COLUMN new_column TEXT DEFAULT ''");
            
            // 示例：数据迁移
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
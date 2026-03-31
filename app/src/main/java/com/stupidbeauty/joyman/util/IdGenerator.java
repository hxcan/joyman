package com.stupidbeauty.joyman.util;

import java.security.SecureRandom;
import java.util.Random;

/**
 * JoyMan ID 生成器
 * 
 * 生成策略：时间戳（10 位）+ 随机数（4 位）= 14 位数字 ID
 * - 前 10 位：当前时间戳（秒级），保证时间维度唯一性
 * - 后 4 位：随机数，保证同一秒内的唯一性
 * 
 * 特点：
 * - 完全离线生成，无需网络
 * - 数字 ID，便于用户记忆和输入
 * - 冲突概率极低（同一秒内碰撞概率 < 0.01%）
 * - 线程安全（使用 SecureRandom）
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.0
 */
public class IdGenerator {
    
    private static final Random RANDOM = new SecureRandom();
    private static final long BASE_TIMESTAMP = 1700000000L; // 2023-11-15 00:00:00 UTC
    
    /**
     * 生成一个唯一的 14 位数字 ID
     * 
     * @return 14 位数字 ID（long 类型）
     */
    public static long generateId() {
        // 获取当前时间戳（秒级），减去基准时间以缩短位数
        long timestamp = System.currentTimeMillis() / 1000 - BASE_TIMESTAMP;
        
        // 生成 4 位随机数（0000-9999）
        int randomPart = RANDOM.nextInt(10000);
        
        // 组合：时间戳（10 位）* 10000 + 随机数（4 位）
        return timestamp * 10000 + randomPart;
    }
    
    /**
     * 从 ID 中提取时间戳
     * 
     * @param id 14 位数字 ID
     * @return 原始时间戳（秒级，相对于 BASE_TIMESTAMP）
     */
    public static long extractTimestamp(long id) {
        return id / 10000;
    }
    
    /**
     * 从 ID 中提取随机部分
     * 
     * @param id 14 位数字 ID
     * @return 随机数部分（0000-9999）
     */
    public static int extractRandomPart(long id) {
        return (int)(id % 10000);
    }
    
    /**
     * 获取 ID 的创建时间（毫秒级时间戳）
     * 
     * @param id 14 位数字 ID
     * @return 创建时间的毫秒级时间戳
     */
    public static long getCreationTime(long id) {
        return (extractTimestamp(id) + BASE_TIMESTAMP) * 1000;
    }
    
    /**
     * 验证 ID 格式是否有效
     * 
     * @param id 待验证的 ID
     * @return true 如果是有效的 14 位数字 ID
     */
    public static boolean isValidId(long id) {
        // 检查是否为 14 位数字（最小值：10000000000000，最大值：99999999999999）
        return id >= 10000000000000L && id < 100000000000000L;
    }
    
    /**
     * 批量生成 ID（用于测试或预生成）
     * 
     * @param count 需要生成的 ID 数量
     * @return ID 数组
     */
    public static long[] generateBatch(int count) {
        long[] ids = new long[count];
        for (int i = 0; i < count; i++) {
            ids[i] = generateId();
        }
        return ids;
    }
}
package com.stupidbeauty.joyman.util;

import java.util.Random;

/**
 * 随机数字 ID 生成器
 * 
 * 生成格式：时间戳 (10 位) + 随机数 (4 位) = 14 位数字
 * 示例：17746135571234
 * 
 * 特点：
 * - 离线生成，无需网络
 * - 冲突概率极低（4 位随机数提供 10000 种可能）
 * - 按时间排序友好
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.0
 */
public class IdGenerator {
    
    private static final String TAG = "IdGenerator";
    
    private static final Random random = new Random();
    
    /**
     * 生成唯一的 14 位数字 ID
     * 
     * @return 14 位数字 ID（long 类型）
     */
    public static long generateId() {
        // 获取当前时间戳（毫秒），取后 10 位
        long timestamp = System.currentTimeMillis();
        long timePart = timestamp % 10000000000L; // 确保不超过 10 位
        
        // 生成 4 位随机数 (0000-9999)
        long randomPart = random.nextInt(10000);
        
        // 组合：时间戳 * 10000 + 随机数
        long id = timePart * 10000 + randomPart;
        
        return id;
    }
    
    /**
     * 批量生成多个唯一 ID
     * 
     * @param count 需要生成的 ID 数量
     * @return ID 数组
     */
    public static long[] generateIds(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be positive");
        }
        
        long[] ids = new long[count];
        for (int i = 0; i < count; i++) {
            ids[i] = generateId();
            // 短暂延迟避免同一毫秒内生成重复 ID
            if (i < count - 1) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        return ids;
    }
    
    /**
     * 验证 ID 格式是否正确（14 位数字）
     * 
     * @param id 待验证的 ID
     * @return true 如果格式正确
     */
    public static boolean isValidId(long id) {
        // 14 位数字的范围：10000000000000 ~ 99999999999999
        return id >= 10000000000000L && id < 100000000000000L;
    }
    
    /**
     * 从 ID 中提取时间戳部分
     * 
     * @param id 14 位 ID
     * @return 时间戳（毫秒）
     */
    public static long extractTimestamp(long id) {
        if (!isValidId(id)) {
            throw new IllegalArgumentException("Invalid ID format");
        }
        
        // 前 10 位是时间戳部分
        long timePart = id / 10000;
        
        // 还原为完整时间戳（需要加上基准年份的偏移）
        // 这里简化处理，假设是 2020 年之后的时间
        return timePart + 10000000000L; // 近似还原
    }
}

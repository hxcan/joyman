package com.stupidbeauty.joyman.util;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 日志工具类
 * 支持同时输出到 Logcat 和文件
 * 
 * @author 太极美术工程狮狮长
 * @version 1.0.0
 * @since 2026-04-01
 */
public class LogUtils {
    
    private static final String TAG = "LogUtils";
    private static final String LOG_DIR_NAME = "joyman_logs";
    private static final String LOG_FILE_PREFIX = "joyman_";
    private static final String LOG_FILE_SUFFIX = ".log";
    
    private static LogUtils instance;
    private ExecutorService executorService;
    private String currentLogFile;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat timeFormat;
    
    /**
     * 获取单例实例
     */
    public static synchronized LogUtils getInstance() {
        if (instance == null) {
            instance = new LogUtils();
        }
        return instance;
    }
    
    private LogUtils() {
        executorService = Executors.newSingleThreadExecutor();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        timeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
        updateCurrentLogFile();
    }
    
    /**
     * 更新当前日志文件路径（按日期分割）
     */
    private void updateCurrentLogFile() {
        String dateStr = dateFormat.format(new Date());
        File logDir = getLogDirectory();
        currentLogFile = new File(logDir, LOG_FILE_PREFIX + dateStr + LOG_FILE_SUFFIX).getAbsolutePath();
    }
    
    /**
     * 获取日志目录
     */
    private File getLogDirectory() {
        File externalDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File logDir = new File(externalDir, LOG_DIR_NAME);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        return logDir;
    }
    
    /**
     * 写入日志到文件
     */
    private void writeToFile(final String level, final String tag, final String message, final Throwable throwable) {
        executorService.execute(() -> {
            // 检查是否需要切换日志文件（跨天情况）
            updateCurrentLogFile();
            
            String timestamp = timeFormat.format(new Date());
            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append(timestamp)
                     .append(" ")
                     .append(level)
                     .append("/")
                     .append(tag)
                     .append(": ")
                     .append(message);
            
            if (throwable != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                throwable.printStackTrace(pw);
                logBuilder.append("\n").append(sw.toString());
                pw.close();
            }
            
            synchronized (this) {
                try (FileWriter writer = new FileWriter(currentLogFile, true)) {
                    writer.write(logBuilder.toString());
                    writer.write("\n");
                    writer.flush();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to write log to file", e);
                }
            }
        });
    }
    
    /**
     * Verbose 级别日志
     */
    public void v(String tag, String message) {
        Log.v(tag, message);
        writeToFile("V", tag, message, null);
    }
    
    /**
     * Debug 级别日志
     */
    public void d(String tag, String message) {
        Log.d(tag, message);
        writeToFile("D", tag, message, null);
    }
    
    /**
     * Info 级别日志
     */
    public void i(String tag, String message) {
        Log.i(tag, message);
        writeToFile("I", tag, message, null);
    }
    
    /**
     * Warning 级别日志
     */
    public void w(String tag, String message) {
        Log.w(tag, message);
        writeToFile("W", tag, message, null);
    }
    
    /**
     * Error 级别日志
     */
    public void e(String tag, String message) {
        Log.e(tag, message);
        writeToFile("E", tag, message, null);
    }
    
    /**
     * Error 级别日志（带异常）
     */
    public void e(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);
        writeToFile("E", tag, message, throwable);
    }
    
    /**
     * 获取当前日志文件路径
     */
    public String getCurrentLogFile() {
        return currentLogFile;
    }
    
    /**
     * 获取日志目录路径
     */
    public String getLogDirectoryPath() {
        return getLogDirectory().getAbsolutePath();
    }
}
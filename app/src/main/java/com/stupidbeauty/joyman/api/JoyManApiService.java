    /**
     * 从文件读取内容（Android 兼容方式，支持 API 24+）
     */
    private String readFileContent(String filePath) {
        try {
            logUtils.d(TAG, "readFileContent: Reading file: " + filePath);
            
            // Android 环境下使用 File 类读取
            File file = new File(filePath);
            if (!file.exists()) {
                logUtils.e(TAG, "readFileContent: File does not exist: " + filePath);
                return null;
            }

            // 使用 FileInputStream + ByteArrayOutputStream 替代 Files.readAllBytes (API 26+)
            // 以兼容 minSdkVersion 24
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(file);
                byte[] data = new byte[8192];
                int nRead;
                while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
                
                String content = buffer.toString(StandardCharsets.UTF_8.name());
                logUtils.d(TAG, "readFileContent: File size: " + content.length() + " chars");
                logUtils.d(TAG, "readFileContent: Content preview: " + (content.length() > 200 ? content.substring(0, 200) + "..." : content));
                
                return content;
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        logUtils.w(TAG, "readFileContent: Error closing stream", e);
                    }
                }
            }
        } catch (IOException e) {
            logUtils.e(TAG, "readFileContent: Error reading file: " + filePath, e);
            return null;
        }
    }
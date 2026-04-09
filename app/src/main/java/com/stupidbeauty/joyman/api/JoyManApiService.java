            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        logUtils.w(TAG, "readFileContent: Error closing stream: " + e.getMessage());
                    }
                }
            }
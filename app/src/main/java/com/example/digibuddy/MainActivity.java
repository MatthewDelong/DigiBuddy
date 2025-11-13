package com.example.digibuddy;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class CrashLogger {
    private static final String TAG = "CrashLogger";
    private static final String LOG_FILE_NAME = "digibuddy_crash_log.txt";
    private Context context;

    public CrashLogger(Context context) {
        this.context = context;
    }

    public void log(String message) {
        Log.d(TAG, message);
        writeToFile(message);
    }

    public void logError(String message, Exception e) {
        String errorMessage = message + " - " + (e != null ? e.getMessage() : "No exception details");
        Log.e(TAG, errorMessage);
        writeToFile("ERROR: " + errorMessage);

        if (e != null) {
            for (StackTraceElement element : e.getStackTrace()) {
                writeToFile("    at " + element.toString());
            }
        }
    }

    private void writeToFile(String message) {
        FileOutputStream outputStream = null;
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
            String logMessage = timestamp + " - " + message + "\n";

            File file = new File(context.getExternalFilesDir(null), LOG_FILE_NAME);
            outputStream = new FileOutputStream(file, true);
            outputStream.write(logMessage.getBytes());

        } catch (IOException e) {
            Log.e(TAG, "Failed to write to log file: " + e.getMessage());
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to close log file: " + e.getMessage());
                }
            }
        }
    }

    public String getLogFilePath() {
        File externalFile = new File(context.getExternalFilesDir(null), LOG_FILE_NAME);
        return externalFile.getAbsolutePath();
    }

    public void clearLog() {
        try {
            File externalFile = new File(context.getExternalFilesDir(null), LOG_FILE_NAME);
            if (externalFile.exists()) {
                externalFile.delete();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear log: " + e.getMessage());
        }
    }
}
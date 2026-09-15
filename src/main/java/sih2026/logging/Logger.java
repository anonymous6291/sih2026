package sih2026.logging;

/*
 * Copyright (c) 2026 Anish Kumar Shaw
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for license terms.
 *
 * Original project:
 * https://github.com/anonymous6291/sih2026
 */

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class Logger {
    private static final String LOG_FILE_PATH = "log/";
    private static final String LOG_FILE_NAME = "log";
    private static final String LOG_FILE_EXTENSION = ".txt";
    private static final int MAX_LOG_SIZE = 1_00_000;
    private final Semaphore lock = new Semaphore(1, true);
    private final StringBuffer logs;
    private final AtomicLong nextFileNumber;
    private final AtomicBoolean isClosed;

    Logger() throws Exception {
        logs = new StringBuffer();
        Files.createDirectories(Path.of(LOG_FILE_PATH));
        long fileNumber = 0;
        while (Files.exists(getLogFilePath(fileNumber))) {
            fileNumber++;
        }
        nextFileNumber = new AtomicLong(fileNumber);
        isClosed = new AtomicBoolean(false);
    }

    private Path getLogFilePath(long number) {
        return Path.of(LOG_FILE_PATH, LOG_FILE_NAME + number + LOG_FILE_EXTENSION);
    }

    private boolean setLock() {
        try {
            lock.acquire();
            return true;
        } catch (Exception e) {
            IO.println(e);
        }
        return false;
    }

    private void unlock() {
        lock.release();
    }

    public void logInfo(String data) {
        log(data, LogType.INFO);
    }

    public void logWarn(String data) {
        log(data, LogType.WARN);
    }

    public void logError(String data) {
        log(data, LogType.ERROR);
    }

    public void logSevere(String data) {
        log(data, LogType.SEVERE);
    }

    public void log(String data, LogType logType) {
        if (isClosed()) {
            throw new IllegalStateException("Attempt to write log when Logger is closed.");
        }
        String formattedLogData = formatLogData(data, logType);
        if (!setLock()) {
            IO.println(formattedLogData);
            return;
        }
        IO.println(formattedLogData);
        try {
            if (logs.length() + formattedLogData.length() > MAX_LOG_SIZE) {
                flushLogs();
                logs.delete(0, logs.length());
            }
            logs.append(formattedLogData);
        } catch (Exception e) {
            IO.println(e);
        } finally {
            unlock();
        }
    }

    private String formatLogData(String data, LogType logType) {
        return "[%s] [%s] : %s".formatted(logType.toString(), LocalDateTime.now().toString(), data);
    }

    private void flushLogs() throws Exception {
        Path logFilePath = getLogFilePath(nextFileNumber.getAndIncrement());
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(logFilePath)) {
            bufferedWriter.write(logs.toString());
        }
    }

    public boolean isClosed() {
        return isClosed.get();
    }

    @PreDestroy
    public void close() {
        try {
            if (isClosed()) {
                return;
            }
            if (!setLock()) {
            }
            try {
                flushLogs();
                isClosed.set(true);
            } finally {
                unlock();
            }
        } catch (Exception e) {
            IO.println("Error occurred while writing log files. " + e);
        }
    }

    public enum LogType {
        INFO, WARN, ERROR, SEVERE
    }
}

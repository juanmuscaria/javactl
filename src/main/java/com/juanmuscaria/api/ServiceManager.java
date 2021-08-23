package com.juanmuscaria.api;

import lombok.SneakyThrows;

import java.io.IOException;

public enum ServiceManager {
    UNKNOWN,
    SYSTEMD;
    // Cache the result for next calls.
    private static ServiceManager found = null;

    @SneakyThrows(InterruptedException.class) // don't catch it, pass along to kill the entire thread.
    public static ServiceManager getSystemServiceManager() {
        if (found != null)
            return found;

        Process process;
        try {
            process = Runtime.getRuntime().exec("systemctl");
            process.waitFor();
            found = SYSTEMD;
            return SYSTEMD;
        } catch (IOException ignored) {
            // command missing, run next check
        }
        found = UNKNOWN;
        return UNKNOWN;
    }
}

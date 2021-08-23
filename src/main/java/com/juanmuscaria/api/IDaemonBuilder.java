package com.juanmuscaria.api;

import com.juanmuscaria.api.jvm.JavaInstallation;
import com.juanmuscaria.impl.SystemdDaemonBuilder;

import java.io.File;

public interface IDaemonBuilder {
    static IDaemonBuilder builder() {
        switch (ServiceManager.getSystemServiceManager()) {
            case SYSTEMD:
                return new SystemdDaemonBuilder();
            case UNKNOWN:
            default:
                throw new IllegalStateException("System not supported");
        }
    }

    IDaemonBuilder name(String name);

    IDaemonBuilder description(String description);

    IDaemonBuilder java(JavaInstallation java);

    IDaemonBuilder user(String user);

    IDaemonBuilder group(String group);

    IDaemonBuilder workingDir(File workingDir);

    IDaemonBuilder autoRestart(boolean restart);

    IDaemonBuilder jvmArgs(String args);

    IDaemonBuilder args(String args);

    IDaemonBuilder jarFile(File jar);

    File[] build();
}

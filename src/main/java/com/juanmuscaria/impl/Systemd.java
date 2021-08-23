package com.juanmuscaria.impl;

import com.juanmuscaria.api.IService;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class Systemd implements IService {

    @Override
    public Optional<File> getServiceFileFor(String name) {
        if (!isValidName(name)) {
            return Optional.empty();
        }
        var file = new File("/etc/systemd/system/javactl-" + name + ".service");
        if (file.exists()) {
            return Optional.of(file);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<File> getSocketConfigurationFor(String name) {
        if (!isValidName(name)) {
            return Optional.empty();
        }
        var file = new File("/etc/systemd/system/javactl-" + name + ".socket");
        if (file.exists()) {
            return Optional.of(file);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<File> getSocketFileFor(String name) {
        if (!isValidName(name)) {
            return Optional.empty();
        }
        var file = new File("/run/javactl/" + name);
        if (file.exists()) {
            return Optional.of(file);
        } else {
            return Optional.empty();
        }
    }

    @Override
    @SneakyThrows({InterruptedException.class}) // pass it up the chain
    public boolean isServiceRunning(String name) {
        try {
            Process check = Runtime.getRuntime().exec("systemctl is-active --quiet javactl-" + name);
            check.waitFor();
            return check.exitValue() == 0;
        } catch (IOException e) {
            throw new IllegalStateException("systemctl not found, this should be an unreachable exception, please open an issue if you see this message!");
        }
    }
}

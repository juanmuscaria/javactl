package com.juanmuscaria.impl;

import com.juanmuscaria.api.IService;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

    @Override
    public List<String> getInstalledServices() {
        List<String> list = new ArrayList<>();
        File systemdServices = new File("/etc/systemd/system/");
        File[] services = systemdServices.listFiles();
        if (services == null)
            throw new IllegalArgumentException("Unable to list services.");
        for (File service : services) {
            if (service.getName().startsWith("javactl-") && service.getName().endsWith(".service"))
                list.add(removeJavactlIdent(service.getName()));
        }
        return list;
    }

    @Override
    @SneakyThrows({InterruptedException.class}) // pass it up the chain
    public void startService(String name) {
        try {
            Process check = Runtime.getRuntime().exec("systemctl start --quiet javactl-" + name);
            check.waitFor();
            if (check.exitValue() != 0)
                throw new IllegalArgumentException("Systemd returned an error! Are you running with proper permissions?");
        } catch (IOException e) {
            throw new IllegalStateException("systemctl not found, this should be an unreachable exception, please open an issue if you see this message!");
        }
    }

    @Override
    @SneakyThrows({InterruptedException.class}) // pass it up the chain
    public void stopService(String name) {
        try {
            Process check = Runtime.getRuntime().exec("systemctl stop --quiet javactl-" + name);
            check.waitFor();
            if (check.exitValue() != 0)
                throw new IllegalArgumentException("Systemd returned an error! Are you running with proper permissions?");
        } catch (IOException e) {
            throw new IllegalStateException("systemctl not found, this should be an unreachable exception, please open an issue if you see this message!");
        }
    }

    @Override
    @SneakyThrows({InterruptedException.class}) // pass it up the chain
    public void restartService(String name) {
        try {
            Process check = Runtime.getRuntime().exec("systemctl restart --quiet javactl-" + name);
            check.waitFor();
            if (check.exitValue() != 0)
                throw new IllegalArgumentException("Systemd returned an error! Are you running with proper permissions?");
        } catch (IOException e) {
            throw new IllegalStateException("systemctl not found, this should be an unreachable exception, please open an issue if you see this message!");
        }
    }

    @Override
    public void deleteService(String name) {
        if (isServiceRunning(name))
            stopService(name);
        disableService(name);
        if (!getServiceFileFor(name).orElseThrow().delete())
            throw new IllegalArgumentException("Unable to delete service file! Try running as root.");
        Optional<File> file = getServiceFileFor(name);
        if (file.isPresent()) {
            if (!file.get().delete())
                throw new IllegalArgumentException("Unable to delete socket file " + file.get().getAbsolutePath());
        }
    }

    @Override
    @SneakyThrows({InterruptedException.class}) // pass it up the chain
    public void enableService(String name) {
        try {
            Process check = Runtime.getRuntime().exec("systemctl enable --quiet javactl-" + name);
            check.waitFor();
            if (check.exitValue() != 0)
                throw new IllegalArgumentException("Systemd returned an error! Are you running with proper permissions?");
        } catch (IOException e) {
            throw new IllegalStateException("systemctl not found, this should be an unreachable exception, please open an issue if you see this message!");
        }
    }

    @Override
    @SneakyThrows({InterruptedException.class}) // pass it up the chain
    public void disableService(String name) {
        try {
            Process check = Runtime.getRuntime().exec("systemctl disable --quiet javactl-" + name);
            check.waitFor();
            if (check.exitValue() != 0)
                throw new IllegalArgumentException("Systemd returned an error! Are you running with proper permissions?");
        } catch (IOException e) {
            throw new IllegalStateException("systemctl not found, this should be an unreachable exception, please open an issue if you see this message!");
        }
    }

    private String removeJavactlIdent(String name) {
        return name.substring(8,name.indexOf(".service"));
    }
}

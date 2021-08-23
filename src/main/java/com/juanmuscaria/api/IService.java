package com.juanmuscaria.api;

import com.juanmuscaria.impl.Systemd;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.util.Optional;
import java.util.regex.Pattern;

public interface IService {
    @NotNull
    IService SERVICE = getService();
    Pattern validNamePattern = Pattern.compile("^[a-zA-Z0-9\\s]+$");

    private static IService getService() {
        switch (ServiceManager.getSystemServiceManager()) {
            case SYSTEMD:
                return new Systemd();
            case UNKNOWN:
            default:
                throw new IllegalStateException("System not supported");
        }
    }

    Optional<File> getServiceFileFor(String name);

    Optional<File> getSocketConfigurationFor(String name);

    Optional<File> getSocketFileFor(String name);

    boolean isServiceRunning(String name);

    default boolean isValidName(String name) {
        return validNamePattern.matcher(name).matches();
    }

    default String getValidationRules() {
        return "^[a-zA-Z0-9\\s]+$";
    }
}

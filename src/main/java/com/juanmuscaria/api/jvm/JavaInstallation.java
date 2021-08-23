package com.juanmuscaria.api.jvm;

import lombok.Getter;
import lombok.SneakyThrows;

import java.io.*;
import java.nio.file.Files;

public class JavaInstallation {
    private static final File probe;

    // Creates the probe to detect more information about the java installation
    static {
        InputStream probeClass = JavaInstallation.class.getResourceAsStream("/probe/JavaProbe.class");
        if (probeClass == null)
            throw new IllegalStateException("Unable to extract java probe");

        File tmp;
        try {
            tmp = new File(Files.createTempDirectory("javaclt").toFile(), "JavaProbe.class");
            tmp.deleteOnExit();
            OutputStream out = new FileOutputStream(tmp);
            byte[] buffer = new byte[1024];
            int len = probeClass.read(buffer);
            while (len != -1) {
                out.write(buffer, 0, len);
                len = probeClass.read(buffer);
            }
            probeClass.close();
            out.close();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to extract java probe", e);
        }
        probe = tmp;

    }

    @Getter
    private final File exec;
    @Getter
    private final String home;
    @Getter
    private final String version;
    @Getter
    private final String vendor;
    @Getter
    private final String arch;
    @Getter
    private final String vm;
    @Getter
    private final String vmVersion;
    @Getter
    private final String runtime;
    @Getter
    private final String runtimeVersion;

    // TODO: Is it the best way...?
    @SneakyThrows(InterruptedException.class)
    public JavaInstallation(File exec) {
        this.exec = exec;
        try {
            var builder = new ProcessBuilder();
            builder.directory(probe.getParentFile());
            builder.command(exec.getAbsolutePath(), "-cp", ".", "JavaProbe");
            var process = builder.start();
            process.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            home = reader.readLine();
            version = reader.readLine();
            vendor = reader.readLine();
            arch = reader.readLine();
            vm = reader.readLine();
            vmVersion = reader.readLine();
            runtime = reader.readLine();
            runtimeVersion = reader.readLine();
            reader.close();
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to probe java installation, are you using the right path?", e);
        }
    }

    // To test if we can probe in this system.
    public static void init() {
    }

    @Override
    public String toString() {
        return "JavaInstallation{" +
                "file=" + exec +
                ", home='" + home + '\'' +
                ", version='" + version + '\'' +
                ", vendor='" + vendor + '\'' +
                ", arch='" + arch + '\'' +
                ", vm='" + vm + '\'' +
                ", vmVersion='" + vmVersion + '\'' +
                ", runtime='" + runtime + '\'' +
                ", runtimeVersion='" + runtimeVersion + '\'' +
                '}';
    }

}

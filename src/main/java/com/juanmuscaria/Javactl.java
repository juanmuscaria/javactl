package com.juanmuscaria;

import com.juanmuscaria.api.IDaemonBuilder;
import com.juanmuscaria.api.IService;
import com.juanmuscaria.api.ServiceManager;
import com.juanmuscaria.api.jvm.JavaFinder;
import com.juanmuscaria.api.jvm.JavaInstallation;
import com.juanmuscaria.api.jvm.VirtualTerminal;
import io.micronaut.configuration.picocli.PicocliRunner;
import picocli.AutoComplete;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.List;

import static com.juanmuscaria.Javactl.checkEnvironment;
import static com.juanmuscaria.Javactl.warnPrivilegedAction;
import static picocli.CommandLine.Help.Ansi.AUTO;

@Command(name = "javactl", version = "0.1", description = "Command line utility to make and control java daemons.",
        mixinStandardHelpOptions = true,
        subcommands = { AutoComplete.GenerateCompletion.class, CommandCreate.class, CommandConnect.class,
                CommandList.class, CommandStart.class, CommandStop.class, CommandRestart.class, CommandDelete.class })
public class Javactl implements Runnable {

    public static void main(String[] args) throws Exception {
        PicocliRunner.run(Javactl.class, args);
    }

    public static void checkEnvironment() {
        if (ServiceManager.getSystemServiceManager() == ServiceManager.UNKNOWN) {
            System.out.println("Only systems using systemd are supported!");
            System.exit(1);
        }
        try {
            JavaInstallation.init();
        } catch (Throwable e) {
            System.out.println("Unable to configure JVM probing!");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void warnPrivilegedAction() {
        if (!System.getProperty("user.name").equalsIgnoreCase("root"))
            System.out.println(AUTO.string("@|yellow This action may require root privileges, it's recommended to run it as root!|@"));
    }

    public void run() {
        checkEnvironment();
        System.out.println(AUTO.string("@|green javactl is ready to use! Type javactl --help for available commands.|@"));
    }

    @Command(name = "probeJava", description = "List all available java installations.")
    public void commandProbeJavaInstalls() {
        checkEnvironment();
        JavaFinder.findJavaInstallations().forEach(System.out::println);
    }
}

@Command(name = "create", description = "Creates a new daemon configuration.", mixinStandardHelpOptions = true)
class CommandCreate implements Runnable {
    @Option(names = {"--name", "-n"}, description = "The daemon name, it must be unique.", required = true)
    String name;
    @Option(names = {"--description", "-d"}, description = "The daemon description.")
    String desc;
    @Option(names = {"--jar", "-j"}, description = "The jar file to be daemonized.", required = true)
    File jar;
    @Option(names = {"--javaExec", "-E"}, description = "The full path to a java executable.", required = true)
    File javaPath;
    @Option(names = {"--workDir", "-D"}, description = "The working dir the daemon will use, if omitted the jar's parent dir will be used instead.")
    File workingDir;
    @Option(names = {"--user", "-u"}, description = "Set the UNIX user that the daemon will run as, if omitted it will default to systemd's default user.")
    String user;
    @Option(names = {"--group", "-g"}, description = "Set the UNIX group that the daemon will run as.")
    String group;
    @Option(names = {"--autoRestart", "-r"}, description = "The daemon will auto restart when crashed.")
    boolean autoRestart;
    @Option(names = {"--jvmArgs", "-v"}, description = "Arguments to be passed to the jvm of the daemon.")
    String jvmArgs;
    @Option(names = {"--args", "-a"}, description = "Arguments to be passed to the jvm of the daemon.")
    String args;

    @Override
    public void run() {
        checkEnvironment();
        Javactl.warnPrivilegedAction();
        try {
            IDaemonBuilder builder = IDaemonBuilder.builder();
            var javaExec = new JavaInstallation(javaPath);
            if (!IService.SERVICE.isValidName(name))
                throw new IllegalArgumentException("Invalid name! It must follow the following rules:" + IService.SERVICE.getValidationRules());
            System.out.printf("Using java %s-%s\n", javaExec.getVendor(), javaExec.getVersion());
            builder.name(name)
                    .description(desc)
                    .java(javaExec)
                    .workingDir(workingDir)
                    .jarFile(jar)
                    .args(args)
                    .jvmArgs(jvmArgs)
                    .user(user)
                    .group(group)
                    .autoRestart(autoRestart);
            File[] files = builder.build();
            System.out.println("Service created as: " + files[0].getName());
            System.out.println("Enable it using javactl enable " + name);
            System.exit(0);
        } catch (IllegalArgumentException e) {
            System.err.println(AUTO.string("@|red " + e.getMessage() + "|@"));
            System.exit(2);
        }
    }
}

@Command(name = "connect", description = "Connects to a running java service allowing you to gather information about the jvm and control over the input stream.", mixinStandardHelpOptions = true)
class CommandConnect implements Runnable {
    @Option(names = {"--useCat", "-c"}, description = "Journalctl will be configured to use cat as it's output.")
    boolean useCat;
    @Parameters(paramLabel = "name", description = "The daemon name.")
    String name;
    @Override
    public void run() {
        checkEnvironment();
        try {
            if (!IService.SERVICE.isServiceRunning(name))
                throw new IllegalArgumentException("Service is not running!");
            if (IService.SERVICE.getSocketFileFor(name).isEmpty())
                throw new IllegalArgumentException("Unable to find service socket!");
            new VirtualTerminal(name, useCat).start();
            System.exit(0);
        } catch (IllegalArgumentException e) {
            System.err.println(AUTO.string("@|red " + e.getMessage() + "|@"));
            System.exit(2);
        }
    }
}

@Command(name = "list", description = "List all installed daemons.", mixinStandardHelpOptions = true)
class CommandList implements Runnable {

    @Override
    public void run() {
        checkEnvironment();
        try {
            List<String> services = IService.SERVICE.getInstalledServices();
            if (services.isEmpty()) {
                System.out.println(AUTO.string("@|red No services installed!|@"));
            }
            for (String service : services) {
                boolean isRunning = IService.SERVICE.isServiceRunning(service);
                System.out.println(service + " - " + (isRunning?"Running":"Stopped"));
            }
        } catch (IllegalArgumentException e) {
            System.err.println(AUTO.string("@|red " + e.getMessage() + "|@"));
            System.exit(2);
        }
    }
}

@Command(name = "start", description = "Start a daemon.", mixinStandardHelpOptions = true)
class CommandStart implements Runnable {
    @Parameters(paramLabel = "name", description = "The daemon name.")
    String name;

    @Override
    public void run() {
        warnPrivilegedAction();
        try {
            if (IService.SERVICE.getServiceFileFor(name).isEmpty())
                throw new IllegalArgumentException("Service '" + name + "' does not exist.");
            IService.SERVICE.startService(name);
        } catch (IllegalArgumentException e) {
            System.err.println(AUTO.string("@|red " + e.getMessage() + "|@"));
            System.exit(2);
        }
    }
}

@Command(name = "stop", description = "Stop a daemon.", mixinStandardHelpOptions = true)
class CommandStop implements Runnable {
    @Parameters(paramLabel = "name", description = "The daemon name.")
    String name;

    @Override
    public void run() {
        warnPrivilegedAction();
        try {
            if (IService.SERVICE.getServiceFileFor(name).isEmpty())
                throw new IllegalArgumentException("Service '" + name + "' does not exist.");
            IService.SERVICE.stopService(name);
        } catch (IllegalArgumentException e) {
            System.err.println(AUTO.string("@|red " + e.getMessage() + "|@"));
            System.exit(2);
        }
    }
}

@Command(name = "restart", description = "(Re)start a daemon.", mixinStandardHelpOptions = true)
class CommandRestart implements Runnable{
    @Parameters(paramLabel = "name", description = "The daemon name.")
    String name;

    @Override
    public void run() {
        warnPrivilegedAction();
        try {
            if (IService.SERVICE.getServiceFileFor(name).isEmpty())
                throw new IllegalArgumentException("Service '" + name + "' does not exist.");
            IService.SERVICE.restartService(name);
        } catch (IllegalArgumentException e) {
            System.err.println(AUTO.string("@|red " + e.getMessage() + "|@"));
            System.exit(2);
        }
    }
}

@Command(name = "delete", aliases = "uninstall", description = "Stop and delete a daemon configuration.", mixinStandardHelpOptions = true)
class CommandDelete implements Runnable {
    @Parameters(paramLabel = "name", description = "The daemon name.")
    String name;

    @Override
    public void run() {
        warnPrivilegedAction();
        try {
            if (IService.SERVICE.getServiceFileFor(name).isEmpty())
                throw new IllegalArgumentException("Service '" + name + "' does not exist.");
            IService.SERVICE.deleteService(name);
        } catch (IllegalArgumentException e) {
            System.err.println(AUTO.string("@|red " + e.getMessage() + "|@"));
            System.exit(2);
        }
    }
}
package com.juanmuscaria.api.jvm;

import com.juanmuscaria.api.IService;
import lombok.SneakyThrows;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class VirtualTerminal {
    final String name;
    private boolean useCat;
    private final AtomicBoolean stop = new AtomicBoolean(false);
    File socket;

    public VirtualTerminal(String name, boolean useCat) {
        socket = IService.SERVICE.getSocketFileFor(name).orElseThrow();
        this.name = name;
        this.useCat = useCat;
    }

    @SneakyThrows
    public void start() {
        Terminal terminal = TerminalBuilder.builder().build();
        if (terminal.getWidth() == 0 || terminal.getHeight() == 0) {
            terminal.setSize(new Size(120, 40));
        }
        File userHome = new File(System.getProperty("user.home", ""));
        LineReaderBuilder builder = LineReaderBuilder.builder();
        builder.terminal(terminal)
                .variable(LineReader.INDENTATION, 2)
                .variable(LineReader.LIST_MAX, 100);
        if (userHome.exists())
            builder.variable(LineReader.HISTORY_FILE, Paths.get(userHome.getAbsolutePath(), ".javactl", name + ".history"));
        LineReader reader = builder
                .build();
        FileOutputStream socketIo = new FileOutputStream(socket);
        Thread journalReader = new Thread(() -> {
            Process journal = null;
            try {
                BufferedReader bufferedReader;
                journal = makeJournal(useCat).start();
                bufferedReader = new BufferedReader(new InputStreamReader(journal.getInputStream()));
                String line;
                while ((journal.isAlive() && (line = bufferedReader.readLine()) != null)) {
                    reader.printAbove(line + "\n");
                }
            } catch (Throwable e) {
                reader.printAbove(e.getMessage());
                if (journal != null && journal.isAlive())
                    journal.destroy();
                stop.set(true);
            }
        });
        journalReader.setDaemon(true);
        journalReader.start();
        while (true) {
            try {
                String line = reader.readLine(name+" > ");
                socketIo.write(line.getBytes());
                socketIo.write('\n');
                socketIo.flush();
                if (stop.get())
                    break;
            } catch (UserInterruptException | EndOfFileException e) {
                break;
            } catch (Exception | Error e) {
                e.printStackTrace();
                break;
            }
        }
        journalReader.interrupt();
    }

    private ProcessBuilder makeJournal(boolean useCat) {
        ProcessBuilder b = new ProcessBuilder();
        List<String> command = new ArrayList<>(Arrays.asList("journalctl", "-u", "javactl-" + name + ".service", "-b", "-f", "-a"));
        if (useCat)
            command.addAll(Arrays.asList("--output", "cat"));
        b.command(command);
        b.redirectErrorStream(true);
        return b;
    }
}

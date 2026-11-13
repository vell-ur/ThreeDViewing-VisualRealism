package com.grafkom.viewing3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Titik masuk aplikasi "3D Visual Realism Suite".
 * Di macOS, GLFW wajib berjalan di thread utama (-XstartOnFirstThread); bila belum,
 * program otomatis menjalankan ulang dirinya dengan opsi tersebut.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        if (relaunchOnMacIfNeeded()) {
            return;
        }
        new VisualRealismSuite().run();
    }

    private static boolean relaunchOnMacIfNeeded() throws Exception {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("mac") && !os.contains("darwin")) {
            return false;
        }
        String key = "JAVA_STARTED_ON_FIRST_THREAD_" + ProcessHandle.current().pid();
        if ("1".equals(System.getenv(key))) {
            return false;
        }
        List<String> cmd = new ArrayList<>();
        cmd.add(System.getProperty("java.home") + "/bin/java");
        cmd.add("-XstartOnFirstThread");
        cmd.add("-cp");
        cmd.add(System.getProperty("java.class.path"));
        cmd.add(Main.class.getName());
        Process p = new ProcessBuilder(cmd).inheritIO().start();
        System.exit(p.waitFor());
        return true;
    }
}

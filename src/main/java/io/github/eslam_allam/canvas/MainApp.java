package io.github.eslam_allam.canvas;

import io.github.eslam_allam.canvas.cli.CliApp;
import io.github.eslam_allam.canvas.gui.CanvasRubricGuiApp;

public class MainApp {
    public static void main(String[] args) throws Exception {
        boolean gui = false;
        boolean cli = false;

        for (String arg : args) {
            if ("--gui".equals(arg)) {
                gui = true;
            } else if ("--cli".equals(arg)) {
                cli = true;
            }
        }

        if (gui && cli) {
            System.err.println("Cannot specify both --gui and --cli.");
            System.exit(1);
        }

        if (gui) {
            CanvasRubricGuiApp.main(stripFlag(args, "--gui"));
        } else {
            CliApp.main(stripFlag(args, "--cli"));
        }
    }

    private static String[] stripFlag(String[] args, String flag) {
        return java.util.Arrays.stream(args)
                .filter(a -> !a.equals(flag))
                .toArray(String[]::new);
    }
}

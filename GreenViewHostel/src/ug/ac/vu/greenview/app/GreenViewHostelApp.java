package ug.ac.vu.greenview.app;

import java.nio.file.Path;
import java.nio.file.Paths;

import ug.ac.vu.greenview.core.AppConfig;
import ug.ac.vu.greenview.core.InputClosedException;

/**
 * Green View Hostel Management System - the program starts here (run this class).
 * Optionally pass a data folder as the first argument; the default is "data".
 *
 * @author Nakayi Jamillah (Member 6 - Shared Core and Integration)
 */
public final class GreenViewHostelApp {

    private GreenViewHostelApp() { }

    public static void main(String[] args) {
        Path dataDir = Paths.get(args.length > 0 ? args[0] : AppConfig.DATA_DIR);
        System.out.println("==============================================");
        System.out.println("   " + AppConfig.HOSTEL_NAME.toUpperCase() + " MANAGEMENT SYSTEM");
        System.out.println("   Group " + AppConfig.GROUP_CODE + " - Object Oriented Programming");
        System.out.println("==============================================");
        System.out.println("Data folder: " + dataDir.toAbsolutePath());
        try {
            HostelSystem system = new HostelSystem(dataDir);
            new MainMenu(system).run();
            System.out.println("Goodbye. All changes were saved.");
        } catch (InputClosedException e) {
            System.out.println();
            System.out.println("Input closed. All changes were already saved. Goodbye.");
        } catch (RuntimeException e) {
            System.out.println("The program had to stop: " + e);
            System.out.println("Your data files were not changed by this error.");
        }
    }
}

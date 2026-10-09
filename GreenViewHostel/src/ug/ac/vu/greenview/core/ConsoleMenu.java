package ug.ac.vu.greenview.core;

/**
 * Template for every submenu. A module only supplies a title, its options and
 * what each option does; this class shows the menu, reads the choice and makes
 * sure no error can escape and crash the program.
 *
 * @author Nakayi Jamillah (Member 6 - Shared Core and Integration)
 */
public abstract class ConsoleMenu {

    protected abstract String title();

    /** The numbered options (the "0" option is added automatically). */
    protected abstract String[] options();

    /** Runs option number 1..options().length. */
    protected abstract void handle(int choice) throws HostelException;

    protected String backLabel() {
        return "Back to main menu";
    }

    public final void run() {
        String[] options = options();
        while (true) {
            System.out.println();
            System.out.println("========== " + title() + " ==========");
            for (int i = 0; i < options.length; i++) {
                System.out.println((i + 1) + ". " + options[i]);
            }
            System.out.println("0. " + backLabel());
            int choice = InputHelper.readInt("Enter choice: ", 0, options.length);
            if (choice == 0) {
                return;
            }
            try {
                handle(choice);
            } catch (HostelException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (InputClosedException e) {
                throw e;
            } catch (RuntimeException e) {
                System.out.println("Something went wrong (" + e + "). Nothing was lost; please try again.");
            }
        }
    }

    /** Small helper so menus print lists consistently. */
    protected static void printList(String heading, java.util.Collection<? extends Record> records) {
        System.out.println();
        System.out.println("--- " + heading + " (" + records.size() + ") ---");
        if (records.isEmpty()) {
            System.out.println("(none)");
        } else {
            System.out.print(Record.describeAll(records));
        }
    }
}

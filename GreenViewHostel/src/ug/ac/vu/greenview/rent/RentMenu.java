package ug.ac.vu.greenview.rent;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import ug.ac.vu.greenview.core.AppConfig;
import ug.ac.vu.greenview.core.ConsoleMenu;
import ug.ac.vu.greenview.core.HostelException;
import ug.ac.vu.greenview.core.InputHelper;

/**
 * The Rent Payments submenu.
 *
 * @author Nabongo Mark (Member 3 - Rent Payment Management)
 */
public class RentMenu extends ConsoleMenu {

    private final RentPaymentManager manager;

    public RentMenu(RentPaymentManager manager) {
        this.manager = manager;
    }

    @Override
    protected String title() {
        return "RENT PAYMENT MANAGEMENT";
    }

    @Override
    protected String[] options() {
        return new String[] {
            "Add rent record (tenant + room + month)",
            "Record a payment",
            "Check payment status",
            "Update a rent record",
            "View all rent records",
            "View records for a tenant",
            "View records for a room",
            "Paid rent report",
            "Unpaid rent report",
            "Remove a rent record"
        };
    }

    @Override
    protected void handle(int choice) throws HostelException {
        switch (choice) {
            case 1: addRentRecord(); break;
            case 2: recordPayment(); break;
            case 3: checkStatus(); break;
            case 4: updateRecord(); break;
            case 5: printList("All rent records", manager.getAll()); break;
            case 6: showList("Tenant ID: ", true); break;
            case 7: showList("Room number or ID: ", false); break;
            case 8: System.out.println(manager.generatePaidReport()); break;
            case 9: System.out.println(manager.generateUnpaidReport()); break;
            case 10: removeRecord(); break;
            default: break;
        }
    }

    private void addRentRecord() throws HostelException {
        String tenantId = InputHelper.readRequired("Tenant ID: ", 20);
        YearMonth period = InputHelper.readYearMonth("Rent month (YYYY-MM, e.g. " + YearMonth.now() + "): ");
        Long due = InputHelper.readOptionalLong("Amount due in UGX (Enter to use the room's monthly rent): ",
                1, RentPayment.MAX_AMOUNT);
        RentPayment p = manager.addPayment(tenantId, period, due);
        System.out.println("Rent record created: " + p.describe());
    }

    private void recordPayment() throws HostelException {
        RentPayment p = manager.findById(InputHelper.readRequired("Rent record ID: ", 20));
        if (p == null) {
            throw new InvalidPaymentException("No rent record found with that ID.");
        }
        System.out.println(p.describe());
        if (p.getBalance() <= 0) {
            throw new InvalidPaymentException("This record is already fully paid.");
        }
        long amount = InputHelper.readLong("Amount paid (UGX, at most " + AppConfig.money(p.getBalance()) + "): ",
                1, RentPayment.MAX_AMOUNT);
        LocalDate date = InputHelper.readDateOrToday("Payment date (YYYY-MM-DD, Enter for today): ");
        RentPayment updated = manager.recordPayment(p.getId(), amount, date);
        System.out.println("Payment recorded: " + updated.describe());
    }

    private void checkStatus() throws HostelException {
        String id = InputHelper.readRequired("Rent record ID: ", 20);
        PaymentStatus status = manager.checkStatus(id);
        System.out.println("Status of " + manager.findById(id).getId() + ": " + status);
    }

    private void updateRecord() throws HostelException {
        RentPayment p = manager.findById(InputHelper.readRequired("Rent record ID to update: ", 20));
        if (p == null) {
            throw new InvalidPaymentException("No rent record found with that ID.");
        }
        System.out.println(p.describe());
        int n = 1;
        for (PaymentTransaction t : p.getTransactions()) {
            System.out.println("   Payment " + n++ + ": " + t);
        }
        System.out.println("1. Change the amount due");
        System.out.println("2. Correct one payment (amount / date)");
        System.out.println("3. Delete one wrongly entered payment");
        int what = InputHelper.readInt("What do you want to do? (1-3): ", 1, 3);
        RentPayment result;
        if (what == 1) {
            long due = InputHelper.readLong("New amount due (UGX): ", 1, RentPayment.MAX_AMOUNT);
            result = manager.updateAmountDue(p.getId(), due);
        } else {
            if (p.getTransactions().isEmpty()) {
                throw new InvalidPaymentException("This record has no payments yet.");
            }
            int number = InputHelper.readInt("Payment number (1-" + p.getTransactions().size() + "): ",
                    1, p.getTransactions().size());
            if (what == 2) {
                long amount = InputHelper.readLong("Correct amount (UGX): ", 1, RentPayment.MAX_AMOUNT);
                LocalDate date = InputHelper.readDate("Correct date (YYYY-MM-DD): ");
                result = manager.correctPayment(p.getId(), number, amount, date);
            } else {
                result = manager.deletePaymentEntry(p.getId(), number);
            }
        }
        System.out.println("Record updated: " + result.describe());
    }

    private void showList(String prompt, boolean byTenant) {
        String key = InputHelper.readRequired(prompt, 20);
        List<RentPayment> list = byTenant ? manager.findByTenant(key) : manager.findByRoom(key);
        printList("Rent records", list);
    }

    private void removeRecord() throws HostelException {
        RentPayment p = manager.findById(InputHelper.readRequired("Rent record ID to remove: ", 20));
        if (p == null) {
            throw new InvalidPaymentException("No rent record found with that ID.");
        }
        System.out.println(p.describe());
        if (!InputHelper.readYesNo("Permanently remove this rent record?")) {
            System.out.println("Cancelled. Nothing was removed.");
            return;
        }
        manager.removePayment(p.getId());
        System.out.println("Rent record " + p.getId() + " removed.");
    }
}

# Green View Hostel Management System

Java command-line (no GUI, no database) system for **Green View Hostel**.
Course: 1301 ST - Object Oriented Programming. Seven members, five modules, one shared core.

## How to run in VS Code

1. Install a **JDK 17 or newer** and the VS Code **"Extension Pack for Java"**.
2. *File > Open Folder...* and choose this `GreenViewHostel` folder (the one containing `src`).
3. Open `src/ug/ac/vu/greenview/app/GreenViewHostelApp.java` and click **Run** above `main`
   (or press F5 and pick *Green View Hostel (run the system)*).

Without VS Code: double-click `run.bat` (Windows) or run `./run.sh` (Mac/Linux).
Manual: `javac -d out @sources.txt` then `java -cp out ug.ac.vu.greenview.app.GreenViewHostelApp`.

Data is saved in a `data` folder created next to the project. To try the system quickly choose
**8. Load sample (fictional) data** in the main menu on a fresh start.
To run the 95 automated tests: `test.bat` / `./test.sh` (results: `docs/TEST_PLAN.md`).

**Set your group number:** open `core/AppConfig.java` and change `GROUP_CODE = "G01"` to your group
(for example `"G07"`). All record IDs (G07-R001, G07-T001, ...) follow it.

## Who built what (every class has an `@author` tag)

| Member | Part | Package |
|---|---|---|
| Josephina Ayok Weiu | Rooms | `rooms` |
| Emmanuella Andrew | Tenants | `tenants` |
| Nabongo Mark | Rent payments | `rent` |
| Yasir Basheer Mohammed | Maintenance | `maintenance` |
| Khalid Abdalla | Visitors | `visitors` |
| Nakayi Jamillah | Shared core, main menu, integration | `core`, `app` |
| Bijeneza Ndege Fidele | File storage, testing | `storage`, `tests` |

Record IDs: R room, A room allocation, T tenant, P rent payment, M maintenance, V visitor, L visit.

## Menu tree

```
MAIN MENU
1 Rooms:        register | view all | check availability | allocate | vacate | update | remove | available report | occupied report
2 Tenants:      register | view all | search | update | link to room | vacate | remove | report by name | by room | without room
3 Rent:         add record | record payment | check status | update | view all | by tenant | by room | paid report | unpaid report | remove
4 Maintenance:  tenant complaint | room request | view all | search | start repair | mark completed | edit | remove | pending report | completed report
5 Visitors:     register | check in | check out | view all | search visitors | search visits | update visitor | update visit |
                remove visit | remove visitor | inside now | history | by date | by tenant
6 Whole-hostel summary report
7 Record counts for the whole system
8 Load sample (fictional) data
0 Exit
```

## Where each OOP idea is used

* **Abstract class** `core.Record` (ID, `describe()`, `toFields()`); **interface** `core.Reportable`; `core.RemovalListener`.
* **Inheritance / polymorphism:** `Room` > `EnsuiteRoom`; `Tenant` (abstract) > `StudentTenant`, `WorkingTenant`;
  `MaintenanceRequest` (abstract) > `TenantComplaint`, `RoomMaintenanceRequest`. Reports and "view all" loop over mixed
  objects calling `describe()`; `MainMenu.printCounts()` loops over every record of every class in the system.
* **Encapsulation:** all fields private; setters/constructors validate and throw module exceptions
  (`InvalidRoomException`, `RoomFullException`, `InvalidTenantException`, `InvalidPaymentException`,
  `InvalidMaintenanceException`, `InvalidVisitorException`, `VisitorLimitExceededException`, all extending `HostelException`).
* **Modules working together through object references:** a `RentPayment` holds a real `Tenant` and `Room`;
  a `Tenant` holds its `Room`; a `Visit` holds a `Visitor` and a `Tenant`; a `MaintenanceRequest` holds a `Room`.
* **Services** keep records in an `ArrayList` with add / find / update / remove, and every service saves after each change.
* **Comparator reports:** rooms by room number (natural order A2 < A10), occupied rooms by crowding, tenants by name / room,
  rent by tenant then month, maintenance by priority then date, visits newest first.
* **Storage:** `storage.RecordStore` loads at start-up, saves after every change (write-to-temp-then-swap), skips damaged
  lines with a warning, backs up damaged files, never crashes on a missing file.
* **One `InputHelper`** is the only class that reads the keyboard; it keeps asking until the input is valid and shuts down
  politely if the input stream closes.

## Business rules implemented

* Room numbers are unique; capacity 1-8; a room can never hold more tenants than beds (`RoomFullException`);
  a room with tenants, unpaid rent or open repairs cannot be removed. Rent is per tenant per month.
* Tenants need a valid name and an emergency contact; duplicates are rejected; moving a tenant frees the old bed first.
* Rent: one record per tenant + room + month; payments add up; status PAID / PARTIAL / UNPAID is calculated; no overpayment,
  no future dates; a tenant who still owes rent cannot be removed.
* Maintenance: Pending > In progress > Completed (cannot go back or be edited once completed).
* Visitors: a tenant may have at most 3 visitors inside at once (`VisitorService.MAX_VISITORS_AT_ONCE`);
  check-out must be after check-in; no future check-ins.
* Removing a tenant deletes their rent and visit records (the complaint history is kept, with the name).
* All names and numbers in the sample data are invented. Never type real phone numbers or account details.

## Note on Member 3's code (`rent work.zip`)

Nabongo Mark's classes `RentPayment`, `RentPaymentManager`, `PaymentStatus`, `InvalidPaymentException` and the menu were
kept and merged into the shared design. Changes made so they fit and have no weak spots:
amounts are whole `long` UGX (the old `double` accepted `Infinity`); the month is a validated `YearMonth` instead of free text;
payments are now a list of `PaymentTransaction`s (amount + date each); tenant and room are real objects, not typed-in IDs;
his stand-in `Record`, `InputHelper` and `IdGenerator` were replaced by the shared core versions.

## For the report

* `docs/UML.md` - starting class diagram (Mermaid; redraw in draw.io and check it against the code).
* `docs/TEST_PLAN.md` and `docs/TEST_RESULTS.txt` - test cases with actual results.
* Change request (Checkpoint 2): easy examples are in the design: change `MAX_VISITORS_AT_ONCE`, add a late-fee rule in
  `RentPayment`, or add a visiting-hours rule in `Visit.checkWhen`. Record which classes you changed and who changed them.

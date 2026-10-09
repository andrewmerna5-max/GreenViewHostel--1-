# Green View Hostel - System Test Plan and Actual Results

Prepared by Bijeneza Ndege Fidele (Member 7 - File Storage and Testing).

How to repeat: run `test.bat` (Windows) / `./test.sh`, or run the *Run all system tests* launch entry in VS Code.
Every test uses a fresh temporary data folder, so your real `data` folder is never touched.

Every module has well over three tests, including invalid-input tests.

| ID | Module | Test case | Expected | Actual | Result |
|---|---|---|---|---|---|
| ROOM-01 | Rooms | Register a valid room | G01-R001 A101 cap2 | G01-R001 A101 cap2 | PASS |
| ROOM-02 | Rooms | Duplicate room number is rejected | rejected: InvalidRoomException | rejected: InvalidRoomException | PASS |
| ROOM-03 | Rooms | Capacity 0 and capacity 9 are rejected | rejected: InvalidRoomException/rejected: InvalidRoomException | rejected: InvalidRoomException/rejected: InvalidRoomException | PASS |
| ROOM-04 | Rooms | Zero / negative rent and bad room number are rejected | rejected: InvalidRoomException/rejected: InvalidRoomException/rejected: InvalidRoomException | rejected: InvalidRoomException/rejected: InvalidRoomException/rejected: InvalidRoomException | PASS |
| ROOM-05 | Rooms | Allocating a tenant to a full room throws RoomFullException | rejected: RoomFullException | rejected: RoomFullException | PASS |
| ROOM-06 | Rooms | A room with tenants cannot be removed | rejected: InvalidRoomException | rejected: InvalidRoomException | PASS |
| ROOM-07 | Rooms | Capacity cannot drop below occupants, and nothing changes | rejected: InvalidRoomException A1 cap2 | rejected: InvalidRoomException A1 cap2 | PASS |
| ROOM-08 | Rooms | Update room details | A2 cap3 rent500000 | A2 cap3 rent500000 | PASS |
| ROOM-09 | Rooms | Available rooms report is filtered (full rooms hidden) and naturally sorted | A2,A10 | A2,A10 | PASS |
| ROOM-10 | Rooms | Moving a tenant frees the old bed and fills the new one | old0 new1 | old0 new1 | PASS |
| ROOM-11 | Rooms | Putting a tenant in the room they are already in is rejected | rejected: InvalidRoomException | rejected: InvalidRoomException | PASS |
| ROOM-12 | Rooms | Rooms and occupancy survive a restart (Room and EnsuiteRoom classes) | A1:Room:1 A2:EnsuiteRoom:0 | A1:Room:1 A2:EnsuiteRoom:0 | PASS |
| TEN-01 | Tenants | Register a valid tenant linked to a room | G01-T001 room A1 | G01-T001 room A1 | PASS |
| TEN-02 | Tenants | Invalid names are rejected (digits, empty, 1 letter) | rejected: InvalidTenantException/rejected: InvalidTenantException/rejected: InvalidTenantException | rejected: InvalidTenantException/rejected: InvalidTenantException/rejected: InvalidTenantException | PASS |
| TEN-03 | Tenants | Invalid phone numbers are rejected | rejected: InvalidTenantException/rejected: InvalidTenantException | rejected: InvalidTenantException/rejected: InvalidTenantException | PASS |
| TEN-04 | Tenants | Duplicate tenant is rejected | rejected: InvalidTenantException | rejected: InvalidTenantException | PASS |
| TEN-05 | Tenants | Registering into a full room fails and registers nobody | rejected: RoomFullException count=1 | rejected: RoomFullException count=1 | PASS |
| TEN-06 | Tenants | Registering into a room that does not exist fails | rejected: InvalidRoomException count=0 | rejected: InvalidRoomException count=0 | PASS |
| TEN-07 | Tenants | Search finds by name part, by room number, and nothing for junk | 1/1/0 | 1/1/0 | PASS |
| TEN-08 | Tenants | Update tenant details; bad value changes nothing | Aaron Test \| Better Name | Aaron Test \| Better Name | PASS |
| TEN-09 | Tenants | Removing a tenant frees their bed | bed free=2 count=0 | bed free=2 count=0 | PASS |
| TEN-10 | Tenants | Vacating a tenant who has no room is rejected | rejected: InvalidRoomException | rejected: InvalidRoomException | PASS |
| TEN-11 | Tenants | Removing an unknown tenant is rejected | rejected: InvalidTenantException | rejected: InvalidTenantException | PASS |
| TEN-12 | Tenants | Tenants survive a restart with room link and correct subclass | StudentTenant room A1 \| WorkingTenant no room | StudentTenant room A1 \| WorkingTenant no room | PASS |
| RENT-01 | Rent | Rent record uses the room's rent by default and is linked to tenant and room | G01-P001 due400000 UNPAID A1 Aaron Test | G01-P001 due400000 UNPAID A1 Aaron Test | PASS |
| RENT-02 | Rent | Status moves UNPAID -> PARTIAL -> PAID | UNPAID/PARTIAL/PAID | UNPAID/PARTIAL/PAID | PASS |
| RENT-03 | Rent | Overpayment is rejected and nothing is recorded | rejected: InvalidPaymentException paid=0 | rejected: InvalidPaymentException paid=0 | PASS |
| RENT-04 | Rent | Zero, negative and future-dated payments are rejected | rejected: InvalidPaymentException/rejected: InvalidPaymentException/rejected: InvalidPaymentException | rejected: InvalidPaymentException/rejected: InvalidPaymentException/rejected: InvalidPaymentException | PASS |
| RENT-05 | Rent | Duplicate rent record for same tenant, room and month is rejected | rejected: InvalidPaymentException | rejected: InvalidPaymentException | PASS |
| RENT-06 | Rent | Tenant without a room cannot be charged rent | rejected: InvalidPaymentException | rejected: InvalidPaymentException | PASS |
| RENT-07 | Rent | Unknown tenant and absurd month are rejected | rejected: InvalidTenantException/rejected: InvalidPaymentException | rejected: InvalidTenantException/rejected: InvalidPaymentException | PASS |
| RENT-08 | Rent | Amount due above the limit or zero is rejected | rejected: InvalidPaymentException/rejected: InvalidPaymentException | rejected: InvalidPaymentException/rejected: InvalidPaymentException | PASS |
| RENT-09 | Rent | Amount due cannot be set below what is already paid | rejected: InvalidPaymentException due=200000 | rejected: InvalidPaymentException due=200000 | PASS |
| RENT-10 | Rent | Correct a payment, then delete a payment | paid=120000 paid=0 | paid=120000 paid=0 | PASS |
| RENT-11 | Rent | Correcting a payment above the amount due is rejected; bad payment number is rejected | rejected: InvalidPaymentException/rejected: InvalidPaymentException | rejected: InvalidPaymentException/rejected: InvalidPaymentException | PASS |
| RENT-12 | Rent | Paid and unpaid reports split the records correctly | paid=1 unpaid=2 | paid=1 unpaid=2 | PASS |
| RENT-13 | Rent | Tenant who owes rent cannot be removed; after paying they can, and their rent records go | rejected: InvalidPaymentException then removed rentRecords=0 | rejected: InvalidPaymentException then removed rentRecords=0 | PASS |
| RENT-14 | Rent | Rent records (multiple payments) survive a restart and re-link to tenant and room | PARTIAL paid=150000 Aaron Test A1 | PARTIAL paid=150000 Aaron Test A1 | PASS |
| RENT-15 | Rent | Removing a rent record | removed count=0 | removed count=0 | PASS |
| MAINT-01 | Maintenance | Record a tenant complaint (room taken from the tenant) and a staff room request | G01-M001 A1 Tenant complaint \| G01-M002 A1 Room request | G01-M001 A1 Tenant complaint \| G01-M002 A1 Room request | PASS |
| MAINT-02 | Maintenance | Too-short and over-long descriptions are rejected | rejected: InvalidMaintenanceException/rejected: InvalidMaintenanceException | rejected: InvalidMaintenanceException/rejected: InvalidMaintenanceException | PASS |
| MAINT-03 | Maintenance | Complaint from a tenant without a room, and request for unknown room, are rejected | rejected: InvalidMaintenanceException/rejected: InvalidRoomException | rejected: InvalidMaintenanceException/rejected: InvalidRoomException | PASS |
| MAINT-04 | Maintenance | Status flow Pending -> In progress -> Completed; start twice is rejected | Pending/In progress/rejected: InvalidMaintenanceException/Completed | Pending/In progress/rejected: InvalidMaintenanceException/Completed | PASS |
| MAINT-05 | Maintenance | Completion date in the future or before the report date is rejected | rejected: InvalidMaintenanceException/rejected: InvalidMaintenanceException | rejected: InvalidMaintenanceException/rejected: InvalidMaintenanceException | PASS |
| MAINT-06 | Maintenance | A completed request cannot be completed again or edited | rejected: InvalidMaintenanceException/rejected: InvalidMaintenanceException | rejected: InvalidMaintenanceException/rejected: InvalidMaintenanceException | PASS |
| MAINT-07 | Maintenance | Edit an open request | Electrical Urgent | Electrical Urgent | PASS |
| MAINT-08 | Maintenance | Pending report is filtered (no completed) and sorted most-urgent first | M003,M001 completed excluded | M003,M001 completed excluded | PASS |
| MAINT-09 | Maintenance | Search finds by word, room and status | 1/2/1 | 1/2/1 | PASS |
| MAINT-10 | Maintenance | Room with an open request cannot be removed; after completion it can (history goes with it) | rejected: InvalidMaintenanceException then removed records=0 | rejected: InvalidMaintenanceException then removed records=0 | PASS |
| MAINT-11 | Maintenance | Removing a tenant keeps their complaint history with the name | kept=1 by Aaron Test tenantLink=none | kept=1 by Aaron Test tenantLink=none | PASS |
| MAINT-12 | Maintenance | Maintenance records survive a restart with status, notes and subclass | TenantComplaint Completed Fixed \| RoomMaintenanceRequest In progress | TenantComplaint Completed Fixed \| RoomMaintenanceRequest In progress | PASS |
| MAINT-13 | Maintenance | Text with the file separator and a backslash in it survives a restart | Pipe \| and \ slash ok | Pipe \| and \ slash ok | PASS |
| VIS-01 | Visitors | Register a visitor, check in and check out | G01-V001 G01-L001 inside=true then false | G01-V001 G01-L001 inside=true then false | PASS |
| VIS-02 | Visitors | Invalid visitor names and phone are rejected | rejected: InvalidVisitorException/rejected: InvalidVisitorException/rejected: InvalidVisitorException | rejected: InvalidVisitorException/rejected: InvalidVisitorException/rejected: InvalidVisitorException | PASS |
| VIS-03 | Visitors | Duplicate visitor (same name and phone) is rejected | rejected: InvalidVisitorException | rejected: InvalidVisitorException | PASS |
| VIS-04 | Visitors | Check-in with a future date or future time is rejected | rejected: InvalidVisitorException/rejected: InvalidVisitorException | rejected: InvalidVisitorException/rejected: InvalidVisitorException | PASS |
| VIS-05 | Visitors | Check-out before check-in time is rejected and visitor stays inside | rejected: InvalidVisitorException inside=true | rejected: InvalidVisitorException inside=true | PASS |
| VIS-06 | Visitors | Checking out twice is rejected | rejected: InvalidVisitorException | rejected: InvalidVisitorException | PASS |
| VIS-07 | Visitors | Same visitor cannot be inside twice | rejected: InvalidVisitorException | rejected: InvalidVisitorException | PASS |
| VIS-08 | Visitors | A 4th visitor for one tenant throws VisitorLimitExceededException | rejected: VisitorLimitExceededException | rejected: VisitorLimitExceededException | PASS |
| VIS-09 | Visitors | Visit to an unknown tenant is rejected | rejected: InvalidTenantException | rejected: InvalidTenantException | PASS |
| VIS-10 | Visitors | Tenant cannot be removed while a visitor is inside; after check-out removal deletes their visits | rejected: InvalidVisitorException then visits=0 | rejected: InvalidVisitorException then visits=0 | PASS |
| VIS-11 | Visitors | Visitor who is inside cannot be removed | rejected: InvalidVisitorException | rejected: InvalidVisitorException | PASS |
| VIS-12 | Visitors | Update a visit: bad time rejected, good change applied | rejected: InvalidVisitorException 09:15 Delivery | rejected: InvalidVisitorException 09:15 Delivery | PASS |
| VIS-13 | Visitors | Reports: inside is filtered, date report is filtered, history is newest first | inside=1 onDate=1 newestFirst=true | inside=1 onDate=1 newestFirst=true | PASS |
| VIS-14 | Visitors | Visitors and visits survive a restart and re-link | Guest One -> Aaron Test out=11:00 | Guest One -> Aaron Test out=11:00 | PASS |
| VIS-15 | Visitors | Mixed Visitor and Visit objects are listed together | 2 visitors + 1 visit = 3 records | 2 visitors + 1 visit = 3 records | PASS |
| STORE-01 | Storage | First run with no data folder or files starts empty without crashing | rooms=0 tenants=0 | rooms=0 tenants=0 | PASS |
| STORE-02 | Storage | Data folder is created and files are written after every change | true true | true true | PASS |
| STORE-03 | Storage | One damaged line is skipped, good lines still load, a backup is kept | rooms=2 backup=true | rooms=2 backup=true | PASS |
| STORE-04 | Storage | A completely binary / unreadable file does not crash the program | tenants=0 then app works | tenants=0 then app works | PASS |
| STORE-05 | Storage | An empty file and a file with only blank/comment lines load as empty | rooms=0 | rooms=0 | PASS |
| STORE-06 | Storage | Deleted record's ID is never reused after a restart | G01-R003 | G01-R003 | PASS |
| STORE-07 | Storage | ID counter survives even if counters.txt is deleted (rebuilt from records) | G01-R003 | G01-R003 | PASS |
| STORE-08 | Storage | Damaged counters.txt is ignored safely | G01-R002 | G01-R002 | PASS |
| STORE-09 | Storage | Records pointing at a deleted tenant are dropped and the bed is freed on load | tenants=0 rent=0 freeBeds=2 | tenants=0 rent=0 freeBeds=2 | PASS |
| STORE-10 | Storage | Allocation pointing at a missing room is dropped on load | rooms=0 allocations=0 | rooms=0 allocations=0 | PASS |
| STORE-11 | Storage | Over-booked data file (more tenants than beds) is repaired on load | occupants=1 | occupants=1 | PASS |
| STORE-12 | Storage | join/split round trip with separator, backslash and line breaks | ok | ok | PASS |
| STORE-13 | Storage | Duplicate IDs in a file: the second copy is skipped | rooms=1 | rooms=1 | PASS |
| STORE-14 | Storage | Saving to an impossible location warns but does not crash | returned false | returned false | PASS |
| STORE-15 | Storage | A full set of data written by one run is read back identically by the next | 24 24 | 24 24 | PASS |
| CORE-01 | Core | InputHelper.readInt ignores garbage until a valid number | 3 | 3 | PASS |
| CORE-02 | Core | InputHelper.readLong accepts 450,000 and rejects NaN / Infinity / text | 450000 | 450000 | PASS |
| CORE-03 | Core | InputHelper.readDate rejects 2026-02-30 and bad formats | 2026-02-28 | 2026-02-28 | PASS |
| CORE-04 | Core | InputHelper.readTime rejects 25:00, 9:5 and text | 09:05 | 09:05 | PASS |
| CORE-05 | Core | InputHelper.readName rejects digits and symbols | Mary Anne | Mary Anne | PASS |
| CORE-06 | Core | End of input throws InputClosedException (no endless loop) | rejected: InputClosedException | rejected: InputClosedException | PASS |
| CORE-07 | Core | readYesNo accepts y/yes/n/no only | true/false | true/false | PASS |
| CORE-08 | Core | NaturalOrder sorts A2 < A10 and G01-T9 < G01-T10 | true true true | true true true | PASS |
| CORE-09 | Core | Every record ID starts with the group code and the right letter | R,A,T,P,M,V,L all G01- | R,A,T,P,M,V,L all G01- | PASS |
| CORE-10 | Core | Sample data loads once and refuses to load into a non-empty system | true/false | true/false | PASS |
| CORE-11 | Core | Every module is Reportable and produces a report | 5 modules, all reports non-empty | 5 modules, all reports non-empty | PASS |
| CORE-12 | Core | Menu scenario typed like a real user: room -> tenant -> rent -> payment (all via menus) | tenant=G01-T001 room=A1 paid=100000 status=PARTIAL | tenant=G01-T001 room=A1 paid=100000 status=PARTIAL | PASS |
| STRESS-01 | Stress | 40 runs of 600 random inputs typed into the whole menu system: no crash, no 'went wrong' message, files still load cleanly | 40 runs clean | 40 runs clean | PASS |

**Total: 95 | Passed: 95 | Failed: 0**

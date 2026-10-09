package ug.ac.vu.greenview.tenants;

import java.time.LocalDate;

import ug.ac.vu.greenview.core.Record;
import ug.ac.vu.greenview.core.Validator;
import ug.ac.vu.greenview.rooms.Room;

/**
 * A person living (or registered to live) at Green View Hostel. Abstract: a
 * tenant is always either a StudentTenant or a WorkingTenant. The tenant keeps
 * an object reference to the Room they are allocated to.
 *
 * @author Emmanuella Andrew (Member 2 - Tenant Management)
 */
public abstract class Tenant extends Record {

    private String fullName;
    private String phone;            // "" when none was given
    private String affiliation;      // institution for students, employer for workers
    private EmergencyContact emergencyContact;
    private final LocalDate dateRegistered;
    private Room room;               // null when the tenant has no room

    protected Tenant(String id, String fullName, String phone, String affiliation,
                     EmergencyContact emergencyContact, LocalDate dateRegistered) throws InvalidTenantException {
        super(id);
        this.fullName = validateName(fullName);
        this.phone = validatePhone(phone);
        this.affiliation = validateAffiliation(affiliation);
        this.emergencyContact = validateContact(emergencyContact);
        if (dateRegistered == null || dateRegistered.isAfter(LocalDate.now())) {
            throw new InvalidTenantException("Registration date is required and cannot be in the future.");
        }
        this.dateRegistered = dateRegistered;
    }

    // ----------------------------------------------------- validation rules

    public static String validateName(String name) throws InvalidTenantException {
        if (!Validator.isValidName(name)) {
            throw new InvalidTenantException("Name must be 2 to 60 letters (spaces, - . ' allowed).");
        }
        return Validator.clean(name);
    }

    /** Phone is optional: an empty value is accepted and stored as "". */
    public static String validatePhone(String phone) throws InvalidTenantException {
        String p = phone == null ? "" : phone.replace(" ", "");
        if (!p.isEmpty() && !Validator.isValidPhone(p)) {
            throw new InvalidTenantException("Phone must be 9 to 13 digits (use a fictional number).");
        }
        return p;
    }

    public static String validateAffiliation(String affiliation) throws InvalidTenantException {
        if (!Validator.isTextOfLength(affiliation, 2, 60)) {
            throw new InvalidTenantException("Institution/employer must be 2 to 60 characters.");
        }
        return Validator.clean(affiliation);
    }

    private static EmergencyContact validateContact(EmergencyContact contact) throws InvalidTenantException {
        if (contact == null) {
            throw new InvalidTenantException("An emergency contact is required.");
        }
        return contact;
    }

    // ------------------------------------------------- what subclasses decide

    public abstract TenantType getType();

    /** "Institution" or "Employer". */
    public abstract String getAffiliationLabel();

    // --------------------------------------------------------------- getters

    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getAffiliation() { return affiliation; }
    public EmergencyContact getEmergencyContact() { return emergencyContact; }
    public LocalDate getDateRegistered() { return dateRegistered; }
    public Room getRoom() { return room; }
    public boolean hasRoom() { return room != null; }

    // --------------------------------------------------------------- setters

    public final void setFullName(String fullName) throws InvalidTenantException {
        this.fullName = validateName(fullName);
    }

    public final void setPhone(String phone) throws InvalidTenantException {
        this.phone = validatePhone(phone);
    }

    public final void setAffiliation(String affiliation) throws InvalidTenantException {
        this.affiliation = validateAffiliation(affiliation);
    }

    public final void setEmergencyContact(EmergencyContact contact) throws InvalidTenantException {
        this.emergencyContact = validateContact(contact);
    }

    /** Checks every value first so a bad value changes nothing. */
    public final void updateDetails(String newName, String newPhone, String newAffiliation,
                                    EmergencyContact newContact) throws InvalidTenantException {
        String n = validateName(newName);
        String p = validatePhone(newPhone);
        String a = validateAffiliation(newAffiliation);
        EmergencyContact c = validateContact(newContact);
        this.fullName = n;
        this.phone = p;
        this.affiliation = a;
        this.emergencyContact = c;
    }

    /** Only the tenants module changes the room link (package-private on purpose). */
    void setRoomReference(Room room) {
        this.room = room;
    }

    // ------------------------------------------------------- Record methods

    @Override
    public String describe() {
        return String.format("%s | %s | %s | %s: %s | Room: %s | Phone: %s",
                getId(), fullName, getType(), getAffiliationLabel(), affiliation,
                room == null ? "not allocated" : room.getRoomNumber(),
                phone.isEmpty() ? "-" : phone);
    }

    @Override
    public String[] toFields() {
        return new String[] {getId(), getType().name(), fullName, phone, affiliation,
                emergencyContact.getName(), emergencyContact.getRelationship(),
                emergencyContact.getPhone(), dateRegistered.toString()};
    }

    /** Rebuilds a StudentTenant or WorkingTenant from one line of the tenants file. */
    public static Tenant fromFields(String[] f) throws Exception {
        if (f.length < 9) {
            throw new IllegalArgumentException("expected 9 fields but found " + f.length);
        }
        EmergencyContact contact = new EmergencyContact(f[5], f[6], f[7]);
        LocalDate registered = LocalDate.parse(f[8].trim());
        TenantType type = TenantType.valueOf(f[1].trim());
        switch (type) {
            case STUDENT: return new StudentTenant(f[0], f[2], f[3], f[4], contact, registered);
            case WORKING: return new WorkingTenant(f[0], f[2], f[3], f[4], contact, registered);
            default: throw new IllegalArgumentException("unknown tenant type");
        }
    }
}

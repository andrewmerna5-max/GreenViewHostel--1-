package ug.ac.vu.greenview.core;

/**
 * Shared interface for every module service that can produce a report.
 *
 * @author Nakayi Jamillah (Member 6 - Shared Core and Integration)
 */
public interface Reportable {

    /** Name of the module, used as a heading in system-wide reports. */
    String getModuleName();

    /** A complete, ready-to-print report for the module. */
    String generateReport();
}

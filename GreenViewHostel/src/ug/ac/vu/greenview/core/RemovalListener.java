package ug.ac.vu.greenview.core;

/**
 * Lets one module react when another module removes a record, so modules stay
 * consistent (for example, rent records must not point to a deleted tenant).
 * canRemove can veto the removal; onRemoved cleans up afterwards.
 *
 * @param <T> the type of record being removed
 * @author Nakayi Jamillah (Member 6 - Shared Core and Integration)
 */
public interface RemovalListener<T> {

    /** Throw a HostelException to stop the removal. */
    default void canRemove(T item) throws HostelException {
    }

    /** Called after the item has been removed. */
    default void onRemoved(T item) {
    }
}

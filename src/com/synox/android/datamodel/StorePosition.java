package com.synox.android.datamodel;

import android.os.Parcelable;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ddijak on 24.11.2015.
 */
public class StorePosition {

    private static ConcurrentHashMap<Long, Parcelable> savedListPosition = new ConcurrentHashMap<>(5);

    /**
     * Save current list position
     *
     * @param id
     * @param listState
     */
    public static void setParentPosition(long id, Parcelable listState) {
        savedListPosition.put(id, listState);
    }

    /**
     * Restore list position
     *
     * @param id
     * @return
     */
    public static Parcelable getParentPosition(long id) {
        Parcelable restorePosition = savedListPosition.get(id);
        return restorePosition;
    }

    /**
     * Return complete list position
     *
     * @return savedListPosition
     */
    public static ConcurrentHashMap<Long, Parcelable> getListPositionList() {
        return savedListPosition;
    }

    /**
     * Remove position from list
     * @param id
     */
    public static void removeParentPosition(long id)
    {
        if (savedListPosition.containsKey(id))
            savedListPosition.remove(id);
    }
}

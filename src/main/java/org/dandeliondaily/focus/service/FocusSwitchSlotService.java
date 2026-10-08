package org.dandeliondaily.focus.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Keeps the Focused Action switch bar in fixed positions, like browser tabs. An action that is
 * already in a slot never moves; a new action takes an empty slot, or else replaces the slot that
 * was used least recently. Slot ids of 0 are empty.
 */
public class FocusSwitchSlotService {

    public static final int SLOT_COUNT = 4;

    /**
     * @param slots          current slot ids (may be null, short, or contain 0 for empty)
     * @param recentIds      most-recently-used action ids, newest first, already including
     *                       currentId at the front
     * @param currentId      the action now being focused
     * @param unavailableIds slot ids whose action no longer exists or was cancelled
     * @return the new slot ids, always SLOT_COUNT long
     */
    public List<Integer> place(List<Integer> slots, List<Integer> recentIds, int currentId,
            Set<Integer> unavailableIds) {
        List<Integer> result = new ArrayList<Integer>();
        for (int i = 0; i < SLOT_COUNT; i++) {
            Integer id = slots != null && i < slots.size() ? slots.get(i) : null;
            if (id == null || id.intValue() <= 0
                    || (unavailableIds != null && unavailableIds.contains(id))
                    || result.contains(id)) {
                result.add(Integer.valueOf(0));
            } else {
                result.add(id);
            }
        }
        if (currentId <= 0 || result.contains(Integer.valueOf(currentId))) {
            return result;
        }

        int target = result.indexOf(Integer.valueOf(0));
        if (target < 0) {
            int oldestRank = -1;
            for (int i = 0; i < result.size(); i++) {
                int rank = recentIds == null ? -1 : recentIds.indexOf(result.get(i));
                if (rank < 0) {
                    rank = Integer.MAX_VALUE;
                }
                if (rank > oldestRank) {
                    oldestRank = rank;
                    target = i;
                }
            }
        }
        result.set(target, Integer.valueOf(currentId));
        return result;
    }
}

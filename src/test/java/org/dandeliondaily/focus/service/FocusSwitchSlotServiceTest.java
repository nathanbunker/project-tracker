package org.dandeliondaily.focus.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class FocusSwitchSlotServiceTest {

    private final FocusSwitchSlotService service = new FocusSwitchSlotService();

    @Test
    public void place_fillsFirstEmptySlot() {
        List<Integer> slots = service.place(null, Arrays.asList(5), 5, null);
        Assert.assertEquals(Arrays.asList(5, 0, 0, 0), slots);
        slots = service.place(slots, Arrays.asList(7, 5), 7, null);
        Assert.assertEquals(Arrays.asList(5, 7, 0, 0), slots);
    }

    @Test
    public void place_existingActionDoesNotMove() {
        List<Integer> slots = Arrays.asList(5, 7, 9, 11);
        Assert.assertEquals(slots, service.place(slots, Arrays.asList(9, 11, 7, 5), 9, null));
    }

    @Test
    public void place_replacesLeastRecentlyUsedSlot() {
        List<Integer> slots = Arrays.asList(5, 7, 9, 11);
        // 7 was used longest ago
        List<Integer> recent = Arrays.asList(13, 11, 5, 9, 7);
        Assert.assertEquals(Arrays.asList(5, 13, 9, 11), service.place(slots, recent, 13, null));
    }

    @Test
    public void place_slotMissingFromHistoryIsOldest() {
        List<Integer> slots = Arrays.asList(5, 7, 9, 11);
        List<Integer> recent = Arrays.asList(13, 5, 7, 11);
        Assert.assertEquals(Arrays.asList(5, 7, 13, 11), service.place(slots, recent, 13, null));
    }

    @Test
    public void place_unavailableActionFreesItsSlot() {
        List<Integer> slots = Arrays.asList(5, 7, 9, 11);
        List<Integer> placed = service.place(slots, Arrays.asList(13, 5, 7, 9, 11), 13,
                new HashSet<Integer>(Collections.singletonList(7)));
        Assert.assertEquals(Arrays.asList(5, 13, 9, 11), placed);
    }

    @Test
    public void place_padsShortListAndDropsDuplicates() {
        List<Integer> placed = service.place(Arrays.asList(5, 5), Arrays.asList(5), 5, null);
        Assert.assertEquals(Arrays.asList(5, 0, 0, 0), placed);
    }
}

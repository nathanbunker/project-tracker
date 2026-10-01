package org.openimmunizationsoftware.pt.model;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

public class ActionNextTest {

    @Test
    public void unchangedNotesPreserveExistingEntries() {
        ActionNext action = actionWithExistingNote();
        ActionNextNote existingNote = action.getNextNoteEntries().iterator().next();

        boolean changed = action.setNextNotesIfChanged(" - Existing note");

        Assert.assertFalse(changed);
        Assert.assertEquals(1, action.getNextNoteEntries().size());
        Assert.assertSame(existingNote, action.getNextNoteEntries().iterator().next());
        Assert.assertEquals(41, existingNote.getActionNextNoteId());
        Assert.assertEquals(73, existingNote.getContactId());
        Assert.assertEquals(new Date(123456789L), existingNote.getNoteDate());
    }

    @Test
    public void changedNotesCreateEntriesWithRequiredParentAndContactIds() {
        ActionNext action = actionWithExistingNote();

        boolean changed = action.setNextNotesIfChanged("Replacement note");

        Assert.assertTrue(changed);
        Assert.assertEquals(1, action.getNextNoteEntries().size());
        ActionNextNote replacement = action.getNextNoteEntries().iterator().next();
        Assert.assertEquals(101, replacement.getActionNextId());
        Assert.assertEquals(73, replacement.getContactId());
        Assert.assertEquals("Replacement note", replacement.getNoteLine());
        Assert.assertNotNull(replacement.getNoteDate());
    }

    @Test
    public void generatedParentIdPropagatesToNotesAddedBeforePersistence() {
        ActionNext action = new ActionNext();
        action.setContactId(Integer.valueOf(73));
        action.setNextNotes("Template note");

        action.setActionNextId(202);

        Assert.assertEquals(1, action.getNextNoteEntries().size());
        Assert.assertEquals(202, action.getNextNoteEntries().iterator().next().getActionNextId());
    }

    @Test
    public void noteCollectionMappingDeclaresNonNullParentKey() throws Exception {
        InputStream mapping = ActionNext.class
                .getResourceAsStream("/org/openimmunizationsoftware/pt/model/ActionNext.hbm.xml");
        Assert.assertNotNull(mapping);
        String xml;
        try {
            ByteArrayOutputStream content = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = mapping.read(buffer)) != -1) {
                content.write(buffer, 0, read);
            }
            xml = new String(content.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            mapping.close();
        }
        Assert.assertTrue(xml.contains("<set name=\"nextNoteEntries\" inverse=\"true\""));
        Assert.assertTrue(xml.contains("<key column=\"action_next_id\" not-null=\"true\" />"));
    }

    private ActionNext actionWithExistingNote() {
        ActionNext action = new ActionNext();
        action.setActionNextId(101);
        action.setContactId(Integer.valueOf(73));

        ActionNextNote note = new ActionNextNote();
        note.setActionNextNoteId(41);
        note.setActionNextId(101);
        note.setContactId(73);
        note.setNoteLine("Existing note");
        note.setNoteDate(new Date(123456789L));
        action.getNextNoteEntries().add(note);
        return action;
    }
}
package es.studium.notesapp.listeners;

import es.studium.notesapp.entities.Note;

public interface NotesListener {
    void onNoteClicked(Note note, int position);
}

package es.studium.notesapp.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import es.studium.notesapp.dao.NoteDao;
import es.studium.notesapp.entities.Note;

@Database(entities = {Note.class}, version = 1, exportSchema = false)
public abstract class NotesDatabase extends RoomDatabase {
    //UNA CLASE ABSTRACTA no puede ser instanciada directamente, es decir, no se puede crear un objeto de
// esta misma. Las clases abstractas se utilizan como plantillas para otras clases.

    // Definir una única instancia de la base de datos

    //volatile garantiza que la variable será siempre leída y escrita desde la memoria principal, lo que asegura
    // que los hilos vean siempre el valor más reciente de la variable
    private static volatile NotesDatabase notesDatabase;

    // Método para obtener la instancia de la base de datos
    public static synchronized NotesDatabase getNotesDatabase(Context context) {
        if (notesDatabase == null) {
            // Crear un metodo de clase de la base de datos usando Room.databaseBuilder,
            notesDatabase = Room.databaseBuilder(context.getApplicationContext(),
                            NotesDatabase.class, "notes_db")
                    .build();
        }
        return notesDatabase;
    }

    // Método abstracto para obtener el DAO
    public abstract NoteDao noteDao();
}


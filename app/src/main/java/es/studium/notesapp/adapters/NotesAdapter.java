package es.studium.notesapp.adapters;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Looper;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.makeramen.roundedimageview.RoundedImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import es.studium.notesapp.R;
import es.studium.notesapp.entities.Note;
import es.studium.notesapp.listeners.NotesListener;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder>{

    private List<Note> notes;
    private NotesListener notesListener;
    private Timer timer;
    private List<Note> notesSource;

    public NotesAdapter(List<Note> notes, NotesListener notesListener) {
        this.notes = notes;
        this.notesListener=notesListener;
        notesSource = notes;
    }
    @NonNull
    @Override
    //Se llama cuando el RecyclerView necesita una
    // nueva vista para representar un elemento.
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        //Crea una nueva instancia de NoteViewHolder pasando la vista inflada.
        return new NoteViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_container_note,
                        parent,
                        false
                )
        );
    }
    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, @SuppressLint("RecyclerView") int position) {
        // Se llama para vincular los datos a una vista. Aquí es donde los datos de una Note se asignan a los componentes de la vista.
        holder.setNote(notes.get(position));
        holder.layoutNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notesListener.onNoteClicked(notes.get(position), position);
            }
        });
    }
    @Override
    public int getItemCount() {
        // Devuelve el número total de elementos en la lista de datos para que el recycler cree las vistas necesarias.
        return notes.size();
    }
    @Override
    public int getItemViewType(int position) {
        //Devuelve el tipo de vista de un elemento en una posición determinada, cada elemento es único
        return position;
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder{//lo que le permite contener y gestionar las vistas individuales
        TextView textTitle, textSubtitle, textDateTime;

        //Se refiere a una vista de diseño (por ejemplo, un LinearLayout o RelativeLayout)
        LinearLayout layoutNote; //El layout perteneciente al item_container_note.Se refiere a una vista de diseño (por ejemplo, un LinearLayout o RelativeLayout)

        RoundedImageView imageNote;

        // Constructor que recibe la vista del elemento (inflada en onCreateViewHolder).
        NoteViewHolder(@NonNull View itemView) {
            super(itemView);// Llama al constructor de la superclase (RecyclerView.ViewHolder), pasando la vista itemView.
            textTitle = itemView.findViewById(R.id.textTitle);
            textSubtitle = itemView.findViewById(R.id.textSubtitle);
            textDateTime = itemView.findViewById(R.id.textDateTime);
            layoutNote = itemView.findViewById(R.id.layoutNote);
            imageNote = itemView.findViewById(R.id.imageNote);
        }

        //: Establece los datos de una Note en las vistas correspondientes
        void setNote(Note note) {
            textTitle.setText(note.getTitle());
            if (note.getSubtitle().trim().isEmpty()) {
                textSubtitle.setVisibility(View.GONE);
            } else {
                textSubtitle.setText(note.getSubtitle());
            }
            textDateTime.setText(note.getDateTime());

            //Obtener el Fondo del layoutNote
            GradientDrawable gradientDrawable = (GradientDrawable) layoutNote.getBackground();
            //Esto verifica si la nota tiene un color definido, y si no le da el por defecto
            if (note.getColor() != null) {
                gradientDrawable.setColor(Color.parseColor(note.getColor()));
            }else{
                gradientDrawable.setColor(Color.parseColor("#333333"));
            }

            if(note.getImagePath() != null){
                imageNote.setImageBitmap(BitmapFactory.decodeFile(note.getImagePath()));
                imageNote.setVisibility(View.VISIBLE);
            }else{
                imageNote.setVisibility(View.GONE);
            }

        }

    }

    public void searchNotes(final String searchKeyword){
        timer= new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(searchKeyword.trim().isEmpty()){
                    notes = notesSource;
                }else{
                    ArrayList<Note> temp = new ArrayList<>();
                    for(Note note : notesSource){
                        if(note.getTitle().toLowerCase().contains(searchKeyword.toLowerCase())
                                || note.getSubtitle().toLowerCase().contains(searchKeyword.toLowerCase())
                        || note.getNoteText().toLowerCase().contains(searchKeyword.toLowerCase())){
                            temp.add(note);
                        }
                    }
                    notes = temp;
                }
                new Handler(Looper.getMainLooper()).post(() -> notifyDataSetChanged());

            }
        }, 500);

    }
    public void calcelTimer(){
        if(timer != null){
            timer.cancel();
        }
    }

}

package es.studium.notesapp.activities;

import static es.studium.notesapp.activities.CreateNoteActivity.REQUEST_CODE_STORAGE_PERMISSION;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import es.studium.notesapp.R;
import es.studium.notesapp.adapters.NotesAdapter;
import es.studium.notesapp.database.NotesDatabase;
import es.studium.notesapp.entities.Note;
import es.studium.notesapp.listeners.NotesListener;

public class MainActivity extends AppCompatActivity implements NotesListener {

    private ActivityResultLauncher<Intent> addNoteActivityResultLauncher;
    private ActivityResultLauncher<Intent> updateNoteActivityResultLauncher;
    private CreateNoteActivity createNoteActivityInstance;

    //registerForActivityResult se usa para lanzar CreateNoteActivity y manejar el resultado cuando la actividad regresa.
    //Sirve para iniciar la actividad y manejar el resultado.
    private ExecutorService executorService;
    private Handler mainThreadHandler;

    private RecyclerView notesRecyclerView; //Mismo nombre que en el xml del activity main
    private List<Note> noteList;
    private NotesAdapter notesAdapter;
    private int noteClickedPosition = -1;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializa el ExecutorService y el Handler
        executorService = Executors.newSingleThreadExecutor();
        mainThreadHandler = new Handler(Looper.getMainLooper());

        ImageView imageAddNoteMain = findViewById(R.id.imageAddNoteMain);
        imageAddNoteMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                addNoteActivityResultLauncher.launch(intent);//Utiliza el lanzador registrado para iniciar CreateNoteActivity.
            }
        });
        //Registra el lanzador con un contrato que especifica el tipo de resultado esperado
        //Dentro del lambda de registerForActivityResult, maneja el resultado devuelto por la actividad iniciada.
        //Este es el método recomendado y moderno para manejar la comunicación entre actividades en Android,
        // proporcionando una manera más segura y robusta de gestionar resultados de actividades.
        // addNoteActi... para manejar el resultado de CreateNoteActivity.
        addNoteActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),//
                result -> { //Expresion lambda
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        Note note = (Note) data.getSerializableExtra("note");
                        noteList.add(0, note);
                        notesAdapter.notifyItemInserted(0);
                        notesRecyclerView.smoothScrollToPosition(0);
                    }
                }
        );
        updateNoteActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        if (data.getBooleanExtra("isNoteUpdated", false)) {
                            Note note = (Note) data.getSerializableExtra("note");
                            noteList.set(noteClickedPosition, note);
                            notesAdapter.notifyItemChanged(noteClickedPosition);
                        } else if (data.getBooleanExtra("isNoteDeleted", false)) {
                            noteList.remove(noteClickedPosition);
                            notesAdapter.notifyItemRemoved(noteClickedPosition);
                        }else{
                            noteList.add(noteClickedPosition, noteList.get(noteClickedPosition));
                            notesAdapter.notifyItemChanged(noteClickedPosition);
                        }
                    }
                }
        );

        notesRecyclerView = findViewById(R.id.notesRecyclerView);
        notesRecyclerView.setLayoutManager(
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        );

        noteList = new ArrayList<>();
        notesAdapter = new NotesAdapter(noteList, this);
        notesRecyclerView.setAdapter(notesAdapter);

        getNotes();

        EditText inputSearch = findViewById(R.id.inputSearch);
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                notesAdapter.calcelTimer();
            }

            @Override
            public void afterTextChanged(Editable s) {
            if(noteList.size()!=0){
                notesAdapter.searchNotes(s.toString()); //La Consulta
            }
            }
        });

        findViewById(R.id.imageAddNote).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                addNoteActivityResultLauncher.launch(intent);//Utiliza el lanzador registrado para iniciar CreateNoteActivity.
            }
        });

        findViewById(R.id.imageAddImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (!Environment.isExternalStorageManager()) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.addCategory("android.intent.category.DEFAULT");
                        intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                        startActivity(intent);
                    } else {
                        selectImage();
                    }
                }else {
                    if (ContextCompat.checkSelfPermission(
                            getApplicationContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                                REQUEST_CODE_STORAGE_PERMISSION);
                    } else {
                        selectImage();
                    }
                }
            }
        });

        findViewById(R.id.imageLanguage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLanguageDialog();
            }
        });
    }
    private final ActivityResultLauncher<Intent> selectImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        String selectedImagePath = getPathFromUri(this, selectedImageUri);
                        Intent intent = new Intent(MainActivity.this, CreateNoteActivity.class);
                        intent.putExtra("isFromQuickActions", true);
                        intent.putExtra("quickActionType", "image");
                        intent.putExtra("imagePath", selectedImagePath);
                        startActivity(intent);
                    }
                }
            }
    );

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        selectImageLauncher.launch(Intent.createChooser(intent, "Selecciona una imagen"));
    }
    
    
    private String getPathFromUri(Context context, Uri contentUri){
        String filePath;
        Cursor cursor = context.getContentResolver().query(contentUri, null, null, null, null );
        if(cursor == null){
            filePath = contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }
        return filePath;
    }

    @Override
    public void onNoteClicked(Note note, int position) {
        noteClickedPosition = position;
        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
        intent.putExtra("isViewOrUpdate", true);
        intent.putExtra("note", note);
        updateNoteActivityResultLauncher.launch(intent);

    }

    @SuppressLint("NotifyDataSetChanged")
    private void getNotes() {
        executorService.execute(() -> {
            // Consulta en segundo plano
            List<Note> notes = NotesDatabase.getNotesDatabase(getApplicationContext()).noteDao().getAllNotes();
            mainThreadHandler.post(() -> {
                noteList.clear();
                noteList.addAll(notes);
                notesAdapter.notifyDataSetChanged();
            });
        });
    }

    private void showLanguageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.layout_dialog_language_selection, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialogView.findViewById(R.id.textSpanish).setOnClickListener(v -> {
            changeLanguage("Español");
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.textEnglish).setOnClickListener(v -> {
            changeLanguage("English");
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.textFrench).setOnClickListener(v -> {
            changeLanguage("Français");
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.textCancel).setOnClickListener(v -> dialog.dismiss());
    }

    private void changeLanguage(String language) {
        // Aquí puedes implementar la lógica para cambiar el idioma
        Locale locale;
        switch (language) {
            case "Español":
                locale = new Locale("es");
                break;
            case "English":
                locale = Locale.ENGLISH;
                break;
            case "Français":
                locale = new Locale("fr");
                break;
            default:
                locale = Locale.getDefault();
                break;
        }
        Locale.setDefault(locale);
        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        // Recargar la actividad para aplicar los cambios de idioma
        Intent refresh = new Intent(this, MainActivity.class);
        startActivity(refresh);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Apaga el ExecutorService cuando ya no sea necesario
        executorService.shutdown();
    }

}
package com.example.notesapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class EditNoteActivity extends AppCompatActivity {

    private EditText editTitle, editContent;
    private Button saveButton;
    private String noteId;
    private FirebaseAuth mAuth;
    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        // Inisialisasi komponen
        editTitle = findViewById(R.id.edit_title);
        editContent = findViewById(R.id.edit_content);
        saveButton = findViewById(R.id.save_button);

        // Inisialisasi Firebase
        mAuth = FirebaseAuth.getInstance();
        String userId = mAuth.getCurrentUser().getUid();
        database = FirebaseDatabase.getInstance().getReference("users").child(userId).child("notes");

        // Ambil data dari Intent
        noteId = getIntent().getStringExtra("note_id");
        String title = getIntent().getStringExtra("title");
        String content = getIntent().getStringExtra("content");

        // Set data ke EditText
        editTitle.setText(title);
        editContent.setText(content);

        // Tombol Simpan
        saveButton.setOnClickListener(v -> {
            String newTitle = editTitle.getText().toString().trim();
            String newContent = editContent.getText().toString().trim();

            if (newTitle.isEmpty() && newContent.isEmpty()) {
                Toast.makeText(this, "Note cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update catatan di Firebase
            HomeFragment.Note updatedNote = new HomeFragment.Note(newTitle, newContent, System.currentTimeMillis());
            updatedNote.setPinned(getIntent().getBooleanExtra("is_pinned", false)); // Pertahankan status pinned
            database.child(noteId).setValue(updatedNote).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Note updated", Toast.LENGTH_SHORT).show();
                    finish(); // Kembali ke HomeFragment
                } else {
                    Toast.makeText(this, "Failed to update note", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
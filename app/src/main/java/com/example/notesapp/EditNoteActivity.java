package com.example.notesapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class EditNoteActivity extends AppCompatActivity {

    private TextInputEditText titleEditText, contentEditText;
    private MaterialButton saveButton;
    private TextView doneButton;
    private Toolbar toolbar;
    private String noteId;
    private FirebaseAuth mAuth;
    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        // Inisialisasi komponen
        toolbar = findViewById(R.id.toolbar);
        titleEditText = findViewById(R.id.title_edit_text);
        contentEditText = findViewById(R.id.content_edit_text);
        saveButton = findViewById(R.id.save_button);
        doneButton = findViewById(R.id.done_button);

        // Setup Toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Inisialisasi Firebase
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Pengguna tidak terautentikasi", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();
        database = FirebaseDatabase.getInstance().getReference("users").child(userId).child("notes");

        // Ambil data dari Intent
        noteId = getIntent().getStringExtra("note_id");
        String title = getIntent().getStringExtra("title");
        String content = getIntent().getStringExtra("content");
        boolean isPinned = getIntent().getBooleanExtra("is_pinned", false);

        // Set data ke EditText
        titleEditText.setText(title);
        contentEditText.setText(content);

        // Tombol Done
        doneButton.setOnClickListener(v -> saveNote());

        // Tombol Simpan
        saveButton.setOnClickListener(v -> {
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                saveNote();
            }).start();
        });
    }

    private void saveNote() {
        String newTitle = titleEditText.getText().toString().trim();
        String newContent = contentEditText.getText().toString().trim();

        // Validasi
        if (newTitle.isEmpty() || newContent.isEmpty()) {
            Toast.makeText(this, "Harap isi judul dan catatan", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newTitle.length() > 100) {
            Toast.makeText(this, "Judul maksimal 100 karakter", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update catatan di Firebase
        Note updatedNote = new Note(newTitle, newContent, System.currentTimeMillis());
        updatedNote.isPinned = getIntent().getBooleanExtra("is_pinned", false);

        database.child(noteId).setValue(updatedNote).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("EditNoteActivity", "Note updated: " + noteId);
                Toast.makeText(this, "Catatan diperbarui", Toast.LENGTH_SHORT).show();
                finish(); // Kembali ke HomeFragment
            } else {
                Log.e("EditNoteActivity", "Failed to update note: " + task.getException().getMessage());
                Toast.makeText(this, "Gagal memperbarui: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish(); // Kembali saat tombol back di toolbar diklik
        return true;
    }

    public static class Note {
        public String title;
        public String content;
        public long timestamp;
        public boolean isPinned;

        public Note() {
            // Diperlukan untuk Firebase
        }

        public Note(String title, String content, long timestamp) {
            this.title = title;
            this.content = content;
            this.timestamp = timestamp;
            this.isPinned = false;
        }
    }
}
package com.example.notesapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AddNoteFragment extends Fragment {

    private TextInputEditText titleEditText, contentEditText;
    private TextView doneButton;
    private FirebaseAuth mAuth;
    private DatabaseReference database;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_note, container, false);

        // Inisialisasi komponen
        titleEditText = view.findViewById(R.id.title_edit_text);
        contentEditText = view.findViewById(R.id.content_edit_text);
        doneButton = view.findViewById(R.id.done_button);

        // Inisialisasi Firebase
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Pengguna tidak terautentikasi", Toast.LENGTH_SHORT).show();
            if (getActivity() != null) {
                startActivity(new Intent(getActivity(), LoginActivity.class));
                getActivity().finish();
            }
            return view;
        }
        String userId = mAuth.getCurrentUser().getUid();
        database = FirebaseDatabase.getInstance().getReference("users").child(userId).child("notes");

        // Aksi tombol Done
        doneButton.setOnClickListener(v -> {
            String title = titleEditText.getText().toString().trim();
            String content = contentEditText.getText().toString().trim();

            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(getContext(), "Harap isi judul dan catatan", Toast.LENGTH_SHORT).show();
                return;
            }

            if (title.length() > 100) {
                Toast.makeText(getContext(), "Judul maksimal 100 karakter", Toast.LENGTH_SHORT).show();
                return;
            }

            // Buat objek catatan
            Note note = new Note(title, content, System.currentTimeMillis());
            note.isPinned = false; // Inisialisasi isPinned

            // Simpan ke Firebase
            String noteId = database.push().getKey();
            database.child(noteId).setValue(note).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(getContext(), "Catatan berhasil disimpan", Toast.LENGTH_SHORT).show();
                    // Kosongkan field
                    titleEditText.setText("");
                    contentEditText.setText("");
                    // Kembali ke HomeFragment
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new HomeFragment())
                            .commit();
                } else {
                    Toast.makeText(getContext(), "Gagal menyimpan: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });

        return view;
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
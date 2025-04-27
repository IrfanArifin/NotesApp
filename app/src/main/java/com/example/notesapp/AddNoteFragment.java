package com.example.notesapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
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
import android.graphics.Typeface;
import android.util.TypedValue;

public class AddNoteFragment extends Fragment {

    private TextInputEditText noteEditText;
    private TextView doneButton;
    private FirebaseAuth mAuth;
    private DatabaseReference database;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_note, container, false);

        // Inisialisasi komponen
        noteEditText = view.findViewById(R.id.title_edit_text);
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
        Log.d("AddNoteFragment", "Saving notes to: /users/" + userId + "/notes");

        // Tambahkan TextWatcher untuk mengatur ukuran teks secara dinamis
        noteEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Tidak perlu
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Tidak perlu
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Hapus span sebelumnya untuk menghindari konflik
                noteEditText.removeTextChangedListener(this);

                String text = s.toString();
                SpannableString spannableString = new SpannableString(text);

                // Pisahkan teks menjadi baris
                String[] lines = text.split("\n", 2);
                int titleEndIndex = lines[0].length();

                // Atur ukuran dan gaya untuk judul (baris pertama)
                if (titleEndIndex > 0) {
                    // Ukuran teks judul: 20sp
                    spannableString.setSpan(
                            new AbsoluteSizeSpan((int) TypedValue.applyDimension(
                                    TypedValue.COMPLEX_UNIT_SP, 20, getResources().getDisplayMetrics())),
                            0, titleEndIndex, 0
                    );
                    // Gaya tebal untuk judul
                    spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0, titleEndIndex, 0);
                }

                // Atur ukuran untuk isi (sisa teks)
                if (lines.length > 1) {
                    int contentStartIndex = titleEndIndex + 1; // +1 untuk karakter newline
                    int contentEndIndex = text.length();
                    // Ukuran teks isi: 16sp
                    spannableString.setSpan(
                            new AbsoluteSizeSpan((int) TypedValue.applyDimension(
                                    TypedValue.COMPLEX_UNIT_SP, 16, getResources().getDisplayMetrics())),
                            contentStartIndex, contentEndIndex, 0
                    );
                }

                // Terapkan SpannableString ke EditText
                noteEditText.setText(spannableString);
                noteEditText.setSelection(text.length()); // Kembalikan kursor ke akhir

                noteEditText.addTextChangedListener(this);
            }
        });

        // Aksi tombol Done
        doneButton.setOnClickListener(v -> saveNote());

        return view;
    }

    private void saveNote() {
        String noteText = noteEditText.getText().toString().trim();

        // Validasi
        if (noteText.isEmpty()) {
            Toast.makeText(getContext(), "Catatan tidak boleh kosong", Toast.LENGTH_SHORT).show();
            return;
        }

        // Pisahkan judul dan isi berdasarkan baris
        String[] lines = noteText.split("\n", 2);
        String title = lines[0].trim();
        String content = lines.length > 1 ? lines[1].trim() : "";

        if (title.isEmpty()) {
            Toast.makeText(getContext(), "Judul tidak boleh kosong", Toast.LENGTH_SHORT).show();
            return;
        }

        // Buat objek catatan
        Note note = new Note(title, content, System.currentTimeMillis());

        // Simpan ke Firebase
        String noteId = database.push().getKey();
        Log.d("AddNoteFragment", "Note ID: " + noteId);
        database.child(noteId).setValue(note).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Catatan berhasil disimpan", Toast.LENGTH_SHORT).show();
                // Kosongkan field
                noteEditText.setText("");
                // Kembali ke HomeFragment
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit();
            } else {
                Log.e("AddNoteFragment", "Error saving note: " + task.getException().getMessage());
                Toast.makeText(getContext(), "Gagal menyimpan: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public static class Note {
        public String title;
        public String content;
        public long timestamp;

        public Note() {
            // Diperlukan untuk Firebase
        }

        public Note(String title, String content, long timestamp) {
            this.title = title;
            this.content = content;
            this.timestamp = timestamp;
        }
    }
}
package com.example.notesapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private TextView nameTextView, emailTextView, totalNotesTextView, joinedDateTextView, lastNoteTextView;
    private ImageView profileImage;
    private Button logoutButton;
    private FirebaseAuth mAuth;
    private DatabaseReference database;
    private StorageReference storageReference;
    private Uri photoUri;

    private static final int STORAGE_PERMISSION_CODE = 100;

    // Launcher untuk memilih foto dari galeri
    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    photoUri = uri;
                    uploadPhotoToFirebase();
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Inisialisasi komponen UI
        nameTextView = view.findViewById(R.id.name_text_view);
        emailTextView = view.findViewById(R.id.email_text_view);
        totalNotesTextView = view.findViewById(R.id.total_notes_text_view);
        joinedDateTextView = view.findViewById(R.id.joined_date_text_view);
        lastNoteTextView = view.findViewById(R.id.last_note_text_view);
        profileImage = view.findViewById(R.id.profile_image);
        logoutButton = view.findViewById(R.id.logout_button);

        // Tambahkan animasi fade-in
        view.setAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in));

        // Inisialisasi Firebase
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return view;
        }

        String userId = currentUser.getUid();
        database = FirebaseDatabase.getInstance().getReference("users").child(userId).child("notes");
        storageReference = FirebaseStorage.getInstance().getReference("profile_photos").child(userId + ".jpg");

        // Tampilkan informasi pengguna
        displayUserInfo(currentUser);

        // Hitung jumlah catatan dan catatan terakhir
        loadNotesData();

        // Setup listener untuk foto profil
        profileImage.setOnClickListener(v -> showProfilePhotoOptions());

        // Setup listener untuk tombol logout
        logoutButton.setOnClickListener(v -> logout());

        return view;
    }

    private void displayUserInfo(FirebaseUser user) {
        // Tampilkan nama
        String name = user.getDisplayName();
        if (name != null && !name.isEmpty()) {
            nameTextView.setText(name);
        } else {
            nameTextView.setText("Anonymous User");
        }

        // Tampilkan email
        String email = user.getEmail();
        if (email != null && !email.isEmpty()) {
            emailTextView.setText(email);
        }

        // Tampilkan tanggal bergabung
        long creationTimestamp = user.getMetadata().getCreationTimestamp();
        if (creationTimestamp != 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            String joinedDate = sdf.format(new Date(creationTimestamp));
            joinedDateTextView.setText(joinedDate);
        } else {
            joinedDateTextView.setText("N/A");
        }

        // Tampilkan foto profil
        if (user.getPhotoUrl() != null && getContext() != null) {
            Glide.with(getContext())
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .placeholder(android.R.drawable.ic_menu_info_details)
                    .error(android.R.drawable.ic_menu_info_details)
                    .into(profileImage);
        } else {
            profileImage.setImageResource(android.R.drawable.ic_menu_info_details);
        }
    }

    private void loadNotesData() {
        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Hitung jumlah catatan
                long totalNotes = snapshot.getChildrenCount();
                totalNotesTextView.setText(String.valueOf(totalNotes));

                // Cari catatan terakhir
                List<AddNoteFragment.Note> notes = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    AddNoteFragment.Note note = dataSnapshot.getValue(AddNoteFragment.Note.class);
                    if (note != null) {
                        notes.add(note);
                    }
                }

                if (!notes.isEmpty()) {
                    // Urutkan berdasarkan timestamp (terbaru dulu)
                    Collections.sort(notes, (n1, n2) -> Long.compare(n2.timestamp, n1.timestamp));
                    AddNoteFragment.Note lastNote = notes.get(0);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                    String lastNoteText = lastNote.title + "\n" + sdf.format(new Date(lastNote.timestamp));
                    lastNoteTextView.setText(lastNoteText);
                } else {
                    lastNoteTextView.setText("N/A");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                totalNotesTextView.setText("Error");
                lastNoteTextView.setText("Error");
            }
        });
    }

    private void showProfilePhotoOptions() {
        String[] options = {"Update Photo", "Remove Photo"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Profile Photo")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Update Photo
                            if (checkStoragePermission()) {
                                pickImageLauncher.launch("image/*");
                            } else {
                                requestStoragePermission();
                            }
                            break;
                        case 1: // Remove Photo
                            deletePhotoFromFirebase();
                            break;
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(
                requireActivity(),
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                STORAGE_PERMISSION_CODE
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImageLauncher.launch("image/*");
            } else {
                Toast.makeText(getContext(), "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadPhotoToFirebase() {
        if (photoUri == null) return;

        // Unggah ke Firebase Storage
        storageReference.putFile(photoUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Dapatkan URL foto yang diunggah
                    storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        // Perbarui URL foto di Firebase Auth
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setPhotoUri(uri)
                                    .build();
                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            // Perbarui tampilan foto
                                            if (getContext() != null) {
                                                Glide.with(getContext())
                                                        .load(uri)
                                                        .circleCrop()
                                                        .placeholder(android.R.drawable.ic_menu_info_details)
                                                        .error(android.R.drawable.ic_menu_info_details)
                                                        .into(profileImage);
                                            }
                                            Toast.makeText(getContext(), "Photo uploaded successfully", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to upload photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deletePhotoFromFirebase() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        // Hapus foto dari Firebase Storage
        storageReference.delete()
                .addOnSuccessListener(aVoid -> {
                    // Perbarui URL foto di Firebase Auth menjadi null
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setPhotoUri(null)
                            .build();
                    user.updateProfile(profileUpdates)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    // Kembali ke placeholder
                                    profileImage.setImageResource(android.R.drawable.ic_menu_info_details);
                                    Toast.makeText(getContext(), "Photo deleted successfully", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to delete photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void logout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    mAuth.signOut();
                    Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
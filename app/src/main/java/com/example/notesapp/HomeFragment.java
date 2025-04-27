package com.example.notesapp;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.appcompat.widget.SearchView;
import android.graphics.Typeface;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private SearchView searchView;
    private NoteAdapter noteAdapter;
    private List<Note> noteList;
    private List<Note> filteredNoteList;
    private FirebaseAuth mAuth;
    private DatabaseReference database;

    // Konstanta untuk tipe item
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    // Palet warna
    private final int[] cardColors = new int[] {
            0xFF6A9C78, // Hijau
            0xFF5A92C1, // Biru
            0xFFD1A985, // Kuning
            0xFFB5765A  // Oranye
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Inisialisasi komponen
        recyclerView = view.findViewById(R.id.recycler_view);
        searchView = view.findViewById(R.id.search_view);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        noteList = new ArrayList<>();
        filteredNoteList = new ArrayList<>();
        noteAdapter = new NoteAdapter(filteredNoteList);
        recyclerView.setAdapter(noteAdapter);

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

        // Ambil data dari Firebase
        loadNotes();

        // Setup SearchView
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterNotes(newText);
                return true;
            }
        });

        // Setup ItemTouchHelper untuk swipe-to-delete
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeToDeleteCallback());
        itemTouchHelper.attachToRecyclerView(recyclerView);

        return view;
    }

    private void loadNotes() {
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                noteList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Note note = dataSnapshot.getValue(Note.class);
                    if (note != null) {
                        note.setId(dataSnapshot.getKey());
                        // Pastikan isPinned diinisialisasi
                        if (note.getTimestamp() != 0) { // Bukan header
                            noteList.add(note);
                        }
                    }
                }
                filterNotes(searchView.getQuery().toString());
                Log.d("HomeFragment", "Loaded " + noteList.size() + " notes");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Gagal memuat catatan: " + error.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("HomeFragment", "Error loading notes: " + error.getMessage());
            }
        });
    }

    private void filterNotes(String query) {
        filteredNoteList.clear();
        if (query.isEmpty()) {
            filteredNoteList.addAll(noteList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Note note : noteList) {
                if (note.getTitle().toLowerCase().contains(lowerQuery) ||
                        note.getContent().toLowerCase().contains(lowerQuery)) {
                    filteredNoteList.add(note);
                }
            }
        }

        // Jika tidak ada catatan, perbarui adapter
        if (filteredNoteList.isEmpty()) {
            noteAdapter.notifyDataSetChanged();
            return;
        }

        // Pisahkan catatan menjadi pinned dan unpinned
        List<Note> pinnedNotes = new ArrayList<>();
        List<Note> unpinnedNotes = new ArrayList<>();
        for (Note note : filteredNoteList) {
            if (note.isPinned()) {
                pinnedNotes.add(note);
            } else {
                unpinnedNotes.add(note);
            }
        }

        // Tambahkan header dan catatan ke daftar
        filteredNoteList.clear();
        if (!pinnedNotes.isEmpty()) {
            filteredNoteList.add(new Note("Pinned", "", 0)); // Header Pinned
            filteredNoteList.addAll(pinnedNotes);
        }
        if (!unpinnedNotes.isEmpty()) {
            filteredNoteList.add(new Note("Notes", "", 0)); // Header Notes
            filteredNoteList.addAll(unpinnedNotes);
        }

        noteAdapter.notifyDataSetChanged();
    }

    private class NoteAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final List<Note> notes;

        public NoteAdapter(List<Note> notes) {
            this.notes = notes;
        }

        @Override
        public int getItemViewType(int position) {
            return notes.get(position).getTimestamp() == 0 ? TYPE_HEADER : TYPE_ITEM;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(android.R.layout.simple_list_item_1, parent, false);
                return new HeaderViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_note, parent, false);
                return new NoteViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (getItemViewType(position) == TYPE_HEADER) {
                HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
                headerHolder.headerText.setText(notes.get(position).getTitle());
            } else {
                NoteViewHolder noteHolder = (NoteViewHolder) holder;
                Note note = notes.get(position);
                noteHolder.titleTextView.setText(note.getTitle());
                noteHolder.contentTextView.setText(note.getContent());

                // Atur warna latar belakang dari palet warna
                int colorIndex = position % cardColors.length;
                noteHolder.noteCard.setCardBackgroundColor(cardColors[colorIndex]);

                // Tambahkan animasi fade-in
                noteHolder.itemView.setAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in));

                // Klik item untuk mengedit
                noteHolder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(getContext(), EditNoteActivity.class);
                    intent.putExtra("note_id", note.getId());
                    intent.putExtra("title", note.getTitle());
                    intent.putExtra("content", note.getContent());
                    intent.putExtra("is_pinned", note.isPinned());
                    startActivity(intent);
                });
            }
        }

        @Override
        public int getItemCount() {
            return notes.size();
        }
    }

    private class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {
        private final Drawable deleteIcon;
        private final GradientDrawable background;
        private final int deleteButtonWidth;

        public SwipeToDeleteCallback() {
            super(0, ItemTouchHelper.LEFT);
            deleteIcon = ContextCompat.getDrawable(getContext(), android.R.drawable.ic_menu_delete);
            deleteIcon.setTint(Color.WHITE);

            background = new GradientDrawable();
            background.setColor(Color.parseColor("#FF4444"));
            background.setCornerRadii(new float[] {0, 0, 24, 24, 24, 24, 0, 0});

            deleteButtonWidth = (int) (80 * getResources().getDisplayMetrics().density);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            if (getItemViewType(position) != TYPE_ITEM) {
                noteAdapter.notifyItemChanged(position);
                return;
            }

            Note note = filteredNoteList.get(position);
            String noteId = note.getId();

            database.child(noteId).removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    noteList.removeIf(n -> n.getId().equals(noteId));
                    filterNotes(searchView.getQuery().toString());
                    Toast.makeText(getContext(), "Catatan dihapus", Toast.LENGTH_SHORT).show();
                } else {
                    noteAdapter.notifyItemChanged(position);
                    Toast.makeText(getContext(), "Gagal menghapus: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

            if (getItemViewType(viewHolder.getAdapterPosition()) != TYPE_ITEM) {
                return;
            }

            View itemView = viewHolder.itemView;

            int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
            int iconTop = itemView.getTop() + (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
            int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();

            if (dX < 0) {
                int backgroundLeft = itemView.getRight() - deleteButtonWidth;
                int backgroundRight = itemView.getRight();
                background.setBounds(backgroundLeft, itemView.getTop(), backgroundRight, itemView.getBottom());

                int iconLeft = backgroundLeft + (deleteButtonWidth - deleteIcon.getIntrinsicWidth()) / 2;
                int iconRight = iconLeft + deleteIcon.getIntrinsicWidth();
                deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

                background.draw(c);
                deleteIcon.draw(c);
            } else {
                background.setBounds(0, 0, 0, 0);
            }
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            if (getItemViewType(viewHolder.getAdapterPosition()) == TYPE_HEADER) {
                return 0;
            }
            return super.getMovementFlags(recyclerView, viewHolder);
        }

        private int getItemViewType(int position) {
            return filteredNoteList.get(position).getTimestamp() == 0 ? TYPE_HEADER : TYPE_ITEM;
        }
    }

    private class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView headerText;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            headerText = itemView.findViewById(android.R.id.text1);
            headerText.setTextSize(18);
            headerText.setTextColor(ContextCompat.getColor(getContext(), android.R.color.black));
            headerText.setTypeface(null, Typeface.BOLD);
            headerText.setPadding(16, 16, 16, 8);
        }
    }

    private static class NoteViewHolder extends RecyclerView.ViewHolder {
        androidx.cardview.widget.CardView noteCard;
        TextView titleTextView, contentTextView;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            noteCard = itemView.findViewById(R.id.note_card);
            titleTextView = itemView.findViewById(R.id.title_text_view);
            contentTextView = itemView.findViewById(R.id.content_text_view);
        }
    }

    public static class Note {
        private String id;
        private String title;
        private String content;
        private long timestamp;
        private boolean isPinned;

        public Note() {
            // Diperlukan untuk Firebase
        }

        public Note(String title, String content, long timestamp) {
            this.title = title;
            this.content = content;
            this.timestamp = timestamp;
            this.isPinned = false;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public boolean isPinned() {
            return isPinned;
        }

        public void setPinned(boolean pinned) {
            isPinned = pinned;
        }
    }
}
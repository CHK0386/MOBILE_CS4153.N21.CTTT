package com.example.bookapp.activities;

import android.app.DownloadManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bookapp.MyApplication;
import com.example.bookapp.databinding.ActivityPdfDetailBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PdfDetailActivity extends AppCompatActivity {

    // View binding
    private ActivityPdfDetailBinding binding;

    // PDF id, get from intent
    String bookId, bookTitle, bookUrl;

    private static final String TAG_DOWNLOAD = "DOWNLOAD_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get data from intent e.g., bookId
        Intent intent = getIntent();
        bookId = intent.getStringExtra("bookId");

        // At start, hide the download button, as we need the book URL that we will load later in the function loadBookDetails()
        binding.downloadBookBtn.setVisibility(View.GONE);

        loadBookDetails();
        // Increment book view count whenever this page starts
        MyApplication.incrementBookViewCount(bookId);

        // Handle click, go back
        binding.backBtn.setOnClickListener(view -> onBackPressed());

        // Handle click, open to view PDF
        binding.readBookBtn.setOnClickListener(view -> {
            Intent intent1 = new Intent(PdfDetailActivity.this, PdfViewActivity.class);
            intent1.putExtra("bookId", bookId);
            startActivity(intent1);
        });

        // Handle click, download PDF
        binding.downloadBookBtn.setOnClickListener(view -> {
            downloadBook();
        });
    }

    private void downloadBook() {
        Uri uri = Uri.parse(bookUrl);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir("Download", bookTitle + ".pdf");

        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        if (downloadManager != null) {
            downloadManager.enqueue(request);
            Log.d(TAG_DOWNLOAD, "downloadBook: Permission already granted,can download book");
            Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG_DOWNLOAD, "downloadBook: Permission was not granted,request permission...");
            Toast.makeText(this, "Failed to start download", Toast.LENGTH_SHORT).show();
        }
    }


    private void loadBookDetails() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Get data
                bookTitle = "" + snapshot.child("title").getValue();
                String description = "" + snapshot.child("description").getValue();
                String categoryId = "" + snapshot.child("categoryId").getValue();
                String viewCount = "" + snapshot.child("viewCount").getValue();
                String downloadCount = "" + snapshot.child("downloadCount").getValue();
                bookUrl = "" + snapshot.child("url").getValue();
                String timestamp = "" + snapshot.child("timestamp").getValue();

                // Required data is loaded, show the download button
                binding.downloadBookBtn.setVisibility(View.VISIBLE);

                // Format date
                String date = MyApplication.formatTimestamp(Long.parseLong(timestamp));

                MyApplication.loadCategory("" + categoryId, binding.categoryTv);
                MyApplication.loadPdfFromUrlSinglePage("" + bookUrl, "" + bookTitle, binding.pdfView, binding.progressBar);
                MyApplication.loadPdfSize("" + bookUrl, "" + bookTitle, binding.sizeTv);

                // Set data
                binding.titleTv.setText(bookTitle);
                binding.descriptionTv.setText(description);
                binding.viewsTv.setText(viewCount.replace("null", "N/A"));
                binding.downloadsTv.setText(downloadCount.replace("null", "N/A"));
                binding.dateTv.setText(date);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}

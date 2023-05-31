package com.example.bookapp;

import static com.example.bookapp.Constants.MAX_BYTES_PDF;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

//application class runs before launcher activity
public class MyApplication extends Application {

    private static final String TAG_DOWNLOAD = "DOWNLOAD_TAG";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    //created a static method to convert timestamp to proper date format => can use all project, no need to rewrite
    public static final String formatTimestamp(long timestamp){
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(timestamp);
        //format timestamp to dd/mm/yyyy
        String date = DateFormat.format("dd/mm/yyy", cal).toString();

        return date;
    }

    public static void deleteBook(Context context, String bookId, String bookUrl, String bookTitle) {
        String TAG = "DELETE_BOOK_TAG";


        Log.d(TAG, "deleteBook: Deleting...");
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Please wait...");
        builder.setMessage("Deleting" +bookTitle+"..."); //e.g . Deleting Book ABC...
        builder.show();
        AlertDialog alertDialog = builder.create();

        Log.d(TAG, "deleteBook: Deleting from storage...");
        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl);
        storageReference.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: Deleted from storage");

                        Log.d(TAG, "onSuccess: Now deleting info from db");
                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Books");
                        reference.child(bookId)
                                .removeValue()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Log.d(TAG, "onSuccess: Delete from db too");
                                        alertDialog.dismiss();
                                        Toast.makeText(context, "Book Deleted Successfully...", Toast.LENGTH_SHORT).show();

                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d(TAG, "onFailure: Failed to deleted from db due to"+e.getMessage());
                                        alertDialog.dismiss();
                                        Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: Failed to delete from storage due to"+e.getMessage());
                        alertDialog.dismiss();
                        Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });


    }

    public static void loadPdfSize(String pdfUrl, String pdfTitle, TextView sizeTv) {
        String TAG = "PDF_SIZE_TAG";
        //using url => get file and its metadata from firebase storage

        StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl);
        ref.getMetadata()
                .addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                    @Override
                    public void onSuccess(StorageMetadata storageMetadata) {
                        //get size in bytes
                        double bytes = storageMetadata.getSizeBytes();
                        Log.d(TAG, "onSuccess: "+pdfTitle+ " "+bytes);

                        //convert bytes to KB, MB
                        double kb = bytes/1024;
                        double mb = kb/1024;

                        if(mb >= 1){
                            sizeTv.setText(String.format("%.2f", mb)+" MB");
                        }
                        else if(kb >= 1){
                            sizeTv.setText(String.format("%.2f", kb)+" KB");
                        }
                        else {
                            sizeTv.setText(String.format("%.2f", bytes)+" bytes");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed getting metadata
                        Log.d(TAG, "onFailure: "+e.getMessage());
                    }
                });
    }

    public static void loadPdfFromUrlSinglePage(String pdfUrl, String pdfTitle, PDFView pdfView, ProgressBar progressBar) {
        //using url => get file and its metadata from firebase storage
        String TAG = "PDF_LOAD_SINGLE_TAG";


        StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl);
        ref.getBytes(MAX_BYTES_PDF)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Log.d(TAG, "onSuccess: "+pdfTitle+ " successfully got the file");


                        //set to pdfview
                        pdfView.fromBytes(bytes)
                                .pages(0)//show ony first page
                                .spacing(0)
                                .swipeHorizontal(false)
                                .enableSwipe(false)
                                .onError(new OnErrorListener() {
                                    @Override
                                    public void onError(Throwable t) {
                                        //hide progress
                                        progressBar.setVisibility(View.INVISIBLE);
                                        Log.d(TAG, "onError: "+t.getMessage());
                                    }
                                })
                                .onPageError(new OnPageErrorListener() {
                                    @Override
                                    public void onPageError(int page, Throwable t) {
                                        //hide progress
                                        progressBar.setVisibility(View.INVISIBLE);
                                        Log.d(TAG, "onPageError: "+t.getMessage());
                                    }
                                })
                                .onLoad(new OnLoadCompleteListener() {
                                    @Override
                                    public void loadComplete(int nbPages) {
                                        //pdf loaded
                                        //hide progress
                                        progressBar.setVisibility(View.INVISIBLE);
                                        Log.d(TAG, "loadComplete: pdf loaded");
                                    }
                                })
                                .load();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //hide progress
                        progressBar.setVisibility(View.INVISIBLE);
                        Log.d(TAG, "onFailure: failed getting file from url due to "+e.getMessage());
                    }
                });
    }

    public static void loadCategory(String categoryId, TextView categoryTv) {
        //get category using categoryId


        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Categories");
        ref.child(categoryId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get category
                        String category = ""+snapshot.child("category").getValue();

                        //set to category text view
                        categoryTv.setText(category);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    public static void incrementBookViewCount(String bookId){
        //1) Get book view count
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get views count
                        String viewsCount = ""+snapshot.child("viewCount").getValue();
                        //in case of null replace with 0
                        if(viewsCount.equals("") || viewsCount.equals("null")){
                            viewsCount = "0";
                        }


                        //2) increment views count
                        long newViewsCount = Long.parseLong(viewsCount) + 1;

                        HashMap<String , Object> hashMap = new HashMap<>();
                        hashMap.put("viewCount", newViewsCount);

                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Books");
                        reference.child(bookId)
                                .updateChildren(hashMap);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    public static void downloadBook(Context context, String bookId,String bookUrl, String bookTitle){
        Log.d(TAG_DOWNLOAD, "downloadBook: downloading book...");

        String nameWithExtension = bookTitle + ".pdf";
        Log.d(TAG_DOWNLOAD, "downloadBook: NAME"+nameWithExtension );

        //AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Please wait");
        builder.setMessage("Downloading " + nameWithExtension + "...");
        builder.setCancelable(false); // Prevent dialog from being canceled by touching outside
        AlertDialog alertDialog = builder.create();
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
        alertDialog.show();

        //download from firebase storage using url
        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl);
        storageReference.getBytes(MAX_BYTES_PDF)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Log.d(TAG_DOWNLOAD, "onSuccess: Book Downloaded");
                        Log.d(TAG_DOWNLOAD, "onSuccess: Saving Book...");
                        saveDownloadedBook(context,builder,bytes,nameWithExtension,bookId);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG_DOWNLOAD, "onSuccess: Failed to download due to"+e.getMessage());
                        alertDialog.dismiss();
                        Toast.makeText(context, "Failed to download due to"+e.getMessage(), Toast.LENGTH_SHORT).show();


                    }
                });

    }

    private static void saveDownloadedBook(Context context, AlertDialog.Builder builder, byte[] bytes, String nameWithExtension, String bookId) {
        Log.d(TAG_DOWNLOAD, "saveDownloadedBook: Saving downloaded book");
        //AlertDialog
        AlertDialog alertDialog = builder.create();
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
        alertDialog.show();

        try{
            File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            downloadsFolder.mkdirs();

            String filePath = downloadsFolder.getPath();

            FileOutputStream out = new FileOutputStream(filePath);
            out.write(bytes);
            out.close();

            Toast.makeText(context, "Saved to Download Folder", Toast.LENGTH_SHORT).show();
            Log.d(TAG_DOWNLOAD, "saveDownloadedBook: Saved to Download Folder");
            alertDialog.dismiss();

            incrementBookDownloadCount(bookId);

        }
        catch (Exception e){
            Log.d(TAG_DOWNLOAD, "saveDownloadedBook: Failed saving to Download Folder due to"+e.getMessage());
            Toast.makeText(context, "Failed saving to Download Folder due to"+e.getMessage(), Toast.LENGTH_SHORT).show();
            alertDialog.dismiss();
        }
    }

    private static void incrementBookDownloadCount(String bookId) {
        Log.d(TAG_DOWNLOAD, "incrementBookDownloadCount: Incrementing Book Download Count");

        // Step 1: get previous download count
        DatabaseReference ref = FirebaseDatabase.getInstance().getReferenceFromUrl("Books");
        ref.child(bookId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String downloadsCount = "" + snapshot.child("downloadsCount").getValue();
                Log.d(TAG_DOWNLOAD, "onDataChange: Downloads Count: " + downloadsCount);

                if (downloadsCount.equals("") || downloadsCount.equals("null")) {
                    downloadsCount = "0";
                }

                // Convert to long and increment by 1
                long newDownloadsCount = Long.parseLong(downloadsCount) + 1;
                Log.d(TAG_DOWNLOAD, "onDataChange: New Download Count" +newDownloadsCount);

                //set data to update
                HashMap<String,Object> hashMap = new HashMap<>();
                hashMap.put("downloadsCount", newDownloadsCount);

                // Step 2: Update the downloadsCount in the database
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Books");
                reference.child(bookId).updateChildren(hashMap)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.d(TAG_DOWNLOAD, "onSuccess: Downloads Count update ...");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d(TAG_DOWNLOAD, "onFailure: Failed to update Downloads Count due to"+e.getMessage());

                            }
                        });

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle the cancellation if needed
            }
        });
    }

}

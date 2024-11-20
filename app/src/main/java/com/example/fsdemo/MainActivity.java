package com.example.fsdemo;

import static com.example.fsdemo.FBRef.refImageFull;
import static com.example.fsdemo.FBRef.refImageGallery;
import static com.example.fsdemo.FBRef.refImageStamp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firestore.v1.Document;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
/**
 * The Main activity
 * <p>
 * This activity use to demonstrate:
 * - take photos from camera / gallery & upload them to firebase firestore
 * - download images from firebase firestore & display them
 * </p>
 *
 * @author Levy Albert albert.school2015@gmail.com
 * @version 2.0
 * @since 19/11/2024
 */
public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";
    private ImageView iV;
    private String lastStamp, lastFull, lastGallery;
    Bitmap imageBitmap;
    private String currentPath;
    private File localFile;
    DocumentReference refImage;
    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private static final int REQUEST_READ_EXTERNAL_STORAGE_PERMISSION = 102;
    private static final int REQUEST_STAMP_CAPTURE = 201;
    private static final int REQUEST_FULL_IMAGE_CAPTURE = 202;
    private static final int REQUEST_PICK_IMAGE = 301;

    @SuppressLint("WrongThread")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iV = findViewById(R.id.iV);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE_PERMISSION);
        }
    }

    /**
     * onRequestPermissionsResult method
     * <p> Method triggered by other activities returning result of permissions request
     * </p>
     *
     * @param requestCode the request code triggered the activity
     * @param permissions the array of permissions granted
     * @param grantResults the array of permissions granted
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE_PERMISSION) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "Gallery permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * takeStamp method
     * <p> Taking a photo by camera to upload to Firebase Storage
     * </p>
     *
     * @param view the view that triggered the method
     */
    public void takeStamp(View view) {
        Intent takePicIntent = new Intent();
        takePicIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePicIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePicIntent, REQUEST_STAMP_CAPTURE);
        }
    }

    /**
     * takeFull method
     * <p> Taking a full resolution photo by camera to upload to Firebase Storage
     * </p>
     *
     * @param view the view that triggered the method
     */
    public void takeFull(View view) {
        // creating local temporary file to store the full resolution photo
        String filename = "tempfile";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            File imgFile = File.createTempFile(filename,".jpg",storageDir);
            currentPath = imgFile.getAbsolutePath();
            Uri imageUri = FileProvider.getUriForFile(MainActivity.this,"com.example.fsdemo.fileprovider",imgFile);
            Intent takePicIntent = new Intent();
            takePicIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
            takePicIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,imageUri);
            if (takePicIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePicIntent, REQUEST_FULL_IMAGE_CAPTURE);
            }
        } catch (IOException e) {
            Toast.makeText(MainActivity.this,"Failed to create temporary file",Toast.LENGTH_LONG);
            throw new RuntimeException(e);
        }
    }

    /**
     * takeFull method
     * <p> Selecting image file from gallery to upload to Firebase Storage
     * </p>
     *
     * @param view
     */
    public void gallery(View view) {
        Intent si = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(si, REQUEST_PICK_IMAGE);
    }

    /**
     * Uploading selected image file to Firebase Storage
     * <p>
     *
     * @param requestCode   The call sign of the intent that requested the result
     * @param resultCode    A code that symbols the status of the result of the activity
     * @param data_back     The data returned
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data_back) {
        super.onActivityResult(requestCode, resultCode, data_back);
        if (resultCode == Activity.RESULT_OK) {
            Date date = Calendar.getInstance().getTime();
            DateFormat dateFormat = new SimpleDateFormat("yyyyMMddhhmmss");
            switch (requestCode) {
                // Upload camera thumbnail image file
                case REQUEST_STAMP_CAPTURE:
                    Bundle extras = data_back.getExtras();
                    if (extras != null) {
                        lastStamp = dateFormat.format(date);
                        imageBitmap = (Bitmap) extras.get("data");
                        addImage(imageBitmap, refImageStamp, lastStamp);
                    }
                    break;
                // Upload camera full resolution image file
                case REQUEST_FULL_IMAGE_CAPTURE:
                    lastFull = dateFormat.format(date);
                    imageBitmap = BitmapFactory.decodeFile(currentPath);
                    addImage(imageBitmap, refImageFull, lastFull);
                    break;
                // Upload gallery image file
                case REQUEST_PICK_IMAGE:
                    Uri file = data_back.getData();
                    if (file != null) {
                        lastGallery = dateFormat.format(date);
                        try {
                            imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), file);
                            addImage(imageBitmap, refImageGallery, lastGallery);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        Toast.makeText(this, "No Image was selected", Toast.LENGTH_LONG).show();
                    }
                    break;
            }
        }
    }

    /**
     * Downloading selected image file from Firebase FireStore
     * <p>
     *
     * @param view
     */
    public void readImage(View view) {
        int id = view.getId();
        if (id == R.id.readStamp) {
            refImage = refImageStamp.document(lastStamp);
            try {
                localFile = File.createTempFile(lastStamp,"png");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (id == R.id.readFull) {
            refImage = refImageFull.document(lastFull);
            try {
                localFile = File.createTempFile(lastFull,"jpg");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (id == R.id.readGallery) {
            refImage = refImageGallery.document(lastGallery);
            try {
                localFile = File.createTempFile(lastGallery,"jpg");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
//        // Download the image file and  display it
        final ProgressDialog pd=ProgressDialog.show(this,"Image download","downloading...",true);
        refImage.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    Blob blob = (Blob) documentSnapshot.get("imageData");
                    byte[] bytes = blob.toBytes();
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    iV.setImageBitmap(bitmap);
                } else {
                    Log.d(TAG, "No such document");
                }
                pd.dismiss();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                pd.dismiss();
                Toast.makeText(MainActivity.this, "Image download failed", Toast.LENGTH_LONG).show();
                Log.d(TAG, "get failed with ", e);
            }
        });
    }

    public void addImage(Bitmap image, CollectionReference ref, String name) {
        ProgressDialog pd;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG,100,baos);
        byte[] imageBytes = baos.toByteArray();
        Toast.makeText(this, ""+imageBytes.length, Toast.LENGTH_SHORT).show();
        if (imageBytes.length > 1040000) {
            pd=ProgressDialog.show(this,"Compress","Image size is too large\nCompressing...",true);
            int qual = 100;
            // Degradation
            while (imageBytes.length > 1040000) {
                qual -= 5;
                baos = new ByteArrayOutputStream();
                image.compress(Bitmap.CompressFormat.JPEG,qual,baos);
                imageBytes = baos.toByteArray();
            }
            // Resize
//        while (imageBytes.length > 1048576) {
//            imageBitmap = Bitmap.createScaledBitmap(imageBitmap, (int) (imageBitmap.getWidth() * 0.9), (int) (imageBitmap.getHeight() * 0.9), true);
//            baos = new ByteArrayOutputStream();
//            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
//            imageBytes = baos.toByteArray();
//        }
            pd.dismiss();
        }

        Blob blob = Blob.fromBytes(imageBytes);
        Map<String, Object> imageMap = new HashMap<>();
        imageMap.put("imageName", name);
        imageMap.put("imageData", blob);

        pd=ProgressDialog.show(this,"Upload image","Uploading...",true);
        ref.document(name).set(imageMap)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "DocumentSnapshot successfully written!");
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "Error writing document", e);
                }
            });
        pd.dismiss();
    }
}
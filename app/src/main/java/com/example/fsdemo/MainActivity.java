package com.example.fsdemo;

import static com.example.fsdemo.FBRef.refImageStamp;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";
    private ImageView iV;

    @SuppressLint("WrongThread")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iV = findViewById(R.id.iV);

    }

    public void addImage(Map<String, Object> image, CollectionReference ref) {
        ref.document(String.valueOf(image.get("imageName"))).set(image)
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
    }

    public void readImage(View view) {
        DocumentReference docRef = refImageStamp.document("20241122");

        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
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
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "get failed with ", e);
            }
        });
    }

    public void takeFull(View view) {
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cat);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG,100,baos);
        byte[] imageBytes = baos.toByteArray();
        Toast.makeText(this, ""+imageBytes.length, Toast.LENGTH_SHORT).show();
        // Resize
//        while (imageBytes.length > 1048576) {
//            imageBitmap = Bitmap.createScaledBitmap(imageBitmap, (int) (imageBitmap.getWidth() * 0.9), (int) (imageBitmap.getHeight() * 0.9), true);
//            baos = new ByteArrayOutputStream();
//            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
//            imageBytes = baos.toByteArray();
//        }
        // Degradation
        int qual = 100;
        while (imageBytes.length > 1040000) {
            qual -= 2;
            baos = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.JPEG,qual,baos);
            imageBytes = baos.toByteArray();
        }

        Blob blob = Blob.fromBytes(imageBytes);
        Map<String, Object> image = new HashMap<>();
        image.put("imageName", "20241122");
        image.put("imageData", blob);

        addImage(image, refImageStamp);
    }

    public void takeStamp(View view) {
    }

    public void gallery(View view) {
    }
}
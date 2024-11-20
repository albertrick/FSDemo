package com.example.fsdemo;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class FBRef {
    public static FirebaseFirestore FBFS = FirebaseFirestore.getInstance();
    public static CollectionReference refImageStamp = FBFS.collection("imageStamp");
    public static CollectionReference refImageFull = FBFS.collection("imageFull");
    public static CollectionReference refImageGallery = FBFS.collection("imageGallery");

}

package com.example.stims_studentedition;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.camera.core.ImageCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;

public class ScanFragment extends Fragment {
    private static final int PERMISSION_CODE = 1234;
    private static final int CAPTURE_CODE = 1001;

    String date = new SimpleDateFormat("yyyy, MMMM, d,EEEE", Locale.getDefault()).format(new Date());
    String time = new SimpleDateFormat("h:mm:a", Locale.getDefault()).format(new Date());

    private Uri image_uri;
    ImageCapture imageCapture;


    private PreviewView previewView;

    ProgressDialog progressDialog;

    Button btnScan;

    String scanResult, userPurpose;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_scan, container, false);

        btnScan = v.findViewById(R.id.btnScan);

        final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
                result -> {
                    result.getClass();
                    scanResult = result.getContents();
                    if (TextUtils.isEmpty(scanResult)) {
                        Toast.makeText(getActivity(), "NOTHING SCANNED", Toast.LENGTH_SHORT).show();
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle(scanResult);
                        builder.setCancelable(false);
                        builder.setMessage("Enter Purpose");

                        final EditText input = new EditText(getActivity());
                        input.setInputType(InputType.TYPE_CLASS_TEXT);
                        builder.setView(input);

                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        builder.setPositiveButton("OK", (dialog, which) -> {
                            userPurpose = input.getText().toString();

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                                        == PackageManager.PERMISSION_DENIED ||
                                        ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                                == PackageManager.PERMISSION_DENIED) {
                                    String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                                    requestPermissions(permissions, PERMISSION_CODE);
                                } else {
                                    if(userPurpose == null){
                                        Toast.makeText(getActivity(), "Purpose cannot be nothing", Toast.LENGTH_SHORT).show();
                                    }else {
                                        startCamera(scanResult, date , time, userPurpose);
                                    }                                }
                            } else {
                                if(userPurpose == null){
                                    Toast.makeText(getActivity(), "Purpose cannot be nothing", Toast.LENGTH_SHORT).show();
                                }else {
                                    startCamera(scanResult, date , time, userPurpose);
                                }                            }
                        });
                        builder.show();
                    }
                });

        btnScan.setOnClickListener(view -> {
            barcodeLauncher.launch(new ScanOptions());
            startScan();
        });

        return v;

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_CODE) {
            // Check if the permission was granted or denied
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with the desired action
                if(userPurpose == null){
                    Toast.makeText(getActivity(), "Purpose cannot be nothing", Toast.LENGTH_SHORT).show();
                }else {
                    startCamera(scanResult, date , time, userPurpose);
                }
            } else {
                Toast.makeText(getActivity(), "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void startScan() {
        Toast.makeText(getActivity(), "For Flash use Volume up Key", Toast.LENGTH_SHORT).show();
        ScanOptions options = new ScanOptions();
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(Capture.class);
    }

    private void startCamera(String room, String date, String time, String purpose) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "STIms_Photo");
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "STIms_" + room + "," + date + ".jpg");
        values.put(MediaStore.Images.Media.DESCRIPTION, "STIms Student Edition School Scanner" + "\n" + "Purpose: " + purpose);
        image_uri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Log.d("Image Log", image_uri.toString() + "Image_Uri the start");


        if (image_uri != null) {
            Log.d("Image Log", image_uri.toString() + "Start Intent");

            Intent camintent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            camintent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
            startActivityForResult(camintent, CAPTURE_CODE);

        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (image_uri != null) {

            Log.d("Image Log", image_uri + "Uploaded Images");
            StorageReference storageRef = FirebaseStorage.getInstance().getReference();
            StorageReference imageRef = storageRef.child("images/" + image_uri.getLastPathSegment());

            UploadTask uploadTask = imageRef.putFile(image_uri);
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                // Image upload successful
                // Handle the success case here
            }).addOnFailureListener(e -> {
                // Image upload failed
                // Handle the failure case here
            });

        }else{
            Toast.makeText(getActivity(), "Scan Failed, Please Try Again", Toast.LENGTH_LONG).show();
        }
    }






    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }
}

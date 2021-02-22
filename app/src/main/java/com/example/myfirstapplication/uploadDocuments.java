package com.example.myfirstapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;

public class uploadDocuments extends AppCompatActivity {
    private EditText file_Name;
    private CheckBox image, pdf;
    private Button choose, upload, show;
    private ListView listFiles;
    ArrayList<String> fileListing = new ArrayList<String>();
    ArrayList<Uri> fileUris = new ArrayList<Uri>();
    Uri fileUri;
    StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_documents);
        file_Name = findViewById(R.id.file_Name);
        image = findViewById(R.id.imageCheckbox);
        pdf = findViewById(R.id.pdfCheckbox);
        choose = findViewById(R.id.cFile);
        upload = findViewById(R.id.uFile);
        show = findViewById(R.id.sUp);
        listFiles = findViewById(R.id.list);
        Intent intent = getIntent();
        String str = intent.getStringExtra("MAC_ADDR");
        choose.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                if(image.isChecked() && !pdf.isChecked()) {
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(intent, 100);
                }
                else if(!image.isChecked() && pdf.isChecked()) {
                    intent.setType("application/pdf");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(intent, 100);
                }
                else {
                    Toast.makeText(uploadDocuments.this, "Check any one of the boxes.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                storageReference = FirebaseStorage.getInstance().getReference("User Documents/" + str + "/" + file_Name.getText().toString());
                fileListing.add(file_Name.getText().toString());
                UploadTask uploadTask = storageReference.putFile(fileUri);
                Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (!task.isSuccessful()) {
                            Toast.makeText(uploadDocuments.this, "Upload Failure", Toast.LENGTH_LONG).show();
                            throw task.getException();
                        }
                        return storageReference.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            Uri downloadUri = task.getResult();
                            fileUris.add(downloadUri);
                            Toast.makeText(uploadDocuments.this, "Upload Success", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });

        show.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(fileListing.size() > 0) {
                    ArrayAdapter<String> files = new ArrayAdapter<String>(uploadDocuments.this, android.R.layout.simple_list_item_1, fileListing);
                    listFiles.setAdapter(files);
                    listFiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            openInBrowser(view, position);
                        }
                    });
                }
                else
                    Toast.makeText(uploadDocuments.this, "Unable to list files.", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void openInBrowser(View v, int pos) {
        Intent intent = new Intent(Intent.ACTION_VIEW, fileUris.get(pos));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 100 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            fileUri = data.getData();
        }
    }
}
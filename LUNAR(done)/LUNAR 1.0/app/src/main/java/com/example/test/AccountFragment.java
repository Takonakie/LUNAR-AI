package com.example.test;

import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class AccountFragment extends Fragment {

    private AccountViewModel mViewModel;
    private ImageView imageView;
    private FloatingActionButton changePhotoButton;
    private EditText usernameEditText;
    private EditText emailEditText;
    private Button saveButton;

    private Uri imageUri;
    private String currentUsername;
    private String currentEmail;

    public static AccountFragment newInstance() {
        return new AccountFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        imageView = view.findViewById(R.id.imageView);
        changePhotoButton = view.findViewById(R.id.changePhotoButton);
        usernameEditText = view.findViewById(R.id.usn);
        emailEditText = view.findViewById(R.id.email);
        saveButton = view.findViewById(R.id.savebtn);

        // Load user data from the server
        loadUserData();


        changePhotoButton.setOnClickListener(v -> {
            ImagePicker.with(this)
                    .crop()
                    .compress(1024)
                    .maxResultSize(1080, 1080)
                    .start();
        });

        saveButton.setOnClickListener(v -> saveChanges());

        return view;
    }

    private void loadUserData() {
        String url = "http://192.168.24.29/lunar/get_user.php"; // Use 10.0.2.2 for emulator

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            JSONObject user = jsonResponse.getJSONObject("user");
                            currentUsername = user.getString("username");
                            currentEmail = user.getString("email");
                            String photoUri = user.getString("photo_uri");


                            usernameEditText.setText(currentUsername); // Display username
                            emailEditText.setText(currentEmail);

                            usernameEditText.setText(currentUsername);
                            emailEditText.setText(currentEmail);
                            if (photoUri != null && !photoUri.isEmpty()) {
                                imageUri = Uri.parse(photoUri);
                                imageView.setImageURI(imageUri);
                            }
                        } else {
                            Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error loading user data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show()
        );

        RequestQueue queue = Volley.newRequestQueue(requireContext());
        queue.add(stringRequest);
    }

    private void saveChanges() {
        String newUsername = usernameEditText.getText().toString().trim();
        String newEmail = emailEditText.getText().toString().trim();

        if (newUsername.isEmpty() || newEmail.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if any changes were made
        if (newUsername.equals(currentUsername) && newEmail.equals(currentEmail) && imageUri == null) {
            Toast.makeText(getContext(), "No changes made", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "http://192.168.24.29/lunar/update_user.php"; // Use 10.0.2.2 for emulator
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        boolean success = jsonResponse.getBoolean("success");
                        if (success) {
                            Toast.makeText(getContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();

                            // Update current values
                            currentUsername = newUsername;
                            currentEmail = newEmail;

                            // Update the navigation header if applicable
                            if (getActivity() instanceof MainActivity) {
                                ((MainActivity) getActivity()).updateNavHeader(newUsername, newEmail);
                            }
                        } else {
                            Toast.makeText(getContext(), jsonResponse.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error processing server response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("username", newUsername);
                params.put("email", newEmail);
                if (imageUri != null) {
                    params.put("photo_uri", imageUri.toString());
                }
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(requireContext());
        queue.add(stringRequest);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && data.getData() != null) {
            imageUri = data.getData();
            imageView.setImageURI(imageUri);
        }
    }
}


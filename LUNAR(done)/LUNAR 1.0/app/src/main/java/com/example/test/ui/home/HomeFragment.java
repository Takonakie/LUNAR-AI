package com.example.test.ui.home;

import static android.app.Activity.RESULT_OK;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.test.CameraActivity;
import com.example.test.R;
import com.example.test.StepCounterActivity;
import com.example.test.databinding.FragmentHomeBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HomeFragment extends Fragment implements SensorEventListener {
    private FragmentHomeBinding binding;
    private RecyclerView recyclerView;
    private EditText messageEditText;
    private ImageButton sendButton, cameraBtn, micBtn;
    private ImageView imageHome;

    private ArrayList<Message> messageArrayList;
    private MessageAdapter messageAdapter;
    public static final MediaType JSON = MediaType.get("application/json");
    OkHttpClient client = new OkHttpClient();

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float[] gravity = new float[3];
    private float lastAccelerationMagnitude = 0.0f;
    private long lastShakeTime = 0;

    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;
    private static final String CHANNEL_ID = "shake_channel";
    private static final int NOTIFICATION_ID = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        // Setup UI Components
        recyclerView = binding.recylerView;
        messageEditText = binding.TextInput;
        sendButton = binding.sendButton;
        imageHome = binding.imageHome;
        micBtn = binding.mic;
        cameraBtn = binding.camera;

        // Setup RecyclerView
        messageArrayList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageArrayList);
        recyclerView.setAdapter(messageAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Setup Sensor Manager for Shake Detection
        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(getContext(), "Accelerometer not available", Toast.LENGTH_SHORT).show();
        }

        // Create Notification Channel
        createNotificationChannel();

        // Button Listeners
        sendButton.setOnClickListener(v -> sendMessage());
        micBtn.setOnClickListener(v -> speak());
        cameraBtn.setOnClickListener(v -> openCamera());

        return view;
    }

    private void sendMessage() {
        String question = messageEditText.getText().toString().trim();
        if (!question.isEmpty()) {
            addToChat(question, Message.SENT_BY_ME);
            messageEditText.setText("");
            callAPI(question);
            imageHome.setVisibility(View.GONE);
        }
    }

    private void openCamera() {
        Intent intent = new Intent(requireActivity(), CameraActivity.class);
        startActivity(intent);
    }

    private void speak() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
        } catch (Exception e) {
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            messageEditText.setText(result.get(0));
        }
    }

    private void callAPI(String question) {
        messageArrayList.add(new Message("Typing...", Message.SENT_BY_BOT));
        JSONObject jsonObject = new JSONObject();
        try {
            JSONArray messagesArray = new JSONArray();
            JSONObject messageObject = new JSONObject();
            messageObject.put("role", "user");
            messageObject.put("content", question);
            messagesArray.put(messageObject);

            jsonObject.put("messages", messagesArray);
            jsonObject.put("model", "gpt-3.5-turbo");
            jsonObject.put("temperature", 0.7);
        } catch (JSONException e) {
            addResponse("Error creating request: " + e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer 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")
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                addResponse("Failed: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        String result = jsonObject.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");
                        addResponse(result.trim());
                    } catch (JSONException e) {
                        addResponse("Error parsing response: " + e.getMessage());
                    }
                } else {
                    addResponse("Failed: " + response.message());
                }
            }
        });
    }

    private void addToChat(String message, String sentBy) {
        requireActivity().runOnUiThread(() -> {
            messageArrayList.add(new Message(message, sentBy));
            messageAdapter.notifyDataSetChanged();
            recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
        });
    }

    private void addResponse(String response) {
        messageArrayList.remove(messageArrayList.size() - 1);
        addToChat(response, Message.SENT_BY_BOT);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] linearAcceleration = applyLowPassFilter(event.values);
        float x = linearAcceleration[0], y = linearAcceleration[1], z = linearAcceleration[2];
        float accelerationMagnitude = (float) Math.sqrt(x * x + y * y + z * z);

        if (Math.abs(accelerationMagnitude - lastAccelerationMagnitude) > 2.0f) {
            long currentTime = System.currentTimeMillis();
            if (accelerationMagnitude > 35.0f && (currentTime - lastShakeTime) > 60000) {
                lastShakeTime = currentTime;
                handleShakeEvent();
            }
        }
        lastAccelerationMagnitude = accelerationMagnitude;
    }

    private float[] applyLowPassFilter(float[] input) {
        for (int i = 0; i < input.length; i++) {
            gravity[i] = 0.8f * gravity[i] + 0.2f * input[i];
            input[i] = input[i] - gravity[i];
        }
        return input;
    }

    private void handleShakeEvent() {
        Log.d("ShakeEvent", "Shake detected!");
        sendNotification();
        vibrateDevice();
    }


    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Shake Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void sendNotification() {
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Intent ke StepCounterActivity
        Intent intent = new Intent(getContext(), StepCounterActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK );

        PendingIntent pendingIntent = PendingIntent.getActivity(
                getContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Membangun notifikasi
        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.logo2) // Ganti dengan ikon kecil yang sesuai
                .setContentTitle("Stay alert while running!")
                .setContentText("Be aware of your surroundings and stay safe.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(soundUri)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent); // Set PendingIntent di sini

        // Menampilkan notifikasi
        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }


    private void vibrateDevice() {
        Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(500); // Getaran selama 500 ms
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Tidak digunakan
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }
}

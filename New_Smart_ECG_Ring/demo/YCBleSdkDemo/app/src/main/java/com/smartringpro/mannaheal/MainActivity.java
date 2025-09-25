package com.smartringpro.mannaheal;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.jakewharton.threetenabp.AndroidThreeTen;
import com.smartringpro.mannaheal.api.profile.ProfileDataRepository;
import com.smartringpro.mannaheal.api.profile.ProfileDataResponse;
import com.smartringpro.mannaheal.api.register.FcmTokenResponse;
import com.smartringpro.mannaheal.api.register.RegisterRepository;
import com.smartringpro.mannaheal.ui.activities.HomeActivity;
import com.smartringpro.mannaheal.ui.activities.LoginActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidThreeTen.init(this);

        FirebaseApp.initializeApp(this);

        SharedPreferences prefs = getSharedPreferences("AppPreferences", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

        Intent intent;
        if (isLoggedIn) {
            intent = new Intent(this, HomeActivity.class);
        } else {
            intent = new Intent(this, LoginActivity.class);
        }
        startActivity(intent);
        checkAndFetchUserId();
        finish();

    }

    private void checkAndFetchUserId() {
        SharedPreferences sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE);
        int id = sharedPreferences.getInt("id", -1);
        int userId = sharedPreferences.getInt("user_id", -1);

        if (id != -1 && userId == -1) {
            fetchUserProfileData(id);
        } else {
//            FirebaseMessaging.getInstance().getToken()
//                    .addOnSuccessListener(token -> sendTokenToBackend(token, userId));
        }
    }

    private void fetchUserProfileData(int id) {
        ProfileDataRepository repository = new ProfileDataRepository();
        Call<ProfileDataResponse> call = repository.getUserProfileData(id);

        call.enqueue(new Callback<ProfileDataResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProfileDataResponse> call, @NonNull Response<ProfileDataResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ProfileDataResponse.ProfileData profileData = response.body().getData();
                    Log.d("MainActivity", "Fetched Profile: " + profileData);

                    int userIdFromApi = profileData.getUser_id();

                    SharedPreferences sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE);
                    sharedPreferences.edit().putInt("user_id", userIdFromApi).apply();

//                    FirebaseMessaging.getInstance().getToken()
//                            .addOnSuccessListener(token -> sendTokenToBackend(token, userIdFromApi));

                    Log.d("MainActivity", "Saved user_id: " + userIdFromApi);
                } else {
                    try {
                        Log.e("MainActivity", "Profile API error: " + (response.errorBody() != null ? response.errorBody().string() : "null"));
                    } catch (Exception e) {
                        Log.e("MainActivity", "Error reading errorBody", e);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProfileDataResponse> call, @NonNull Throwable t) {
                Log.e("MainActivity", "Profile API failed: " + t.getMessage(), t);
            }
        });
    }

    private void sendTokenToBackend(String token, int userId) {
        if (userId != -1) {
            Log.d("FCM_TOKEN", "Sending token to backend for user " + userId);

            RegisterRepository repository = new RegisterRepository();
            Call<FcmTokenResponse> call = repository.sendFcmToken(String.valueOf(userId), token);

            call.enqueue(new Callback<FcmTokenResponse>() {
                @Override
                public void onResponse(@NonNull Call<FcmTokenResponse> call, @NonNull Response<FcmTokenResponse> response) {
                    if (response.isSuccessful()) {
                        Log.d("FCM_TOKEN", "Token sent successfully: " + (response.body() != null ? response.body().getMessage() : "null"));
                    } else {
                        Log.e("FCM_TOKEN", "Failed to send token. Code: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<FcmTokenResponse> call, @NonNull Throwable t) {
                    Log.e("FCM_TOKEN", "Error sending token: " + t.getMessage(), t);
                }
            });
        } else {
            Log.e("FCM_TOKEN", "User ID not found in SharedPreferences");
        }
    }

}

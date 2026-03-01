package com.example.fuelmanagementapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fuelmanagementapp.activities.CreateTripActivity;
import com.example.fuelmanagementapp.activities.TripsActivity;
import com.example.fuelmanagementapp.models.ApiResponse;
import com.example.fuelmanagementapp.network.ApiClient;
import com.example.fuelmanagementapp.utils.Constants;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private TextView tvWelcome;
    private TextView tvInfoText;
    private Button btnCreateTrip;
    private Button btnViewTrips;
    private Button btnSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ApiClient.init(this);

        initViews();
        setupClickListeners();

        checkApiConnection();

        Log.d(TAG, "MainActivity створено. Базова адреса: " + ApiClient.getCurrentBaseUrl());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "MainActivity відновлено");
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tv_welcome);
        tvInfoText = findViewById(R.id.tv_selected_driver);
        btnCreateTrip = findViewById(R.id.btn_create_trip);
        btnViewTrips = findViewById(R.id.btn_view_trips);
        btnSettings = findViewById(R.id.btn_settings);
        
        tvInfoText.setText("Пост охорони\nВедіть облік виїздів і приїздів водіїв");
        tvInfoText.setVisibility(View.VISIBLE);
    }

    private void setupClickListeners() {
        btnCreateTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Натиснуто кнопку створення поїздки");

                if (!ApiClient.isNetworkAvailable()) {
                    Toast.makeText(MainActivity.this,
                            "Немає підключення до Інтернету", Toast.LENGTH_LONG).show();
                    return;
                }

                Intent intent = new Intent(MainActivity.this, CreateTripActivity.class);
                startActivityForResult(intent, Constants.REQUEST_CODE_CREATE_TRIP);
            }
        });

        btnViewTrips.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Натиснуто кнопку перегляду поїздок");

                if (!ApiClient.isNetworkAvailable()) {
                    Toast.makeText(MainActivity.this,
                            "Немає підключення до Інтернету", Toast.LENGTH_LONG).show();
                    return;
                }

                Intent intent = new Intent(MainActivity.this, TripsActivity.class);
                startActivity(intent);
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Натиснуто кнопку налаштувань");
                showSettingsDialog();
            }
        });
    }

    private void checkApiConnection() {
        Log.d(TAG, "Перевірка підключення до API: " + ApiClient.getCurrentBaseUrl());

        tvWelcome.setText("Система обліку палива\n Перевірка підключення до сервера...");

        ApiClient.getApiService().healthCheck().enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                Log.d(TAG, "Health check response: " + response.code() + ", body: " + response.body());

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<String> apiResponse = response.body();
                            if (apiResponse.isSuccess()) {
                                tvWelcome.setText("Система обліку палива\n З'єднання з сервером встановлено");
                                Log.i(TAG, "Підключення до API успішне");
                            } else {
                                tvWelcome.setText("Система обліку палива\n Помилка сервера: " + apiResponse.getMessage());
                                Log.w(TAG, "Помилка API: " + apiResponse.getMessage());
                            }
                        } else {
                            tvWelcome.setText("Система обліку палива\n Помилка підключення до сервера (код: " + response.code() + ")");
                            Log.e(TAG, "Збій підключення до API з кодом: " + response.code());
                        }
                    }
                });
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                Log.e(TAG, "Перевірка стану завершилась помилкою", t);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvWelcome.setText("Система обліку палива\n Сервер недоступний");

                        String errorMessage = "Помилка підключення: " + t.getMessage();
                        if (t.getMessage() != null && t.getMessage().contains("Failed to connect")) {
                            errorMessage = "Не вдалося підключитися до сервера. Перевірте:\n" +
                                    "• Чи працює сервер\n" +
                                    "• Правильність адреси: " + ApiClient.getCurrentBaseUrl() + "\n" +
                                    "• Підключення до інтернету";
                        }

                        Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void showSettingsDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Налаштування")
                .setItems(new CharSequence[]{
                        "Інформація про підключення",
                        "Перезавантажити API клієнт",
                        "Перевірити підключення"
                }, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                showConnectionInfo();
                                break;
                            case 1:
                                reloadApiClient();
                                break;
                            case 2:
                                checkApiConnection();
                                break;
                        }
                    }
                })
                .setNegativeButton("Скасування", null)
                .show();
    }

    private void showConnectionInfo() {
        String networkStatus = ApiClient.isNetworkAvailable() ? "Є" : "Ні";
        String info = "Базовий URL: " + ApiClient.getCurrentBaseUrl() + "\n" +
                "Підключення до мережі: " + networkStatus + "\n" +
                "Режим роботи: Пост охорони";

        new android.app.AlertDialog.Builder(this)
                .setTitle("Інформація про підключення")
                .setMessage(info)
                .setPositiveButton("OK", null)
                .setNeutralButton("Тест підключення", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        checkApiConnection();
                    }
                })
                .show();
    }

    private void reloadApiClient() {
        ApiClient.resetClient();
        Toast.makeText(this, "API клієнт перезапущено", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "API клієнт перезапущено");

        checkApiConnection();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == Constants.REQUEST_CODE_CREATE_TRIP && resultCode == RESULT_OK) {
            Toast.makeText(this, "Поїздку успішно створено!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MainActivity знищено");
    }
}

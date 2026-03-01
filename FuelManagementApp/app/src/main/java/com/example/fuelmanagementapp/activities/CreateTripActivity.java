package com.example.fuelmanagementapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fuelmanagementapp.R;
import com.example.fuelmanagementapp.adapters.DriversAdapter;
import com.example.fuelmanagementapp.adapters.VehiclesAdapter;
import com.example.fuelmanagementapp.models.ApiResponse;
import com.example.fuelmanagementapp.models.Driver;
import com.example.fuelmanagementapp.models.Trip;
import com.example.fuelmanagementapp.models.Vehicle;
import com.example.fuelmanagementapp.network.ApiClient;
import com.example.fuelmanagementapp.network.dto.TripCreateRequest;
import com.example.fuelmanagementapp.utils.Constants;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateTripActivity extends AppCompatActivity implements 
        VehiclesAdapter.OnVehicleClickListener, DriversAdapter.OnDriverClickListener {

    private static final String TAG = "CreateTripActivity";

    private TextView tvSelectedVehicle;
    private TextView tvSelectedDriver;
    private Button btnSelectVehicle;
    private Button btnSelectDriver;
    private EditText etOdometer;
    private Button btnCreate;
    private ProgressBar progressBar;
    private TextView tvError;
    private androidx.cardview.widget.CardView cardVehicle;
    private androidx.cardview.widget.CardView cardDriver;

    private Vehicle selectedVehicle;
    private Driver selectedDriver;

    private List<Vehicle> vehicles = new ArrayList<>();
    private List<Driver> drivers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_trip);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Створити поїздку");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        tvSelectedVehicle = findViewById(R.id.tv_selected_vehicle);
        tvSelectedDriver = findViewById(R.id.tv_selected_driver);
        btnSelectVehicle = findViewById(R.id.btn_select_vehicle);
        btnSelectDriver = findViewById(R.id.btn_select_driver);
        etOdometer = findViewById(R.id.et_odometer);
        btnCreate = findViewById(R.id.btn_create);
        progressBar = findViewById(R.id.progress_bar);
        tvError = findViewById(R.id.tv_error);
        cardVehicle = findViewById(R.id.card_vehicle);
        cardDriver = findViewById(R.id.card_driver);
    }

    private void setupClickListeners() {
        btnSelectVehicle.setOnClickListener(v -> showVehicleSelectionDialog());
        btnSelectDriver.setOnClickListener(v -> showDriverSelectionDialog());
        btnCreate.setOnClickListener(v -> createTrip());
    }

    private void showVehicleSelectionDialog() {
        if (!ApiClient.isNetworkAvailable()) {
            Toast.makeText(this, "Немає підключення до Інтернету", Toast.LENGTH_LONG).show();
            return;
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_list_selection, null);
        builder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        RecyclerView recyclerView = dialogView.findViewById(R.id.recycler_dialog);
        ProgressBar progressBar = dialogView.findViewById(R.id.progress_dialog);
        TextView tvEmpty = dialogView.findViewById(R.id.tv_dialog_empty);

        tvTitle.setText("Виберіть автомобіль");

        android.app.AlertDialog dialog = builder.create();
        dialog.show();

        List<Vehicle> vehicleList = new ArrayList<>();
        VehiclesAdapter adapter = new VehiclesAdapter(vehicleList, vehicle -> {
            selectedVehicle = vehicle;
            tvSelectedVehicle.setText("" + vehicle.getDisplayName());
            if (cardVehicle != null) {
                cardVehicle.setCardBackgroundColor(ContextCompat.getColor(CreateTripActivity.this, R.color.secondary_container));
            }
            dialog.dismiss();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        ApiClient.getApiService().getActiveVehicles().enqueue(new Callback<ApiResponse<List<Vehicle>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Vehicle>>> call, Response<ApiResponse<List<Vehicle>>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Vehicle>> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.hasData()) {
                        vehicleList.clear();
                        vehicleList.addAll(apiResponse.getData());
                        adapter.notifyDataSetChanged();

                        recyclerView.setVisibility(View.VISIBLE);
                        tvEmpty.setVisibility(vehicleList.isEmpty() ? View.VISIBLE : View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText("Помилка завантаження: " + apiResponse.getMessage());
                    }
                } else {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("Помилка сервера: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Vehicle>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("Помилка підключення: " + t.getMessage());
                Log.e(TAG, "Помилка завантаження автомобілів", t);
            }
        });
    }

    private void showDriverSelectionDialog() {
        if (!ApiClient.isNetworkAvailable()) {
            Toast.makeText(this, "Немає підключення до Інтернету", Toast.LENGTH_LONG).show();
            return;
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_list_selection, null);
        builder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        RecyclerView recyclerView = dialogView.findViewById(R.id.recycler_dialog);
        ProgressBar progressBar = dialogView.findViewById(R.id.progress_dialog);
        TextView tvEmpty = dialogView.findViewById(R.id.tv_dialog_empty);

        tvTitle.setText("Виберіть водія");

        android.app.AlertDialog dialog = builder.create();
        dialog.show();

        List<Driver> driverList = new ArrayList<>();
        DriversAdapter adapter = new DriversAdapter(driverList, driver -> {
            selectedDriver = driver;
            tvSelectedDriver.setText("" + driver.getFullName());
            if (cardDriver != null) {
                cardDriver.setCardBackgroundColor(ContextCompat.getColor(CreateTripActivity.this, R.color.secondary_container));
            }
            dialog.dismiss();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        ApiClient.getApiService().getActiveDrivers().enqueue(new Callback<ApiResponse<List<Driver>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Driver>>> call, Response<ApiResponse<List<Driver>>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Driver>> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.hasData()) {
                        driverList.clear();
                        driverList.addAll(apiResponse.getData());
                        adapter.notifyDataSetChanged();

                        recyclerView.setVisibility(View.VISIBLE);
                        tvEmpty.setVisibility(driverList.isEmpty() ? View.VISIBLE : View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText("Помилка завантаження: " + apiResponse.getMessage());
                    }
                } else {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("Помилка сервера: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Driver>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("Помилка підключення: " + t.getMessage());
                Log.e(TAG, "Помилка завантаження водіїв", t);
            }
        });
    }

    private void createTrip() {
        if (selectedVehicle == null) {
            showError("Виберіть автомобіль");
            return;
        }

        if (selectedDriver == null) {
            showError("Виберіть водія");
            return;
        }

        String odometerText = etOdometer.getText().toString().trim();
        if (TextUtils.isEmpty(odometerText)) {
            showError("Введіть пробіг");
            return;
        }

        Integer odometer;
        try {
            odometer = Integer.parseInt(odometerText);
            if (odometer <= 0) {
                showError("Пробіг має бути більше 0");
                return;
            }
        } catch (NumberFormatException e) {
            showError("Некорректний формат пробігу");
            return;
        }

        if (!ApiClient.isNetworkAvailable()) {
            Toast.makeText(this, "Немає підключення до Інтернету", Toast.LENGTH_LONG).show();
            return;
        }

        showLoading();
        hideError();

        TripCreateRequest request = new TripCreateRequest(
                selectedVehicle.getId(),
                selectedDriver.getId(),
                odometer
        );

        ApiClient.getApiService().createTrip(request).enqueue(new Callback<ApiResponse<Trip>>() {
            @Override
            public void onResponse(Call<ApiResponse<Trip>> call, Response<ApiResponse<Trip>> response) {
                hideLoading();

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Trip> apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        Toast.makeText(CreateTripActivity.this, 
                                "Поїздку успішно створено!", Toast.LENGTH_LONG).show();
                        Log.i(TAG, "Поїздку створено: " + apiResponse.getData().getId());
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        showError("Помилка: " + apiResponse.getMessage());
                    }
                } else {
                    showError("Помилка сервера: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Trip>> call, Throwable t) {
                hideLoading();
                showError("Помилка підключення: " + t.getMessage());
                Log.e(TAG, "Помилка створення поїздки", t);
            }
        });
    }

    @Override
    public void onVehicleClick(Vehicle vehicle) {
        selectedVehicle = vehicle;
        tvSelectedVehicle.setText("" + vehicle.getDisplayName());
        if (cardVehicle != null) {
            cardVehicle.setCardBackgroundColor(ContextCompat.getColor(this, R.color.secondary_container));
        }
    }

    @Override
    public void onDriverClick(Driver driver) {
        selectedDriver = driver;
        tvSelectedDriver.setText("" + driver.getFullName());
        if (cardDriver != null) {
            cardDriver.setCardBackgroundColor(ContextCompat.getColor(this, R.color.secondary_container));
        }
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        btnCreate.setEnabled(false);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        btnCreate.setEnabled(true);
    }

    private void showError(String message) {
        tvError.setVisibility(View.VISIBLE);
        tvError.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void hideError() {
        tvError.setVisibility(View.GONE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

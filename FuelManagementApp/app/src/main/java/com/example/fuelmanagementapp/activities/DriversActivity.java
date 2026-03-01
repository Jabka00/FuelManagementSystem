package com.example.fuelmanagementapp.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fuelmanagementapp.R;
import com.example.fuelmanagementapp.adapters.DriversAdapter;
import com.example.fuelmanagementapp.models.ApiResponse;
import com.example.fuelmanagementapp.models.Driver;
import com.example.fuelmanagementapp.network.ApiClient;
import com.example.fuelmanagementapp.utils.SharedPrefsUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriversActivity extends AppCompatActivity implements DriversAdapter.OnDriverClickListener {

    private RecyclerView recyclerView;
    private DriversAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private TextView tvError;

    private List<Driver> drivers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drivers);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Виберіть водія");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initViews();
        setupRecyclerView();
        loadDrivers();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_drivers);
        progressBar = findViewById(R.id.progress_bar);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        tvError = findViewById(R.id.tv_error);
    }

    private void setupRecyclerView() {
        adapter = new DriversAdapter(drivers, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadDrivers() {
        showLoading();

        ApiClient.getApiService().getActiveDrivers().enqueue(new Callback<ApiResponse<List<Driver>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Driver>>> call, Response<ApiResponse<List<Driver>>> response) {
                hideLoading();

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Driver>> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.hasData()) {
                        drivers.clear();
                        drivers.addAll(apiResponse.getData());
                        adapter.notifyDataSetChanged();

                        showContent();
                    } else {
                        showError("Помилка завантаження: " + apiResponse.getMessage());
                    }
                } else {
                    showError("Помилка сервера: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Driver>>> call, Throwable t) {
                hideLoading();
                showError("Помилка підключення: " + t.getMessage());
            }
        });
    }

    @Override
    public void onDriverClick(Driver driver) {
        SharedPrefsUtils.saveDriverInfo(this, driver.getId(), driver.getFullName());

        setResult(RESULT_OK);
        finish();
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);
        tvError.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }

    private void showContent() {
        recyclerView.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(drivers.isEmpty() ? View.VISIBLE : View.GONE);
        tvError.setVisibility(View.GONE);

        if (drivers.isEmpty()) {
            tvEmptyState.setText("Немає доступних водіїв");
        }
    }

    private void showError(String message) {
        recyclerView.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);
        tvError.setVisibility(View.VISIBLE);
        tvError.setText(message);

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

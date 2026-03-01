package com.example.fuelmanagementapp.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.example.fuelmanagementapp.R;
import com.example.fuelmanagementapp.adapters.TripsAdapter;
import com.example.fuelmanagementapp.models.ApiResponse;
import com.example.fuelmanagementapp.models.Trip;
import com.example.fuelmanagementapp.network.ApiClient;
import com.example.fuelmanagementapp.utils.Constants;
import com.example.fuelmanagementapp.utils.DateUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TripsActivity extends AppCompatActivity implements TripsAdapter.OnTripClickListener {

    private static final String TAG = "TripsActivity";

    private RecyclerView recyclerView;
    private TripsAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private TextView tvError;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton fabSort;

    private List<Trip> trips = new ArrayList<>();
    private List<Trip> originalTrips = new ArrayList<>(); 

    private enum SortBy {
        DATE, STATUS, DRIVER
    }

    private SortBy currentSortBy = SortBy.DATE;
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trips);

        if (getSupportActionBar() != null) {
            updateActionBarTitle();
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initViews();
        setupRecyclerView();
        loadTrips();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_trips);
        progressBar = findViewById(R.id.progress_bar);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        tvError = findViewById(R.id.tv_error);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        fabSort = findViewById(R.id.fab_sort);

        swipeRefreshLayout.setOnRefreshListener(this::loadTrips);
        
        if (fabSort != null) {
            fabSort.setOnClickListener(v -> showSortDialog());
        }
    }

    private void setupRecyclerView() {
        adapter = new TripsAdapter(trips, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadTrips() {
        if (!swipeRefreshLayout.isRefreshing()) {
            showLoading();
        }

        ApiClient.getApiService().getAllActiveTrips().enqueue(new Callback<ApiResponse<List<Trip>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Trip>>> call, Response<ApiResponse<List<Trip>>> response) {
                hideLoading();
                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Trip>> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.hasData()) {
                        originalTrips.clear();
                        originalTrips.addAll(apiResponse.getData());
                        
                        applySorting();

                        showContent();
                    } else {
                        showError("Помилка завантаження: " + apiResponse.getMessage());
                    }
                } else {
                    showError("Помилка сервера: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Trip>>> call, Throwable t) {
                hideLoading();
                swipeRefreshLayout.setRefreshing(false);
                showError("Помилка підключення: " + t.getMessage());
            }
        });
    }

    @Override
    public void onTripClick(Trip trip) {
        Intent intent = new Intent(this, TripDetailActivity.class);
        intent.putExtra(Constants.EXTRA_TRIP_ID, trip.getId());
        
        intent.putExtra(Constants.EXTRA_VEHICLE_INFO, trip.getVehicleInfo());
        intent.putExtra(Constants.EXTRA_DRIVER_FULL_NAME, trip.getDriverDisplayName());
        startActivityForResult(intent, Constants.REQUEST_CODE_TRIP_DETAIL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.REQUEST_CODE_TRIP_DETAIL && resultCode == RESULT_OK) {
            loadTrips();
        }
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
        tvEmptyState.setVisibility(trips.isEmpty() ? View.VISIBLE : View.GONE);
        tvError.setVisibility(View.GONE);

        if (trips.isEmpty()) {
            tvEmptyState.setText("Наразі немає активних поїздок");
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_trips, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            loadTrips();
            return true;
        } else if (id == R.id.action_sort) {
            showSortDialog();
            return true;
        } else if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showSortDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sort_trips, null);
        builder.setView(dialogView);

        EditText etSearch = dialogView.findViewById(R.id.et_search);
        RadioGroup radioGroupSortBy = dialogView.findViewById(R.id.radio_group_sort_by);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnApply = dialogView.findViewById(R.id.btn_apply);

        if (etSearch != null) {
            etSearch.setText(currentSearchQuery);
        }

        switch (currentSortBy) {
            case DATE:
                ((RadioButton) dialogView.findViewById(R.id.radio_date)).setChecked(true);
                break;
            case STATUS:
                ((RadioButton) dialogView.findViewById(R.id.radio_status)).setChecked(true);
                break;
            case DRIVER:
                ((RadioButton) dialogView.findViewById(R.id.radio_driver)).setChecked(true);
                break;
        }

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnApply.setOnClickListener(v -> {
            
            if (etSearch != null) {
                currentSearchQuery = etSearch.getText().toString().trim();
            }

            int selectedSortBy = radioGroupSortBy.getCheckedRadioButtonId();
            if (selectedSortBy == R.id.radio_date) {
                currentSortBy = SortBy.DATE;
            } else if (selectedSortBy == R.id.radio_status) {
                currentSortBy = SortBy.STATUS;
            } else if (selectedSortBy == R.id.radio_driver) {
                currentSortBy = SortBy.DRIVER;
            }

            applySorting();
            updateActionBarTitle();
            dialog.dismiss();
            
            String sortInfo = getSortInfoText();
            Toast.makeText(this, "Застосовано: " + sortInfo, Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void applySorting() {
        trips.clear();
        
        List<Trip> filteredTrips = new ArrayList<>();
        if (currentSearchQuery.isEmpty()) {
            filteredTrips.addAll(originalTrips);
        } else {
            String query = currentSearchQuery.toLowerCase();
            for (Trip trip : originalTrips) {
                
                if (trip.getTripDisplayNumber().toLowerCase().contains(query)) {
                    filteredTrips.add(trip);
                    continue;
                }
                
                if (trip.getRouteInfo().toLowerCase().contains(query)) {
                    filteredTrips.add(trip);
                    continue;
                }
                
                if (trip.getDriverDisplayName().toLowerCase().contains(query)) {
                    filteredTrips.add(trip);
                    continue;
                }
                
                if (trip.getVehicleInfo().toLowerCase().contains(query)) {
                    filteredTrips.add(trip);
                    continue;
                }
                
                if (trip.getStatusDisplayText().toLowerCase().contains(query)) {
                    filteredTrips.add(trip);
                    continue;
                }
            }
        }
        
        Comparator<Trip> comparator = getComparator();
        if (comparator != null) {
            Collections.sort(filteredTrips, comparator);
        }
        
        trips.addAll(filteredTrips);
        adapter.notifyDataSetChanged();
        Log.d(TAG, "Застосовано сортування: " + currentSortBy + ", пошук: " + currentSearchQuery);
    }

    private Comparator<Trip> getComparator() {
        Comparator<Trip> comparator = null;

        switch (currentSortBy) {
            case DATE:
                
                comparator = (t1, t2) -> {
                    String date1 = t1.getCreatedAt() != null ? t1.getCreatedAt() : "";
                    String date2 = t2.getCreatedAt() != null ? t2.getCreatedAt() : "";
                    if (date1.isEmpty() && date2.isEmpty()) return 0;
                    if (date1.isEmpty()) return 1;
                    if (date2.isEmpty()) return -1;
                    
                    java.util.Date d1 = DateUtils.parseApiDate(date1);
                    java.util.Date d2 = DateUtils.parseApiDate(date2);
                    if (d1 == null && d2 == null) return 0;
                    if (d1 == null) return 1;
                    if (d2 == null) return -1;
                    
                    return d2.compareTo(d1);
                };
                break;

            case STATUS:
                comparator = (t1, t2) -> {
                    String s1 = t1.getStatus() != null ? t1.getStatus() : "";
                    String s2 = t2.getStatus() != null ? t2.getStatus() : "";
                    return s1.compareToIgnoreCase(s2);
                };
                break;

            case DRIVER:
                comparator = (t1, t2) -> {
                    String d1 = t1.getDriverDisplayName();
                    String d2 = t2.getDriverDisplayName();
                    return d1.compareToIgnoreCase(d2);
                };
                break;
        }

        return comparator;
    }

    private String getSortInfoText() {
        String sortByText = "";
        switch (currentSortBy) {
            case DATE:
                sortByText = "дата";
                break;
            case STATUS:
                sortByText = "статус";
                break;
            case DRIVER:
                sortByText = "водій";
                break;
        }
        
        String searchText = currentSearchQuery.isEmpty() ? "" : ", пошук: " + currentSearchQuery;
        return sortByText + searchText;
    }

    private void updateActionBarTitle() {
        if (getSupportActionBar() != null) {
            String sortByIcon = "";
            switch (currentSortBy) {
                case DATE:
                    sortByIcon = "";
                    break;
                case STATUS:
                    sortByIcon = "";
                    break;
                case DRIVER:
                    sortByIcon = "";
                    break;
            }
            String searchIcon = currentSearchQuery.isEmpty() ? "" : "";
            getSupportActionBar().setTitle("Активні поїздки " + sortByIcon + searchIcon);
        }
    }
}

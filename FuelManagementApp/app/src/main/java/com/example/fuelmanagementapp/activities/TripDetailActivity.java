package com.example.fuelmanagementapp.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fuelmanagementapp.R;
import com.example.fuelmanagementapp.models.ApiResponse;
import com.example.fuelmanagementapp.models.Trip;
import com.example.fuelmanagementapp.network.ApiClient;
import com.example.fuelmanagementapp.network.dto.TripFinishRequest;
import com.example.fuelmanagementapp.network.dto.TripStartRequest;
import com.example.fuelmanagementapp.utils.Constants;
import com.example.fuelmanagementapp.utils.DateUtils;

import java.math.BigDecimal;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TripDetailActivity extends AppCompatActivity {

    private static final String TAG = "TripDetailActivity";

    private TextView tvTripNumber;
    private TextView tvRoute;
    private TextView tvStatus;
    private TextView tvVehicle;
    private TextView tvDriver;
    private TextView tvPlannedDistance;
    private TextView tvActualDistance;
    private TextView tvStartTime;
    private TextView tvEndTime;
    private TextView tvStartOdometer;
    private TextView tvEndOdometer;
    private TextView tvCurrentOdometer;

    private Button btnStartTrip;
    private Button btnFinishTrip;
    private ProgressBar progressBar;

    private Integer tripId;
    private Trip currentTrip;
    private String cachedVehicleInfo;
    private String cachedDriverName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);

        tripId = getIntent().getIntExtra(Constants.EXTRA_TRIP_ID, -1);
        
        cachedVehicleInfo = getIntent().getStringExtra(Constants.EXTRA_VEHICLE_INFO);
        cachedDriverName = getIntent().getStringExtra(Constants.EXTRA_DRIVER_FULL_NAME);

        if (tripId == -1) {
            Toast.makeText(this, "Помилка: ідентифікатор поїздки не знайдено", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Контроль поїздки");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initViews();
        setupClickListeners();
        loadTripDetails();
    }

    private void initViews() {
        tvTripNumber = findViewById(R.id.tv_trip_number);
        tvRoute = findViewById(R.id.tv_route);
        tvStatus = findViewById(R.id.tv_status);
        tvVehicle = findViewById(R.id.tv_vehicle);
        tvDriver = findViewById(R.id.tv_driver);
        tvPlannedDistance = findViewById(R.id.tv_planned_distance);
        tvActualDistance = findViewById(R.id.tv_actual_distance);
        tvStartTime = findViewById(R.id.tv_start_time);
        tvEndTime = findViewById(R.id.tv_end_time);
        tvStartOdometer = findViewById(R.id.tv_start_odometer);
        tvEndOdometer = findViewById(R.id.tv_end_odometer);
        tvCurrentOdometer = findViewById(R.id.tv_current_odometer);

        btnStartTrip = findViewById(R.id.btn_start_trip);
        btnFinishTrip = findViewById(R.id.btn_finish_trip);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupClickListeners() {
        btnStartTrip.setOnClickListener(v -> {
            
            if (!btnStartTrip.isEnabled()) {
                Log.w(TAG, "Спроба повторного кліку на кнопку старту заблокована");
                return;
            }
            
            Log.d(TAG, "Натиснуто кнопку виїзду для поїздки: " + tripId);
            showStartTripDialog();
        });

        btnFinishTrip.setOnClickListener(v -> {
            
            if (!btnFinishTrip.isEnabled()) {
                Log.w(TAG, "Спроба повторного кліку на кнопку фінішу заблокована");
                return;
            }
            
            Log.d(TAG, "Натиснуто кнопку приїзду для поїздки: " + tripId);
            showFinishTripDialog();
        });
    }

    private void loadTripDetails() {
        showLoading();

        ApiClient.getApiService().getTripInfo(tripId).enqueue(new Callback<ApiResponse<Trip>>() {
            @Override
            public void onResponse(Call<ApiResponse<Trip>> call, Response<ApiResponse<Trip>> response) {
                hideLoading();

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Trip> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.hasData()) {
                        currentTrip = apiResponse.getData();
                        Log.d(TAG, "Дані поїздки завантажено: ID=" + currentTrip.getId() + 
                                ", actualStartOdometer=" + currentTrip.getActualStartOdometer() + 
                                ", статус=" + currentTrip.getStatus());
                        updateUI();
                        loadCurrentOdometer();  
                    } else {
                        showError("Помилка завантаження: " + apiResponse.getMessage());
                    }
                } else {
                    showError("Помилка сервера: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Trip>> call, Throwable t) {
                hideLoading();
                showError("Помилка підключення: " + t.getMessage());
            }
        });
    }

    private void loadCurrentOdometer() {
        if (currentTrip == null || !currentTrip.canBeStarted()) {
            return; 
        }
        
        ApiClient.getApiService().getCurrentOdometer(tripId).enqueue(new Callback<ApiResponse<Integer>>() {
            @Override
            public void onResponse(Call<ApiResponse<Integer>> call, Response<ApiResponse<Integer>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Integer> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.hasData()) {
                        Integer currentOdometer = apiResponse.getData();
                        
                        if (currentTrip != null) {
                            currentTrip.setCurrentOdometer(currentOdometer);
                            Log.d(TAG, "Поточний пробіг автомобіля: " + currentOdometer + "км");
                        }
                        
                        tvCurrentOdometer.setText("Поточний пробіг автомобіля: " + currentOdometer + "км");
                        tvCurrentOdometer.setVisibility(View.VISIBLE);
                    } else {
                        Log.w(TAG, "Не вдалося отримати поточний пробіг");
                    }
                } else {
                    Log.w(TAG, "Помилка відповіді сервера: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Integer>> call, Throwable t) {
                Log.e(TAG, "Помилка завантаження поточного пробігу: " + t.getMessage());
            }
        });
    }

    private void updateUI() {
        if (currentTrip == null) return;

        tvTripNumber.setText("Поїздка: " + currentTrip.getTripDisplayNumber());
        tvRoute.setText("Маршрут: " + currentTrip.getRouteInfo());
        tvStatus.setText("Статус: " + currentTrip.getStatusDisplayText());
        
        String vehicleInfo = cachedVehicleInfo != null ? cachedVehicleInfo : currentTrip.getVehicleInfo();
        tvVehicle.setText("Автомобіль: " + vehicleInfo);
        
        String driverName = cachedDriverName != null ? cachedDriverName : currentTrip.getDriverDisplayName();
        tvDriver.setText("Водій: " + driverName);

        if (currentTrip.getPlannedDistance() != null) {
            tvPlannedDistance.setText("Запланована відстань: " + currentTrip.getPlannedDistance() + "км");
            tvPlannedDistance.setVisibility(View.VISIBLE);
        } else {
            tvPlannedDistance.setVisibility(View.GONE);
        }

        if (currentTrip.getActualDistance() != null) {
            tvActualDistance.setText("Фактична відстань: " + currentTrip.getActualDistance() + "км");
            tvActualDistance.setVisibility(View.VISIBLE);
        } else {
            tvActualDistance.setVisibility(View.GONE);
        }

        if (currentTrip.getActualStartTime() != null) {
            String startTime = DateUtils.formatFullDisplayDateTime(currentTrip.getActualStartTime());
            tvStartTime.setText("Час початку: " + startTime);
            tvStartTime.setVisibility(View.VISIBLE);
        } else {
            tvStartTime.setVisibility(View.GONE);
        }

        if (currentTrip.getActualEndTime() != null) {
            String endTime = DateUtils.formatFullDisplayDateTime(currentTrip.getActualEndTime());
            tvEndTime.setText("Час завершення: " + endTime);
            tvEndTime.setVisibility(View.VISIBLE);
        } else {
            tvEndTime.setVisibility(View.GONE);
        }

        if (currentTrip.hasStartOdometer()) {
            tvStartOdometer.setText("Початковий пробіг: " + currentTrip.getActualStartOdometer() + "км");
            tvStartOdometer.setVisibility(View.VISIBLE);
            
            tvStartOdometer.setOnLongClickListener(v -> {
                if (currentTrip.isStarted()) {
                    showUpdateStartOdometerDialog();
                    return true;
                }
                return false;
            });
        } else {
            tvStartOdometer.setVisibility(View.GONE);
        }

        if (currentTrip.hasEndOdometer()) {
            tvEndOdometer.setText("Кінцевий пробіг: " + currentTrip.getActualEndOdometer() + "км");
            tvEndOdometer.setVisibility(View.VISIBLE);
        } else {
            tvEndOdometer.setVisibility(View.GONE);
        }

        updateButtons();
    }

    private void updateButtons() {
        if (currentTrip == null) {
            btnStartTrip.setVisibility(View.GONE);
            btnFinishTrip.setVisibility(View.GONE);
            return;
        }

        if (currentTrip.canBeStarted()) {
            btnStartTrip.setVisibility(View.VISIBLE);
            btnStartTrip.setEnabled(true);
            btnFinishTrip.setVisibility(View.GONE);
        } else if (currentTrip.canBeFinished()) {
            btnStartTrip.setVisibility(View.GONE);
            btnFinishTrip.setVisibility(View.VISIBLE);
            btnFinishTrip.setEnabled(true);
        } else {
            btnStartTrip.setVisibility(View.GONE);
            btnFinishTrip.setVisibility(View.GONE);
        }
    }

    private void showStartTripDialog() {
        final boolean hasOdometer = currentTrip != null && currentTrip.hasCurrentOdometer();
        final Integer currentOdometer = hasOdometer ? currentTrip.getCurrentOdometer() : null;
        
        Log.d(TAG, "Відкриття діалогу виїзду. Поточний пробіг: " + 
                (currentOdometer != null ? currentOdometer + "км" : "не вказано"));
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Виїзд водія");

        String message = "Водій виїжджає\n\n";
        if (hasOdometer) {
            message += "ПОТОЧНИЙ ПРОБІГ АВТОМОБІЛЯ:\n" + currentOdometer + "км\n\n";
            message += "Початковий пробіг не може бути меншим!\n\n";
        }
        message += "Введіть показники одометра при виїзді:";
        
        builder.setMessage(message);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Наприклад: 150000");
        
        if (hasOdometer) {
            input.setText(String.valueOf(currentOdometer));
            input.selectAll();
        }
        
        builder.setView(input);

        builder.setPositiveButton("Зафіксувати виїзд", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String inputText = input.getText().toString().trim();
                if (inputText.isEmpty()) {
                    Toast.makeText(TripDetailActivity.this,
                            "Введіть показники одометра", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    Integer startOdometer = Integer.parseInt(inputText);
                    
                    Log.d(TAG, "Перевірка пробігу: введено " + startOdometer + "км, поточний " + 
                            (currentOdometer != null ? currentOdometer + "км" : "невідомо"));

                    if (startOdometer <= 0) {
                        Toast.makeText(TripDetailActivity.this,
                                "Показники одометра повинні бути більше 0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (hasOdometer && currentOdometer != null) {
                        if (startOdometer < currentOdometer) {
                            Log.e(TAG, "БЛОКУВАННЯ: Початковий пробіг " + startOdometer + 
                                    "менший за поточний " + currentOdometer);
                            
                            new AlertDialog.Builder(TripDetailActivity.this)
                                .setTitle("ПОМИЛКА ВВОДУ")
                                .setMessage("Початковий пробіг (" + startOdometer + "км) НЕ МОЖЕ бути менше поточного пробігу автомобіля (" + currentOdometer + "км)!\n\n" +
                                        "Поточний пробіг зафіксовано в системі.\n" +
                                        "Будь ласка, введіть правильне значення.")
                                .setPositiveButton("Виправити", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface d, int w) {
                                        showStartTripDialog();
                                    }
                                })
                                .setNegativeButton("Скасувати", null)
                                .setCancelable(false)
                                .show();
                            return;
                        }
                        
                        if (startOdometer > currentOdometer + 1000) {
                            Log.w(TAG, "Велика різниця в пробігу: " + (startOdometer - currentOdometer) + "км");
                            showLargeOdometerDifferenceWarning(startOdometer, currentOdometer);
                            return;
                        }
                    }

                    Log.i(TAG, "Перевірки пройдено, починаємо поїздку з пробігом " + startOdometer + "км");
                    startTripWithOdometer(startOdometer);

                } catch (NumberFormatException e) {
                    Toast.makeText(TripDetailActivity.this,
                            "Введіть коректне число", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Скасувати", null);
        builder.show();
    }

    private void showLargeOdometerDifferenceWarning(final Integer startOdometer, Integer currentOdometer) {
        int difference = startOdometer - currentOdometer;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Велика різниця в пробігу");
        
        String message = "Увага! Виявлено велику різницю:\n\n" +
                "Поточний пробіг: " + currentOdometer + "км\n" +
                "Введений пробіг: " + startOdometer + "км\n" +
                "Різниця: " + difference + "км\n\n" +
                "Це може бути помилкою вводу.\n" +
                "Ви впевнені, що хочете продовжити?";
        
        builder.setMessage(message);
        
        builder.setPositiveButton("Так, продовжити", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startTripWithOdometer(startOdometer);
            }
        });
        
        builder.setNegativeButton("Ні, виправити", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showStartTripDialog();
            }
        });
        
        builder.setCancelable(false);
        builder.show();
    }

    private void startTripWithOdometer(Integer startOdometer) {
        
        if (!btnStartTrip.isEnabled()) {
            Log.w(TAG, "Спроба повторного старту поїздки заблокована");
            return;
        }
        
        if (!ApiClient.isNetworkAvailable()) {
            Toast.makeText(this, "Немає підключення до інтернету", Toast.LENGTH_LONG).show();
            return;
        }

        btnStartTrip.setEnabled(false);
        showLoading();
        
        Log.i(TAG, "Запуск поїздки " + tripId + "з пробігом " + startOdometer + "км");

        TripStartRequest request = new TripStartRequest(startOdometer);

        ApiClient.getApiService().startTripWithOdometer(tripId, request).enqueue(new Callback<ApiResponse<Trip>>() {
            @Override
            public void onResponse(Call<ApiResponse<Trip>> call, Response<ApiResponse<Trip>> response) {
                hideLoading();
                btnStartTrip.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Trip> apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        Toast.makeText(TripDetailActivity.this,
                                "Виїзд зафіксовано!\nПробіг: " + startOdometer + "км", 
                                Toast.LENGTH_LONG).show();

                        if (apiResponse.hasData()) {
                            currentTrip = apiResponse.getData();
                        }

                        loadTripDetails();

                        setResult(RESULT_OK); 
                    } else {
                        showError("Помилка фіксації виїзду: " + apiResponse.getMessage());
                    }
                } else {
                    showError("Помилка сервера: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Trip>> call, Throwable t) {
                hideLoading();
                btnStartTrip.setEnabled(true);
                showError("Помилка підключення: " + t.getMessage());
            }
        });
    }

    private void showFinishTripDialog() {
        if (currentTrip != null && currentTrip.hasStartOdometer()) {
            showOdometerInputDialog();
        } else {
            showManualDistanceInputDialog();
        }
    }

    private void showOdometerInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Приїзд водія");

        String message = "Водій повернувся\n\n" +
                "Пробіг при виїзді: " + currentTrip.getActualStartOdometer() + "км\n\n" +
                "Введіть показники одометра при приїзді:";
        builder.setMessage(message);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Наприклад: 150000");
        builder.setView(input);

        builder.setPositiveButton("Зафіксувати приїзд", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String inputText = input.getText().toString().trim();
                if (inputText.isEmpty()) {
                    Toast.makeText(TripDetailActivity.this,
                            "Введіть показники одометра", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    Integer endOdometer = Integer.parseInt(inputText);

                    if (endOdometer <= 0) {
                        Toast.makeText(TripDetailActivity.this,
                                "Показники одометра повинні бути більше 0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (endOdometer <= currentTrip.getActualStartOdometer()) {
                        Toast.makeText(TripDetailActivity.this,
                                "Помилка!\nКінцевий пробіг (" + endOdometer + "км) повинен бути більшим за початковий (" +
                                        currentTrip.getActualStartOdometer() + "км)",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    int distance = endOdometer - currentTrip.getActualStartOdometer();
                    if (distance > 1000) {
                        Toast.makeText(TripDetailActivity.this,
                                "Увага! Відстань " + distance + "км здається занадто великою. Перевірте дані.",
                                Toast.LENGTH_LONG).show();
                        
                    }

                    showConfirmationDialog(endOdometer, distance);

                } catch (NumberFormatException e) {
                    Toast.makeText(TripDetailActivity.this,
                            "Введіть коректне число", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Скасувати", null);
        builder.show();
    }

    private void showManualDistanceInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Фактична відстань");
        builder.setMessage("Введіть фактично пройдену відстань у кілометрах:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Наприклад: 25.5");
        builder.setView(input);

        builder.setPositiveButton("Завершити поїздку", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String inputText = input.getText().toString().trim();
                if (inputText.isEmpty()) {
                    Toast.makeText(TripDetailActivity.this,
                            "Введіть відстань", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    BigDecimal distance = new BigDecimal(inputText);

                    if (distance.compareTo(BigDecimal.ZERO) <= 0) {
                        Toast.makeText(TripDetailActivity.this,
                                "Відстань має бути більшою за 0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (distance.compareTo(BigDecimal.valueOf(Constants.MAX_TRIP_DISTANCE)) > 0) {
                        Toast.makeText(TripDetailActivity.this,
                                "Відстань не може бути більшою " + Constants.MAX_TRIP_DISTANCE + "км",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    finishTripWithDistance(distance);

                } catch (NumberFormatException e) {
                    Toast.makeText(TripDetailActivity.this,
                            "Введіть коректне число", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Скасувати", null);
        builder.show();
    }

    private void showConfirmationDialog(Integer endOdometer, int distance) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Підтвердження даних");

        String message = "Пробіг при виїзді: " + currentTrip.getActualStartOdometer() + "км\n" +
                "Пробіг при приїзді: " + endOdometer + "км\n" +
                "\n" +
                "Пройдено: " + distance + "км\n\n" +
                "Зафіксувати приїзд?";
        builder.setMessage(message);

        builder.setPositiveButton("Підтвердити", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finishTripWithOdometer(endOdometer);
            }
        });

        builder.setNegativeButton("Відміна", null);
        builder.show();
    }

    private void finishTripWithOdometer(Integer endOdometer) {
        TripFinishRequest request = new TripFinishRequest(endOdometer);
        finishTrip(request);
    }

    private void finishTripWithDistance(BigDecimal distance) {
        TripFinishRequest request = new TripFinishRequest(distance);
        finishTrip(request);
    }

    private void finishTrip(TripFinishRequest request) {
        
        if (!btnFinishTrip.isEnabled()) {
            Log.w(TAG, "Спроба повторного завершення поїздки заблокована");
            return;
        }
        
        if (!ApiClient.isNetworkAvailable()) {
            Toast.makeText(this, "Немає підключення до Інтернету", Toast.LENGTH_LONG).show();
            return;
        }

        btnFinishTrip.setEnabled(false);
        showLoading();
        
        Log.i(TAG, "Завершення поїздки " + tripId);

        ApiClient.getApiService().finishTrip(tripId, request).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                hideLoading();
                btnFinishTrip.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<String> apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        Toast.makeText(TripDetailActivity.this,
                                "Приїзд зафіксовано успішно!", Toast.LENGTH_LONG).show();

                        loadTripDetails();

                        setResult(RESULT_OK); 
                    } else {
                        showError("Помилка завершення поїздки: " + apiResponse.getMessage());
                    }
                } else {
                    showError("Помилка сервера при завершенні подорожі: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                hideLoading();
                btnFinishTrip.setEnabled(true);
                showError("Помилка підключення при завершенні подорожі: " + t.getMessage());
            }
        });
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, message);
    }

    private void showUpdateStartOdometerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Виправити початковий пробіг");
        
        String message = "Поточний початковий пробіг: " + currentTrip.getActualStartOdometer() + "км\n\n" +
                "Введіть правильне значення:";
        builder.setMessage(message);
        
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Наприклад: " + currentTrip.getActualStartOdometer());
        input.setText(String.valueOf(currentTrip.getActualStartOdometer()));
        input.selectAll();
        builder.setView(input);
        
        builder.setPositiveButton("Зберегти", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String inputText = input.getText().toString().trim();
                if (inputText.isEmpty()) {
                    Toast.makeText(TripDetailActivity.this,
                            "Введіть значення пробігу", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                try {
                    Integer newStartOdometer = Integer.parseInt(inputText);
                    
                    if (newStartOdometer <= 0) {
                        Toast.makeText(TripDetailActivity.this,
                                "Пробіг повинен бути більше 0", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (currentTrip.hasEndOdometer() && newStartOdometer >= currentTrip.getActualEndOdometer()) {
                        Toast.makeText(TripDetailActivity.this,
                                "Початковий пробіг повинен бути меншим за кінцевий (" +
                                        currentTrip.getActualEndOdometer() + "км)",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    
                    updateStartOdometer(newStartOdometer);
                    
                } catch (NumberFormatException e) {
                    Toast.makeText(TripDetailActivity.this,
                            "Введіть коректне число", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        builder.setNegativeButton("Скасувати", null);
        builder.show();
    }
    
    private void updateStartOdometer(Integer newStartOdometer) {
        if (!ApiClient.isNetworkAvailable()) {
            Toast.makeText(this, "Немає підключення до інтернету", Toast.LENGTH_LONG).show();
            return;
        }
        
        showLoading();
        Log.i(TAG, "Оновлення початкового пробігу поїздки " + tripId + ": " + newStartOdometer + "км");
        
        ApiClient.getApiService().updateTripStartOdometer(tripId, newStartOdometer).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                hideLoading();
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<String> apiResponse = response.body();
                    
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(TripDetailActivity.this,
                                "Початковий пробіг оновлено: " + newStartOdometer + "км",
                                Toast.LENGTH_LONG).show();
                        
                        loadTripDetails();
                    } else {
                        showError("Помилка оновлення пробігу: " + apiResponse.getMessage());
                    }
                } else {
                    showError("Помилка сервера: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                hideLoading();
                showError("Помилка підключення: " + t.getMessage());
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

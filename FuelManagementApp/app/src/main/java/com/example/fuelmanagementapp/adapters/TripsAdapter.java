package com.example.fuelmanagementapp.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fuelmanagementapp.R;
import com.example.fuelmanagementapp.models.Trip;
import com.example.fuelmanagementapp.utils.Constants;
import com.example.fuelmanagementapp.utils.DateUtils;

import java.math.BigDecimal;
import java.util.List;

public class TripsAdapter extends RecyclerView.Adapter<TripsAdapter.TripViewHolder> {

    private static final String TAG = "TripsAdapter";

    private List<Trip> trips;
    private OnTripClickListener listener;

    public interface OnTripClickListener {
        void onTripClick(Trip trip);
    }

    public TripsAdapter(List<Trip> trips, OnTripClickListener listener) {
        this.trips = trips;
        this.listener = listener;
        Log.d(TAG, "TripsAdapter створено з " + (trips != null ? trips.size() : 0) + "поїздками");
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trip, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        if (trips != null && position < trips.size()) {
            Trip trip = trips.get(position);
            holder.bind(trip, listener);
        }
    }

    @Override
    public int getItemCount() {
        return trips != null ? trips.size() : 0;
    }

    public void updateTrips(List<Trip> newTrips) {
        this.trips = newTrips;
        notifyDataSetChanged();
        Log.d(TAG, "Список поїздок оновлено. Кількість: " + (newTrips != null ? newTrips.size() : 0));
    }

    static class TripViewHolder extends RecyclerView.ViewHolder {
        private static final String TAG = "TripViewHolder";

        private TextView tvTripNumber;
        private TextView tvRoute;
        private TextView tvStatus;
        private TextView tvVehicle;
        private TextView tvDriver;
        private TextView tvDistance;
        private TextView tvTime;
        private View statusIndicator;
        private View itemView;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            tvTripNumber = itemView.findViewById(R.id.tv_trip_number);
            tvRoute = itemView.findViewById(R.id.tv_route);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvVehicle = itemView.findViewById(R.id.tv_vehicle);
            tvDriver = itemView.findViewById(R.id.tv_driver);
            tvDistance = itemView.findViewById(R.id.tv_distance);
            tvTime = itemView.findViewById(R.id.tv_time);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
        }

        public void bind(Trip trip, OnTripClickListener listener) {
            try {
                Context context = itemView.getContext();

                Log.d(TAG, "Відображення поїздки: " + trip.getId() + ", статус: " + trip.getStatus());

                String tripNumber = trip.getTripDisplayNumber();
                tvTripNumber.setText(tripNumber);

                String route = trip.getRouteInfo();
                tvRoute.setText(route);

                String status = trip.getStatusDisplayText();
                tvStatus.setText(status);

                int statusColor = getStatusColor(context, trip);
                tvStatus.setTextColor(statusColor);
                if (statusIndicator != null) {
                    statusIndicator.setBackgroundColor(statusColor);
                }

                String vehicleInfo = trip.getVehicleInfo();
                tvVehicle.setText("" + vehicleInfo);
                
                String driverInfo = trip.getDriverDisplayName();
                tvDriver.setText("" + driverInfo);

                String distanceText = formatDistanceInfo(trip);
                tvDistance.setText(distanceText);

                String timeText = formatTimeInfo(trip);
                tvTime.setText(timeText);

                itemView.setOnClickListener(v -> {
                    
                    v.setEnabled(false);
                    v.postDelayed(() -> v.setEnabled(true), 1000);
                    
                    Log.d(TAG, "Обрано поїздку: " + trip.getId());
                    if (listener != null) {
                        listener.onTripClick(trip);
                    }
                });

                itemView.setOnLongClickListener(v -> {
                    return true;
                });

            } catch (Exception e) {
                Log.e(TAG, "Помилка під час відображення поїздки: " + trip.getId(), e);

                tvTripNumber.setText("Поїздка #" + (trip.getId() != null ? trip.getId() : "?"));
                tvRoute.setText("Помилка завантаження даних");
                tvStatus.setText("Невідомо");
                tvVehicle.setText("Невідомо");
                tvDistance.setText("Не вказано");
                tvTime.setText("Не вказано");
            }
        }

        private int getStatusColor(Context context, Trip trip) {
            try {
                String status = trip.getStatus();
                if (status == null) {
                    return ContextCompat.getColor(context, android.R.color.darker_gray);
                }

                switch (status.toLowerCase()) {
                    case Constants.TRIP_STATUS_CREATED:
                    case Constants.TRIP_STATUS_ASSIGNED:
                        return ContextCompat.getColor(context, R.color.secondary);
                    case Constants.TRIP_STATUS_STARTED:
                        return ContextCompat.getColor(context, R.color.primary);
                    case Constants.TRIP_STATUS_PAUSED:
                        return ContextCompat.getColor(context, R.color.warning);
                    case Constants.TRIP_STATUS_COMPLETED:
                        return ContextCompat.getColor(context, R.color.muted);
                    case Constants.TRIP_STATUS_CANCELLED:
                        return ContextCompat.getColor(context, R.color.error);
                    default:
                        return ContextCompat.getColor(context, R.color.muted);
                }
            } catch (Exception e) {
                Log.e(TAG, "Не вдалося визначити колір статусу", e);
                return ContextCompat.getColor(context, R.color.muted);
            }
        }

        private String formatDistanceInfo(Trip trip) {
            try {
                BigDecimal plannedDistance = trip.getPlannedDistance();
                BigDecimal actualDistance = trip.getActualDistance();

                if (actualDistance != null && actualDistance.compareTo(BigDecimal.ZERO) > 0) {
                    return "Фактичний: " + actualDistance + "км";
                } else if (plannedDistance != null && plannedDistance.compareTo(BigDecimal.ZERO) > 0) {
                    return "Плановий: " + plannedDistance + "км";
                } else {
                    return "Відстань не вказано";
                }
            } catch (Exception e) {
                Log.e(TAG, "Помилка під час форматування відстані", e);
                return "Помилка даних";
            }
        }

        private String formatTimeInfo(Trip trip) {
            try {
                StringBuilder timeText = new StringBuilder();

                String actualStartTime = trip.getActualStartTime();
                String actualEndTime = trip.getActualEndTime();
                String plannedStartTime = trip.getPlannedStartTime();

                if (actualStartTime != null && !actualStartTime.trim().isEmpty()) {
                    String formattedStart = DateUtils.formatDisplayDateTime(actualStartTime);
                    if (formattedStart != null && !formattedStart.isEmpty()) {
                        timeText.append("Почато: ").append(formattedStart);

                        if (actualEndTime != null && !actualEndTime.trim().isEmpty()) {
                            String formattedEnd = DateUtils.formatDisplayDateTime(actualEndTime);
                            if (formattedEnd != null && !formattedEnd.isEmpty()) {
                                timeText.append("\n Закінчено: ").append(formattedEnd);
                            }
                        }
                    }
                } else if (plannedStartTime != null && !plannedStartTime.trim().isEmpty()) {
                    String formattedPlanned = DateUtils.formatDisplayDateTime(plannedStartTime);
                    if (formattedPlanned != null && !formattedPlanned.isEmpty()) {
                        timeText.append("Заплановано: ").append(formattedPlanned);
                    }
                }

                String result = timeText.toString();
                return result.isEmpty() ? "Час не вказаний" : result;

            } catch (Exception e) {
                Log.e(TAG, "Помилка під час форматування часу", e);
                return "Помилка";
            }
        }
    }
}

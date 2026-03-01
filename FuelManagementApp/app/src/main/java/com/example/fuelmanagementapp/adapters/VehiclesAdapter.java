package com.example.fuelmanagementapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fuelmanagementapp.R;
import com.example.fuelmanagementapp.models.Vehicle;

import java.util.List;

public class VehiclesAdapter extends RecyclerView.Adapter<VehiclesAdapter.VehicleViewHolder> {

    private List<Vehicle> vehicles;
    private OnVehicleClickListener listener;

    public interface OnVehicleClickListener {
        void onVehicleClick(Vehicle vehicle);
    }

    public VehiclesAdapter(List<Vehicle> vehicles, OnVehicleClickListener listener) {
        this.vehicles = vehicles;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VehicleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vehicle, parent, false);
        return new VehicleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VehicleViewHolder holder, int position) {
        Vehicle vehicle = vehicles.get(position);
        holder.bind(vehicle, listener);
    }

    @Override
    public int getItemCount() {
        return vehicles.size();
    }

    static class VehicleViewHolder extends RecyclerView.ViewHolder {
        private TextView tvLicensePlate;
        private TextView tvModel;
        private TextView tvOdometer;
        private View itemView;

        public VehicleViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            tvLicensePlate = itemView.findViewById(R.id.tv_vehicle_license_plate);
            tvModel = itemView.findViewById(R.id.tv_vehicle_model);
            tvOdometer = itemView.findViewById(R.id.tv_vehicle_odometer);
        }

        public void bind(Vehicle vehicle, OnVehicleClickListener listener) {
            tvLicensePlate.setText(vehicle.getLicensePlate() != null ? vehicle.getLicensePlate() : "Невідомо");

            if (vehicle.getModel() != null && !vehicle.getModel().isEmpty()) {
                tvModel.setText(vehicle.getModel());
                tvModel.setVisibility(View.VISIBLE);
            } else {
                tvModel.setVisibility(View.GONE);
            }

            if (vehicle.getCurrentOdometer() != null) {
                tvOdometer.setText("Пробіг: " + vehicle.getCurrentOdometer() + "км");
                tvOdometer.setVisibility(View.VISIBLE);
            } else {
                tvOdometer.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onVehicleClick(vehicle);
                }
            });
        }
    }
}

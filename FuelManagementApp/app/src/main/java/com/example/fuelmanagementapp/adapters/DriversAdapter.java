package com.example.fuelmanagementapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fuelmanagementapp.R;
import com.example.fuelmanagementapp.models.Driver;

import java.util.List;

public class DriversAdapter extends RecyclerView.Adapter<DriversAdapter.DriverViewHolder> {

    private List<Driver> drivers;
    private OnDriverClickListener listener;

    public interface OnDriverClickListener {
        void onDriverClick(Driver driver);
    }

    public DriversAdapter(List<Driver> drivers, OnDriverClickListener listener) {
        this.drivers = drivers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DriverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_driver, parent, false);
        return new DriverViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DriverViewHolder holder, int position) {
        Driver driver = drivers.get(position);
        holder.bind(driver, listener);
    }

    @Override
    public int getItemCount() {
        return drivers.size();
    }

    static class DriverViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName;
        private TextView tvPhone;
        private TextView tvLicense;
        private View itemView;

        public DriverViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            tvName = itemView.findViewById(R.id.tv_driver_name);
            tvPhone = itemView.findViewById(R.id.tv_driver_phone);
            tvLicense = itemView.findViewById(R.id.tv_driver_license);
        }

        public void bind(Driver driver, OnDriverClickListener listener) {
            tvName.setText(driver.getFullName());

            if (driver.getPhone() != null && !driver.getPhone().isEmpty()) {
                tvPhone.setText("" + driver.getPhone());
                tvPhone.setVisibility(View.VISIBLE);
            } else {
                tvPhone.setVisibility(View.GONE);
            }

            if (driver.getLicenseNumber() != null && !driver.getLicenseNumber().isEmpty()) {
                tvLicense.setText("" + driver.getLicenseNumber());
                tvLicense.setVisibility(View.VISIBLE);
            } else {
                tvLicense.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDriverClick(driver);
                }
            });
        }
    }
}

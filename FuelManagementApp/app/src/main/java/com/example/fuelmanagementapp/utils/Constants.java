package com.example.fuelmanagementapp.utils;

import com.example.fuelmanagementapp.BuildConfig;

public class Constants {
    
    public static final String BASE_URL = BuildConfig.API_BASE_URL;  
     
    public static final int CONNECT_TIMEOUT = 45; 
    public static final int READ_TIMEOUT = 45;    
    public static final int WRITE_TIMEOUT = 45;   

    public static final String PREFS_NAME = "FuelManagementPrefs";
    public static final String KEY_SELECTED_DRIVER_ID = "selected_driver_id";
    public static final String KEY_SELECTED_DRIVER_NAME = "selected_driver_name";
    public static final String KEY_API_BASE_URL = "api_base_url";

    public static final int REQUEST_CODE_SELECT_DRIVER = 1001;
    public static final int REQUEST_CODE_TRIP_DETAIL = 1002;
    public static final int REQUEST_CODE_CREATE_TRIP = 1003;

    public static final String EXTRA_DRIVER_ID = "driver_id";
    public static final String EXTRA_DRIVER_NAME = "driver_name";
    public static final String EXTRA_TRIP_ID = "trip_id";
    public static final String EXTRA_TRIP = "trip";
    public static final String EXTRA_VEHICLE_INFO = "vehicle_info";
    public static final String EXTRA_DRIVER_FULL_NAME = "driver_full_name";

    public static final String TRIP_STATUS_CREATED = "created";
    public static final String TRIP_STATUS_ASSIGNED = "assigned";
    public static final String TRIP_STATUS_STARTED = "started";
    public static final String TRIP_STATUS_PAUSED = "paused";
    public static final String TRIP_STATUS_COMPLETED = "completed";
    public static final String TRIP_STATUS_CANCELLED = "cancelled";

    public static final String TRIP_TYPE_ONE_WAY = "one_way";
    public static final String TRIP_TYPE_ROUND_TRIP = "round_trip";

    public static final String DRIVER_STATUS_ACTIVE = "active";
    public static final String DRIVER_STATUS_VACATION = "vacation";
    public static final String DRIVER_STATUS_SICK = "sick";
    public static final String DRIVER_STATUS_INACTIVE = "inactive";

    public static final double MAX_TRIP_DISTANCE = 2000.0;
    public static final double MIN_TRIP_DISTANCE = 0.1;
}

package com.example.fuelmanagementapp.network;

import com.example.fuelmanagementapp.models.ApiResponse;
import com.example.fuelmanagementapp.models.Driver;
import com.example.fuelmanagementapp.models.Trip;
import com.example.fuelmanagementapp.models.Vehicle;
import com.example.fuelmanagementapp.network.dto.TripCreateRequest;
import com.example.fuelmanagementapp.network.dto.TripFinishRequest;
import com.example.fuelmanagementapp.network.dto.TripStartRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @GET("drivers")
    Call<ApiResponse<List<Driver>>> getActiveDrivers();

    @GET("vehicles")
    Call<ApiResponse<List<Vehicle>>> getActiveVehicles();

    @POST("trips/create")
    Call<ApiResponse<Trip>> createTrip(@Body TripCreateRequest request);

    @GET("trips/{driverId}")
    Call<ApiResponse<List<Trip>>> getDriverTrips(@Path("driverId") Integer driverId);

    @GET("trips/active/all")
    Call<ApiResponse<List<Trip>>> getAllActiveTrips();

    @POST("trips/{tripId}/start")
    Call<ApiResponse<Trip>> startTripWithOdometer(@Path("tripId") Integer tripId,
                                                   @Body TripStartRequest request);

    @POST("trips/{tripId}/finish")
    Call<ApiResponse<String>> finishTrip(@Path("tripId") Integer tripId,
                                         @Body TripFinishRequest request);

    @GET("trips/info/{tripId}")
    Call<ApiResponse<Trip>> getTripInfo(@Path("tripId") Integer tripId);

    @GET("trips/{tripId}/odometer")
    Call<ApiResponse<Integer>> getCurrentOdometer(@Path("tripId") Integer tripId);

    @PUT("trips/{tripId}/odometer/start")
    Call<ApiResponse<String>> updateTripStartOdometer(@Path("tripId") Integer tripId,
                                                       @Query("startOdometer") Integer startOdometer);

    @GET("health")
    Call<ApiResponse<String>> healthCheck();
}
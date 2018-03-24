package com.mihir.paginationdemo;

import android.app.Application;
import android.content.Context;

import com.mihir.paginationdemo.api.ApiClient;
import com.mihir.paginationdemo.api.ApiInterface;

import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Mihir on 14-09-2017.
 */

public class UserListingApplication extends Application {

    private ApiInterface vehicleService;
    private Scheduler scheduler;

    private static UserListingApplication get(Context context) {
        return (UserListingApplication) context.getApplicationContext();
    }

    public static UserListingApplication create(Context context) {
        return UserListingApplication.get(context);
    }

    public ApiInterface getVehicleService() {
        if (vehicleService == null) {
            vehicleService = ApiClient.create();
        }

        return vehicleService;
    }

    public Scheduler subscribeScheduler() {
        if (scheduler == null) {
            scheduler = Schedulers.io();
        }

        return scheduler;
    }

    public void setVehicleService(ApiInterface vehicleService) {
        this.vehicleService = vehicleService;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }
}

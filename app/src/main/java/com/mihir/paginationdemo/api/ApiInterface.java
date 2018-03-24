package com.mihir.paginationdemo.api;

import com.mihir.paginationdemo.models.UserProfile;
import com.mihir.paginationdemo.models.UsersResponse;

import io.reactivex.Observable;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;

/**
 * Created by Mihir on 14-09-2017.
 */

public interface ApiInterface {
    @GET("search/users?")
    Observable<UsersResponse> getUsers(
            @Query("q") CharSequence user,
            @Query("page") int page
    );

    @GET
    Observable<UserProfile> getUserProfile(@Url String user);
}

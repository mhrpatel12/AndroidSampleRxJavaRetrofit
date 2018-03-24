package com.mihir.paginationdemo;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mihir.paginationdemo.api.ApiClient;
import com.mihir.paginationdemo.api.ApiInterface;
import com.mihir.paginationdemo.models.UserList;
import com.mihir.paginationdemo.models.UsersResponse;
import com.mihir.paginationdemo.utils.PaginationAdapterCallback;
import com.mihir.paginationdemo.utils.PaginationScrollListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * Created by Mihir on 14-09-2017.
 */

public class HomeActivity extends AppCompatActivity implements PaginationAdapterCallback {

    private static final String TAG = "HomeActivity";
    private static final String LIST_STATE_KEY = "RecyclerViewInstance";

    PaginationAdapter adapter;
    LinearLayoutManager linearLayoutManager;

    RecyclerView rv;
    ProgressBar progressBar;
    LinearLayout errorLayout;
    Button btnRetry;
    TextView txtError;

    private static final int PAGE_START = 1;

    private boolean isLoading = false;
    private boolean isLastPage = false;
    // limiting to 5 for this tutorial, since total pages in actual API is very large. Feel free to modify.
    private int TOTAL_PAGES = 0;
    private int currentPage = PAGE_START;

    private List<UserList> userList = new ArrayList<>();

    private android.widget.SearchView searchUser;

    private ApiInterface apiInterface;
    private Parcelable mListState;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private UserListingApplication userListingApplication;
    private ApiInterface anInterface;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        searchUser = (android.widget.SearchView) toolbar.findViewById(R.id.searchUser);
        searchUser.setOnQueryTextListener(new android.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mHandler.removeCallbacksAndMessages(null);

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!isLoading) {
                            progressBar.setVisibility(View.VISIBLE);
                            loadFirstPage();
                        }
                    }
                }, 300);
                return true;
            }
        });

        userListingApplication = UserListingApplication.create(HomeActivity.this);
        anInterface = userListingApplication.getVehicleService();

        rv = (RecyclerView) findViewById(R.id.main_recycler);
        progressBar = (ProgressBar) findViewById(R.id.main_progress);
        errorLayout = (LinearLayout) findViewById(R.id.error_layout);
        btnRetry = (Button) findViewById(R.id.error_btn_retry);
        txtError = (TextView) findViewById(R.id.error_txt_cause);

        adapter = new PaginationAdapter(this);

        linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        rv.setLayoutManager(linearLayoutManager);
        rv.setItemAnimator(new DefaultItemAnimator());
        rv.setAdapter(adapter);

        rv.addOnScrollListener(new PaginationScrollListener(linearLayoutManager) {
            @Override
            protected void loadMoreItems() {
                isLoading = true;
                currentPage += 1;

                loadNextPage();
            }

            @Override
            public int getTotalPageCount() {
                return TOTAL_PAGES;
            }

            @Override
            public boolean isLastPage() {
                return isLastPage;
            }

            @Override
            public boolean isLoading() {
                return isLoading;
            }
        });

        //init service and load data
        apiInterface = ApiClient.getClient().create(ApiInterface.class);

        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadFirstPage();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mListState != null) {
            linearLayoutManager.onRestoreInstanceState(mListState);
        }
    }

    private void loadFirstPage() {
        Log.d(TAG, "loadFirstPage: ");

        // To ensure list is visible when retry button in error view is clicked
        hideErrorView();

        Disposable disposable = anInterface.getUsers(searchUser.getQuery(), currentPage)
                .subscribeOn(userListingApplication.subscribeScheduler())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<UsersResponse>() {
                    @Override
                    public void accept(UsersResponse vehicleResponse) throws Exception {
                        userList.clear();
                        adapter.clear();
                        adapter.notifyDataSetChanged();
                        TOTAL_PAGES = vehicleResponse.getTotalCount();
                        hideErrorView();
                        progressBar.setVisibility(View.GONE);
                        changeVehicleDataSet(vehicleResponse.getUserList());
                        if (currentPage <= TOTAL_PAGES) adapter.addLoadingFooter();
                        else isLastPage = true;

                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        showErrorView(throwable);
                    }
                });
        compositeDisposable.add(disposable);
    }

    private void loadNextPage() {
        Log.d(TAG, "loadNextPage: " + currentPage);

        Disposable disposable = anInterface.getUsers(searchUser.getQuery(), currentPage)
                .subscribeOn(userListingApplication.subscribeScheduler())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<UsersResponse>() {
                    @Override
                    public void accept(UsersResponse vehicleResponse) throws Exception {
                        adapter.removeLoadingFooter();
                        isLoading = false;
                        changeVehicleDataSet(vehicleResponse.getUserList());
                        if (currentPage != TOTAL_PAGES) adapter.addLoadingFooter();
                        else isLastPage = true;
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        showErrorView(throwable);
                    }
                });
        compositeDisposable.add(disposable);
    }

    private void changeVehicleDataSet(List<UserList> response) {
        userList.addAll(response);
        adapter.addAll(response);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void retryPageLoad() {
        loadNextPage();
    }


    /**
     * @param throwable required for {@link #fetchErrorMessage(Throwable)}
     * @return
     */
    private void showErrorView(Throwable throwable) {

        if (searchUser.getQuery().length() > 0) {
            if (errorLayout.getVisibility() == View.GONE) {
                errorLayout.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                txtError.setText(fetchErrorMessage(throwable));
                clearList();
            }
        } else {
            progressBar.setVisibility(View.GONE);
            clearList();
        }
    }

    private void clearList() {
        userList.clear();
        adapter.clear();
        adapter.notifyDataSetChanged();
    }

    /**
     * @param throwable to identify the type of error
     * @return appropriate error message
     */
    private String fetchErrorMessage(Throwable throwable) {
        String errorMsg = getResources().getString(R.string.error_msg_unknown);

        if (!isNetworkConnected()) {
            errorMsg = getResources().getString(R.string.error_msg_no_internet);
        } else if (throwable instanceof TimeoutException) {
            errorMsg = getResources().getString(R.string.error_msg_timeout);
        }

        return errorMsg;
    }

    // Helpers -------------------------------------------------------------------------------------


    private void hideErrorView() {
        if (errorLayout.getVisibility() == View.VISIBLE) {
            errorLayout.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Remember to add android.permission.ACCESS_NETWORK_STATE permission.
     *
     * @return
     */
    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }

    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);

        // Save list state
        mListState = linearLayoutManager.onSaveInstanceState();
        state.putParcelable(LIST_STATE_KEY, mListState);
    }

    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        // Retrieve list state and list/item positions
        if (state != null)
            mListState = state.getParcelable(LIST_STATE_KEY);
    }

}

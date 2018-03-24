package com.mihir.paginationdemo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.DrawableRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.mihir.paginationdemo.api.ApiInterface;
import com.mihir.paginationdemo.models.UserList;
import com.mihir.paginationdemo.models.UserProfile;
import com.mihir.paginationdemo.utils.PaginationAdapterCallback;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

/**
 * Created by Mihir on 14-09-2017.
 */

public class PaginationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // View Types
    private static final int ITEM = 0;
    private static final int LOADING = 1;

    private List<UserList> listVideoPosts;
    private Context context;

    private boolean isLoadingAdded = false;
    private boolean retryPageLoad = false;

    private PaginationAdapterCallback mCallback;

    private String errorMsg;
    private UserListingApplication userListingApplication;
    private ApiInterface apiInterface;

    public PaginationAdapter(Context context) {
        this.context = context;
        this.mCallback = (PaginationAdapterCallback) context;
        listVideoPosts = new ArrayList<>();
        userListingApplication = UserListingApplication.create(context);
        apiInterface = userListingApplication.getVehicleService();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = null;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case ITEM:
                View viewItem = inflater.inflate(R.layout.item_list, parent, false);
                viewHolder = new VideoPostVH(viewItem);
                break;
            case LOADING:
                View viewLoading = inflater.inflate(R.layout.item_progress, parent, false);
                viewHolder = new LoadingVH(viewLoading);
                break;
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final UserList userProfile = listVideoPosts.get(position); // UserList

        switch (getItemViewType(position)) {

            case ITEM:
                final VideoPostVH user = (VideoPostVH) holder;

                apiInterface.getUserProfile("users/" + userProfile.getLogin()).subscribeOn(userListingApplication.subscribeScheduler())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<UserProfile>() {
                            @Override
                            public void accept(UserProfile vehicleResponse) throws Exception {
                                user.txtFirstName.setText(vehicleResponse.getName());
                                user.txtLastName.setText(vehicleResponse.getFollowers() + "");
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                Log.e("==============", throwable.getMessage());
                            }
                        });

                loadImage(userProfile.getAvatarImageURL())
                        .listener(new RequestListener<String, GlideDrawable>() {
                            @Override
                            public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                                user.mProgress.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                                // image ready, hide progress now
                                user.mProgress.setVisibility(View.GONE);
                                return false;   // return false if you want Glide to handle everything else.
                            }
                        })
                        .into(user.mPosterImg);
                break;

            case LOADING:
                LoadingVH loadingVH = (LoadingVH) holder;

                if (retryPageLoad) {
                    loadingVH.mErrorLayout.setVisibility(View.VISIBLE);
                    loadingVH.mProgressBar.setVisibility(View.GONE);

                    loadingVH.mErrorTxt.setText(
                            errorMsg != null ?
                                    errorMsg :
                                    context.getString(R.string.error_msg_unknown));

                } else {
                    loadingVH.mErrorLayout.setVisibility(View.GONE);
                    loadingVH.mProgressBar.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    @Override
    public int getItemCount() {
        return listVideoPosts == null ? 0 : listVideoPosts.size();
    }

    @Override
    public int getItemViewType(int position) {
        return (position == listVideoPosts.size() - 1 && isLoadingAdded) ? LOADING : ITEM;
    }

    private DrawableRequestBuilder<String> loadImage(@NonNull String posterPath) {
        return Glide
                .with(context)
                .load(posterPath)
                .diskCacheStrategy(DiskCacheStrategy.ALL)   // cache both original & resized image
                .centerCrop()
                .crossFade();
    }


    /*
        Helpers - Pagination
   _________________________________________________________________________________________________
    */

    public void notifyChange() {
        notifyDataSetChanged();
    }

    public void add(UserList r) {
        listVideoPosts.add(r);
        notifyItemInserted(listVideoPosts.size() - 1);
    }

    public void addAll(List<UserList> moveVideoPosts) {
        for (UserList videoPost : moveVideoPosts) {
            add(videoPost);
        }
    }

    public void remove(UserList r) {
        int position = listVideoPosts.indexOf(r);
        if (position > -1) {
            listVideoPosts.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void clear() {
        isLoadingAdded = false;
        while (getItemCount() > 0) {
            remove(getItem(0));
        }
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }


    public void addLoadingFooter() {
        isLoadingAdded = true;
        add(new UserList());
    }

    public void removeLoadingFooter() {
        isLoadingAdded = false;

        int position = listVideoPosts.size() - 1;
        UserList videoPost = getItem(position);

        if (videoPost != null) {
            listVideoPosts.remove(position);
            notifyItemRemoved(position);
        }
    }

    public UserList getItem(int position) {
        return listVideoPosts.get(position);
    }

    /**
     * Displays Pagination retry footer view along with appropriate errorMsg
     *
     * @param show
     * @param errorMsg to display if page load fails
     */
    public void showRetry(boolean show, @Nullable String errorMsg) {
        retryPageLoad = show;
        notifyItemChanged(listVideoPosts.size() - 1);

        if (errorMsg != null) this.errorMsg = errorMsg;
    }


   /*
   View Holders
   _________________________________________________________________________________________________
    */

    /**
     * Main list's content ViewHolder
     */
    protected class VideoPostVH extends RecyclerView.ViewHolder {
        private TextView txtFirstName;
        private TextView txtLastName;
        private ImageView mPosterImg;
        private ProgressBar mProgress;

        public VideoPostVH(View itemView) {
            super(itemView);

            txtFirstName = (TextView) itemView.findViewById(R.id.firstName);
            txtLastName = (TextView) itemView.findViewById(R.id.lastName);
            mPosterImg = (ImageView) itemView.findViewById(R.id.post_thumbnail);
            mProgress = (ProgressBar) itemView.findViewById(R.id.image_progress);
        }
    }


    protected class LoadingVH extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ProgressBar mProgressBar;
        private ImageButton mRetryBtn;
        private TextView mErrorTxt;
        private LinearLayout mErrorLayout;

        public LoadingVH(View itemView) {
            super(itemView);

            mProgressBar = (ProgressBar) itemView.findViewById(R.id.loadmore_progress);
            mRetryBtn = (ImageButton) itemView.findViewById(R.id.loadmore_retry);
            mErrorTxt = (TextView) itemView.findViewById(R.id.loadmore_errortxt);
            mErrorLayout = (LinearLayout) itemView.findViewById(R.id.loadmore_errorlayout);

            mRetryBtn.setOnClickListener(this);
            mErrorLayout.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.loadmore_retry:
                case R.id.loadmore_errorlayout:

                    showRetry(false, null);
                    mCallback.retryPageLoad();

                    break;
            }
        }
    }

}

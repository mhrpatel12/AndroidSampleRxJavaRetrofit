package com.mihir.paginationdemo.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created by Mihir on 14-09-2017.
 */

public class UserList implements Serializable {
    @SerializedName("id")
    private int userID;
    @SerializedName("login")
    private String login;
    @SerializedName("url")
    private String profileUrl;
    @SerializedName("avatar_url")
    private String avatarImageURL;

    public UserList() {
    }

    public int getUserID() {
        return userID;
    }

    public String getLogin() {
        return login;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public String getAvatarImageURL() {
        return avatarImageURL;
    }
}

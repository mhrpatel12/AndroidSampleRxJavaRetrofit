package com.mihir.paginationdemo.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Mihir on 14-09-2017.
 */

public class UsersResponse {
    @SerializedName("total_count")
    public int totalCount;

    @SerializedName("items")
    public List<UserList> userList;

    public List<UserList> getUserList() {
        return userList;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public void setUserList(List<UserList> userList) {
        this.userList = userList;
    }

}

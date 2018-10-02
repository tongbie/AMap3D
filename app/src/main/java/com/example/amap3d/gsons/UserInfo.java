package com.example.amap3d.gsons;

public class UserInfo {

    private String userName;
    private String displayName;
    private String remark;
    private long time;

    public UserInfo(){}

    public void setUserInfo(UserInfo userInfo) {
        this.userName = userInfo.getUserName();
        this.displayName = userInfo.getDisplayName();
        this.remark = userInfo.getRemark();
        this.time = userInfo.getTime();
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}

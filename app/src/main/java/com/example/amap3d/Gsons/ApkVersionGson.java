package com.example.amap3d.Gsons;

/**
 * Created by BieTong on 2018/5/8.
 */

public class ApkVersionGson {
    private String id;
    private String updateTime;
    private String packageVersionName;
    private String packageVersionCode;
    private String description;
    private String minVersionCode;
    private String type;

    public String getId() {
        return id==null?"":id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUpdateTime() {
        return updateTime==null?"":updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getPackageVersionName() {
        return packageVersionName==null?"":packageVersionName;
    }

    public void setPackageVersionName(String packageVersionName) {
        this.packageVersionName = packageVersionName;
    }

    public int getPackageVersionCode() {
        if(packageVersionCode==null){
            return -404;
        }
        return Integer.parseInt(packageVersionCode);
    }

    public void setPackageVersionCode(String packageVersionCode) {
        this.packageVersionCode = packageVersionCode;
    }

    public String getDescription() {
        return description==null?"":description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getMinVersionCode() {
        if(minVersionCode==null){
            return -404;
        }
        return Integer.parseInt(minVersionCode);
    }

    public void setMinVersionCode(String minVersionCode) {
        this.minVersionCode = minVersionCode;
    }

    public String getType() {
        return type==null?"":type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

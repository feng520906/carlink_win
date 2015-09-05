package com.coopox.carlauncher.datamodel;

import android.provider.BaseColumns;
import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.baidu.location.BDLocation;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-9-2
 * 地理位置数据模型
 */
@Table(name = "LocationModel", id = BaseColumns._ID)
public class LocationModel extends Model {

    @Column(name = "City")
    public String city = "";

    @Column(name = "Address")
    public String address = "";

    @Column(name = "Altitude")
    public double altitude;

    @Column(name = "Latitude")
    public double latitude;

    @Column(name = "Longitude")
    public double longitude;

    @Column(name = "Timestamp")
    public long timestamp;

    public LocationModel() {
        timestamp = System.currentTimeMillis();
    }

    LocationModel(BDLocation location) {
        this();
        set(location);
    }

    void set(BDLocation location) {
        if (null != location) {
            city = location.getCity();

            if (location.hasAddr()) {
                address = location.getAddrStr();
            }
            altitude = location.getAltitude();
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }
    }
}

package com.coopox.carlauncher.datamodel;

import android.text.TextUtils;
import android.util.Log;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.coopox.carlauncher.activity.CarApplication;
import com.coopox.common.utils.Checker;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-9-2
 * 定位管理器，负责确定用户当前所在地理位置。另外也起到屏蔽百度接口的作用，为日后更换SDK提供灵活性。
 */
public enum LocationManager {
    INSTANCE;
    public interface LocationListener {
        void onLocationUpdate(LocationModel data);

        void onCityChanged(String oldCity, String newCity);
    }

    private static final String TAG = "LocationManager";
    // 每隔 30 秒钟刷新一次当前位置信息
    public static final int SCAN_INTERVAL = 1000 * 30;
    private LocationClient mLocationClient = null;
    private BDLocationListener mLocationListener = new CarLocationListener();
    private LocationClientOption mLocationOption = new LocationClientOption();
    private Set<LocationListener> mListeners;
    private LocationModel mLocation;

    LocationManager() {
        mListeners = new HashSet<LocationListener>(4);
        mLocation = new LocationModel();
        mLocationOption.setOpenGps(true);
        mLocationOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        mLocationOption.setIsNeedAddress(true);
        mLocationOption.setScanSpan(SCAN_INTERVAL);
        mLocationClient = new LocationClient(CarApplication.getAppContext());     //声明LocationClient类
        mLocationClient.setLocOption(mLocationOption);
        mLocationClient.registerLocationListener(mLocationListener);    //注册监听函数
    }

    public void registerLocationListener(LocationListener listener) {
        if (null != listener) {
            mListeners.add(listener);
        }
    }

    public void unregisterLocationListener(LocationListener listener) {
        if (null != listener) {
            mListeners.remove(listener);
        }
    }

    public void startLocation() {
        // 不要重复 Start 定位
        if (!mLocationClient.isStarted()) {
            mLocationClient.start();
        }
    }

    public void stopLocation() {
        mLocationClient.stop();
        clearLocationHistory();
    }

    // 留下最近的一条位置信息，其它的全部清除掉（视性能可能需要放在 I/O 线程）
    private void clearLocationHistory() {
        List<LocationModel> model =
                new Select().from(LocationModel.class).orderBy("Timestamp DESC").limit(1).execute();
        if (!Checker.isEmpty(model)) {
/*            ActiveAndroid.beginTransaction();
            try {
                for (int i = 1; i < models.size(); ++i) {
                    LocationModel model = models.get(i);
                    model.delete();
                }
                ActiveAndroid.setTransactionSuccessful();
            } finally {
                ActiveAndroid.endTransaction();
            }*/
            new Delete().from(LocationModel.class).where("_id <> ?", model.get(0).getId()).execute();
        }
    }

    // FIXME: 只能获取到开机前上次的最后位置，后续有更新无法感知
    public LocationModel getLastLocation() {
        if (null != mLocation && !TextUtils.isEmpty(mLocation.city)) {
            return mLocation;
        }
        List<LocationModel> model =
                new Select().from(LocationModel.class).orderBy("Timestamp").limit(1).execute();
        if (!Checker.isEmpty(model)) {
            return model.get(0);
        }
        return null;
    }

    // 该监听器会被三个进程（应用本身、remote、bd_service）调用，不过只有应用本身进程注册了
    // LocationListener，所以也不会有什么问题。
    private class CarLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            if (null != bdLocation) {
                String city = bdLocation.getCity();
                // 如果城市名都没有，视为无效定位（因为城市名要为导航等众多功能所用）
                int locType = bdLocation.getLocType();
                if (161/*网络定位*/ == locType
                        || 61/*GPS定位*/ == locType
                        || 66/*离线定位*/ == locType
                        || 65/*定位缓存*/ == locType) {
                    notifyLocationListeners(bdLocation);
                }
            }
        }
    }

    private void notifyLocationListeners(BDLocation location) {
        LocationModel currentLocation = new LocationModel(location);
        currentLocation.save();

        if (null == currentLocation.city) {
            currentLocation.city = mLocation.city;
        }
        boolean isCityChanged = (null != mLocation &&
                        null != mLocation.city &&
                        !mLocation.city.equals(currentLocation.city));
        for (LocationListener listener : mListeners) {
            if (null != listener) {
                if (isCityChanged) {
                    Log.d(TAG, String.format("City changed from %s to %s",
                            mLocation.city, currentLocation.city));
                    listener.onCityChanged(mLocation.city, currentLocation.city);
                }

                listener.onLocationUpdate(currentLocation);
            }
        }
        mLocation = currentLocation;
    }
}

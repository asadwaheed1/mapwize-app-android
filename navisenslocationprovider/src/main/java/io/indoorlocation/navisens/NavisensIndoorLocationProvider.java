package io.indoorlocation.navisens;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import com.navisens.motiondnaapi.MotionDna;
import com.navisens.motiondnaapi.MotionDnaApplication;
import com.navisens.motiondnaapi.MotionDnaInterface;

import java.util.Map;

import io.indoorlocation.core.IndoorLocation;
import io.indoorlocation.core.IndoorLocationProvider;
import io.indoorlocation.core.IndoorLocationProviderListener;

public class NavisensIndoorLocationProvider extends IndoorLocationProvider implements MotionDnaInterface, IndoorLocationProviderListener {

    private boolean mStarted = false;

    private MotionDnaApplication mMotionDna;
    private Context mContext;
    private Double mCurrentFloor = null;
    private String mNavisensKey;
    private IndoorLocationProvider mSourceProvider;
    private MotionDnaInterface navisensInterface;
    private boolean floorsSet = false;

    public NavisensIndoorLocationProvider(Context context, String navisensKey) {
        super();
        mContext = context;
        mMotionDna = new MotionDnaApplication(this);
        mNavisensKey = navisensKey;
    }

    public NavisensIndoorLocationProvider(Context context, IndoorLocationProvider sourceProvider, String navisensKey) {
        super();
        mContext = context;
        mMotionDna = new MotionDnaApplication(this);
        mSourceProvider = sourceProvider;
        mSourceProvider.addListener(this);
        mNavisensKey = navisensKey;
    }

    public void setNavisensInterface(MotionDnaInterface navisensInterface) {
        this.navisensInterface = navisensInterface;
    }

    public void setFloorsAndHeight(Map<Double, Double> floorsAndHeight) {
        if (floorsAndHeight == null || floorsAndHeight.size() < 1) {
            floorsSet = false;
            return;
        }
        floorsSet = true;
        for (Map.Entry<Double, Double> entry : floorsAndHeight.entrySet()) {
            mMotionDna.addFloorNumberAndHeight(entry.getKey().intValue(), entry.getValue());
        }
    }

    public void setIndoorLocation(IndoorLocation indoorLocation) {
        IndoorLocation il = new IndoorLocation(this.getName(), indoorLocation.getLatitude(), indoorLocation.getLongitude(), indoorLocation.getFloor(), indoorLocation.getTime());
        dispatchIndoorLocationChange(il);
        mCurrentFloor = indoorLocation.getFloor();
        mMotionDna.setLocationLatitudeLongitude(indoorLocation.getLatitude(), indoorLocation.getLongitude());
        mMotionDna.setHeadingMagInDegrees();
        mMotionDna.setFloorNumber(indoorLocation.getFloor().intValue());
    }

    public void setFloor(Double floor) {
        mMotionDna.setFloorNumber(floor.intValue());
    }

    @Override
    public boolean supportsFloor() {
        return true;
    }

    @Override
    public void start() {
        if (!mStarted) {
            mStarted = true;
            mMotionDna.runMotionDna(mNavisensKey);
            mMotionDna.setCallbackUpdateRateInMs(1000);
            mMotionDna.setPowerMode(MotionDna.PowerConsumptionMode.PERFORMANCE);
        }
    }

    @Override
    public void stop() {
        if (mStarted) {
            mStarted = false;
            mMotionDna.stop();
        }
    }

    @Override
    public boolean isStarted() {
        return mStarted;
    }

    @Override
    public void receiveMotionDna(MotionDna motionDna) {
        MotionDna.Location location = motionDna.getLocation();
        Double floor = new Double(motionDna.getLocation().floor);
        IndoorLocation indoorLocation = new IndoorLocation(getName(), location.globalLocation.latitude, location.globalLocation.longitude, floorsSet?floor:mCurrentFloor, System.currentTimeMillis());
        if (indoorLocation.getLatitude() != 0 && indoorLocation.getLongitude() != 0) {
            dispatchIndoorLocationChange(indoorLocation);
        }
    }

    @Override
    public void receiveNetworkData(MotionDna motionDna) {

    }

    @Override
    public void receiveNetworkData(MotionDna.NetworkCode networkCode, Map<String, ?> map) {

    }

    @Override
    public void reportError(MotionDna.ErrorCode errorCode, String s) {
        this.dispatchOnProviderError(new Error(errorCode.toString() + " " + s));
    }

    @Override
    public Context getAppContext() {
        return mContext.getApplicationContext();
    }

    @Override
    public PackageManager getPkgManager() {
        return mContext.getPackageManager();
    }

    @Override
    public void onProviderStarted() {
        this.dispatchOnProviderStarted();
    }

    @Override
    public void onProviderStopped() {
        this.dispatchOnProviderStopped();
    }

    @Override
    public void onProviderError(Error error) {
       dispatchOnProviderError(error);
    }

    @Override
    public void onIndoorLocationChange(IndoorLocation indoorLocation) {
        dispatchIndoorLocationChange(indoorLocation);
        mCurrentFloor = indoorLocation.getFloor();
        mMotionDna.setLocationLatitudeLongitude(indoorLocation.getLatitude(), indoorLocation.getLongitude());
        mMotionDna.setHeadingMagInDegrees();
        mMotionDna.setFloorNumber(indoorLocation.getFloor().intValue());
    }
}

package com.example.incivismenavigation.ui.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class SharedViewModel extends AndroidViewModel {
    private final MutableLiveData<String> currentAddress = new MutableLiveData<>();
    private final MutableLiveData<String> checkPermission = new MutableLiveData<>();
    private final MutableLiveData<String> buttonText = new MutableLiveData<>();
    private final MutableLiveData<Boolean> progressBar = new MutableLiveData<>();

    private OnPermissionRequestCallback permissionRequestCallback;

    @SuppressLint("StaticFieldLeak")
    private final Context mContext;
    private boolean mPermissionGranted = false;

    private boolean mTrackingLocation;
    private FusedLocationProviderClient mFusedLocationClient;

    public SharedViewModel(@NonNull Application application) {
        super(application);
        mContext = application.getApplicationContext();
    }

    void setFusedLocationClient(FusedLocationProviderClient mFusedLocationClient) {
        this.mFusedLocationClient = mFusedLocationClient;
    }

    public LiveData<String> getCurrentAddress() {
        return currentAddress;
    }

    public MutableLiveData<String> getButtonText() {
        return buttonText;
    }

    public MutableLiveData<Boolean> getProgressBar() {
        return progressBar;
    }

    LiveData<String> getCheckPermission() {
        return checkPermission;
    }

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult != null) {
                fetchAddress(locationResult.getLastLocation());
            }
        }
    };

    private LocationRequest getLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    public void switchTrackingLocation() {
        if (mPermissionGranted) {
            if (!mTrackingLocation) {
                startTrackingLocation();
            } else {
                stopTrackingLocation();
            }
        } else {
            requestLocationPermission();
        }
    }

    /*private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mPermissionGranted = true;
            startTrackingLocation();
        } else {
            ActivityCompat.requestPermissions((Activity) app, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }*/

    @SuppressLint("MissingPermission")
    private void startTrackingLocation() {
        mFusedLocationClient.requestLocationUpdates(getLocationRequest(), mLocationCallback, Looper.getMainLooper());

        currentAddress.postValue("Carregant...");
        progressBar.postValue(true);
        mTrackingLocation = true;
        buttonText.setValue("Aturar el seguiment de la ubicació");
    }


    private void stopTrackingLocation() {
        if (mTrackingLocation) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            mTrackingLocation = false;
            progressBar.postValue(false);
            buttonText.setValue("Comença a seguir la ubicació");
        }
    }


    private void fetchAddress(Location location) {
        Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (addresses != null && addresses.size() > 0) {
            Address address = addresses.get(0);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                sb.append(address.getAddressLine(i));
                sb.append("\n");
            }
            currentAddress.postValue(sb.toString());
        } else {
            currentAddress.postValue("No es pot obtenir la ubicació actual");
        }
    }

    public String getUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return user.getUid();
        }
        return null;
    }

    public void setPermissionRequestCallback(OnPermissionRequestCallback permissionRequestCallback) {
        this.permissionRequestCallback = permissionRequestCallback;
    }
    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mPermissionGranted = true;
            startTrackingLocation();
        } else {
            if (permissionRequestCallback != null) {
                permissionRequestCallback.requestPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }
    }
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mPermissionGranted = true;
                startTrackingLocation();
            } else {
                Toast.makeText(mContext, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }


}
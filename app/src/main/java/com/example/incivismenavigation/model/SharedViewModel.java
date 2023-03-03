package com.example.incivismenavigation.model;

import android.app.Application;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SharedViewModel extends AndroidViewModel {

    private MutableLiveData<FirebaseUser> user = new MutableLiveData<>();
    private final MutableLiveData<LatLng> currentLatLng = new MutableLiveData<>();
    private MutableLiveData<FirebaseUser> userLiveData;

    public SharedViewModel(@NonNull Application application) {
        super(application);
    }

    public MutableLiveData<LatLng> getCurrentLatLng() {
        return currentLatLng;
    }

    private void getCurrentLatLng(Location location) {
        try {
            LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
            currentLatLng.postValue(latlng);
        } catch (Exception e) {
            Log.e("Error", String.valueOf(e));
        }
    }

    public LiveData<FirebaseUser> getUser() {
        if (userLiveData == null) {
            userLiveData = new MutableLiveData<>();
            FirebaseAuth.getInstance().addAuthStateListener(new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    userLiveData.setValue(firebaseAuth.getCurrentUser());
                }
            });
        }
        return userLiveData;
    }

    public void setUser(FirebaseUser passedUser) {
        user.postValue(passedUser);
    }
}

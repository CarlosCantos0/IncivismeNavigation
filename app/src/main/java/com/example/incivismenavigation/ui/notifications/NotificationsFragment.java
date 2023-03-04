package com.example.incivismenavigation.ui.notifications;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.incivismenavigation.Incidencia;
import com.example.incivismenavigation.IncidenciesInfoWindowAdapter;
import com.example.incivismenavigation.R;
import com.example.incivismenavigation.databinding.FragmentNotificationsBinding;
import com.example.incivismenavigation.model.SharedViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private SharedViewModel sharedviewmodel;
    String mCurrentPhotoPath;
    private Uri photoURI;
    private ImageView foto;
    static final int REQUEST_TAKE_PHOTO = 1;

    @SuppressLint("MissingPermission")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        NotificationsViewModel notificationsViewModel =
                new ViewModelProvider(this).get(NotificationsViewModel.class);

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.g_map);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        DatabaseReference base = FirebaseDatabase.getInstance().getReference();

        DatabaseReference users = base.child("users");
        DatabaseReference uid = users.child(auth.getUid());
        DatabaseReference incidencies = uid.child("incidencies");

                sharedviewmodel = new ViewModelProvider(getActivity()).get(SharedViewModel.class);
        mapFragment.getMapAsync(map -> {
            // Codi a executar quan el mapa s'acabi de carregar.
            map.setMyLocationEnabled(true);
            MutableLiveData<LatLng> currentLatLng = sharedviewmodel.getCurrentLatLng();
            LifecycleOwner owner = getViewLifecycleOwner();
            currentLatLng.observe(owner, latLng -> {
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
                map.animateCamera(cameraUpdate);
                currentLatLng.removeObservers(owner);
            });
            try {
                // Customize the styling of the base map using a JSON object defined
                // in a raw resource file.
                boolean success = map.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(
                                getActivity(), R.raw.map_style));

                if (!success) {
                    Log.e(null, "Style parsing failed.");
                }
            } catch (Resources.NotFoundException e) {
                Log.e(null, "Can't find style. Error: ", e);
            }
            incidencies.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    Incidencia incidencia = dataSnapshot.getValue(Incidencia.class);

                    LatLng aux = new LatLng(
                            Double.valueOf(incidencia.getLatitud()),
                            Double.valueOf(incidencia.getLongitud())
                    );

                    IncidenciesInfoWindowAdapter customInfoWindow = new IncidenciesInfoWindowAdapter(
                            getActivity()
                    );

                    Marker marker = map.addMarker(new MarkerOptions()
                            .title(incidencia.getProblema())
                            .snippet(incidencia.getDireccio())
                            .position(aux)
                            .icon(BitmapDescriptorFactory.defaultMarker
                                    (BitmapDescriptorFactory.HUE_BLUE)));
                    marker.setTag(incidencia);
                    map.setInfoWindowAdapter(customInfoWindow);
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });

        });

        //fusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());


        //View view = inflater.inflate(R.layout.fragment_home, container, false);

        foto = root.findViewById(R.id.foto);
        Button buttonFoto = root.findViewById(R.id.button_foto);

        Button buttonNoti = root.findViewById(R.id.btnGetLocation);
        buttonNoti.setOnClickListener(button -> {
            Log.d("NotificationsFragment", "00000000!!!!!!!!ButtonNOTIIIIIIIIIII clicked!!!!!!!!!!!!!!!");
            //dispatchTakePictureIntent();

        });

        //buttonFoto.setEnabled(true);

        buttonFoto.setOnClickListener(button -> {
            Log.d("NotificationsFragment", "Button clicked!!!!!!!!!!!!!!!");
            dispatchTakePictureIntent();

        });

        return root;
    }

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = null;
        try {
            image = File.createTempFile(
                    imageFileName,
                    ".jpg",
                    storageDir
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (image != null) {
            mCurrentPhotoPath = image.getAbsolutePath();
        }
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(
                getContext().getPackageManager()) != null) {

            File photoFile = null;
            photoFile = createImageFile();

            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(getContext(),
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO) {
            if (resultCode == Activity.RESULT_OK) {
                Glide.with(this).load(photoURI).into(foto);
            } else {
                Toast.makeText(getContext(),
                        "Picture wasn't taken!", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
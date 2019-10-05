package com.alliancesgalore.alliancesgalore.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.alliancesgalore.alliancesgalore.Adapters.UserProfileAdapter;
import com.alliancesgalore.alliancesgalore.Fragments.LocationFragment;
import com.alliancesgalore.alliancesgalore.R;
import com.alliancesgalore.alliancesgalore.UserProfile;
import com.alliancesgalore.alliancesgalore.Utils.DividerItemDecorator;
import com.alliancesgalore.alliancesgalore.Utils.Global;
import com.alliancesgalore.alliancesgalore.Utils.SwipeToRefresh;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED;

public class MapActivity extends AppCompatActivity {
    private Toolbar mToolbar;
    private BottomSheetBehavior bottomSheetBehavior;
    private RecyclerView mRecycler;
    private UserProfileAdapter adapter;
    private List<UserProfile> mMapSelectionList;
    private Bundle bundle;
    MapView mMapView;
    private GoogleMap googleMap;
    private SwipeToRefresh mMapsRefresh;
    private LatLng MyLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        mMapSelectionList = new ArrayList<>();
        mMapSelectionList = getIntent().getParcelableArrayListExtra("objectlist");

        FindIds(savedInstanceState);
        mMapsRefresh.setOnRefreshListener(MapRefrshListener);
        bundle = new Bundle();
        UserProfile obj = getIntent().getParcelableExtra("object");
        MyLocation = new LatLng(obj.getLatitude(), obj.getLongitude());
        LatLng location = new LatLng(obj.getLatitude(), obj.getLongitude());
        setdefault(obj, location);
        setLocation(location);
        setmToolbar();
        setAdapter();
        RecyclerClick();
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottomSheetLayout));
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(View bottomSheet, int newState) {

                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    //      bottomSheetHeading.setText(getString(R.string.text_collapse_me));
                } else {
                    //     bottomSheetHeading.setText(getString(R.string.text_expand_me));
                }

                switch (newState) {
                    case STATE_COLLAPSED:
                        Log.e("Bottom Sheet Behaviour", "STATE_COLLAPSED");
                        break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        Log.e("Bottom Sheet Behaviour", "STATE_DRAGGING");
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        adapter.notifyDataSetChanged();
                        break;
                    case BottomSheetBehavior.STATE_HIDDEN:
                        Log.e("Bottom Sheet Behaviour", "STATE_HIDDEN");
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        Log.e("Bottom Sheet Behaviour", "STATE_SETTLING");
                        break;
                }
            }


            @Override
            public void onSlide(View bottomSheet, float slideOffset) {

            }
        });
    }

    private void FindIds(Bundle savedInstanceState) {
        mMapView = findViewById(R.id.mapview);
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume();
        mMapsRefresh = findViewById(R.id.mapsrefresh);

    }

    private void setAdapter() {
        mRecycler = findViewById(R.id.maplist_recyler);
        mRecycler.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecycler.setLayoutManager(layoutManager);
        adapter = new UserProfileAdapter(this, mMapSelectionList);
        mRecycler.setAdapter(adapter);
        mRecycler.addItemDecoration(new DividerItemDecorator(this));
        adapter.notifyDataSetChanged();
    }

    private void setdefault(UserProfile obj, LatLng location) {

        mMapView.getMapAsync(mMap -> {
            googleMap = mMap;
            googleMap.clear();
            SimpleDateFormat formatter = new SimpleDateFormat("hh:mm:ss");
            String time = formatter.format(new Date(Long.parseLong(obj.getLastUpdated().toString())));
            mMap.addMarker(new MarkerOptions()
                    .position(location)
                    .snippet("Last Updated" + time)
                    .title(obj.getDisplay_name()));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(location));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(14), 2000, null);
        });
    }

    private void setmToolbar() {
        mToolbar = findViewById(R.id.map_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Location");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

        }
        return (super.onOptionsItemSelected(item));
    }

    private void RecyclerClick() {
        adapter.setClickListener(adapterClickListener);
    }

    private void setLocation(LatLng location) {

        mMapView.getMapAsync(mMap -> {
            googleMap = mMap;
            googleMap.setMyLocationEnabled(true);
            CameraPosition cameraPosition = new CameraPosition.Builder().target(location).zoom(18).build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            mMapsRefresh.setRefreshing(false);

        });
    }

    private SwipeRefreshLayout.OnRefreshListener MapRefrshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            setLocation(MyLocation);
        }
    };

    private View.OnClickListener adapterClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int pos = mRecycler.indexOfChild(view);


            bottomSheetBehavior.setState(STATE_COLLAPSED);
            UserProfile obj = mMapSelectionList.get(pos);
            LatLng MyLocation = new LatLng(obj.getLatitude(), obj.getLongitude());
            Toast.makeText(MapActivity.this, obj.getDisplay_name(), Toast.LENGTH_SHORT).show();
            setdefault(obj, MyLocation);
            setLocation(MyLocation);
            adapter.swap(0, pos);
            adapter.notifyDataSetChanged();
        }
    };


}

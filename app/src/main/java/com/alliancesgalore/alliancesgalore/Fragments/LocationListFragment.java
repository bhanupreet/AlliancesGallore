package com.alliancesgalore.alliancesgalore.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.alliancesgalore.alliancesgalore.Activities.MainActivity;
import com.alliancesgalore.alliancesgalore.Activities.MapActivity;
import com.alliancesgalore.alliancesgalore.Activities.ReportingToActivity;
import com.alliancesgalore.alliancesgalore.Adapters.UserProfileAdapter;
import com.alliancesgalore.alliancesgalore.R;
import com.alliancesgalore.alliancesgalore.Models.UserProfile;
import com.alliancesgalore.alliancesgalore.Utils.Functions;
import com.cooltechworks.views.shimmer.ShimmerRecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.alliancesgalore.alliancesgalore.Utils.Global.myProfile;

public class LocationListFragment extends Fragment implements MainActivity.OnBackPressedListener {

    private RecyclerView mRecycler;
    private UserProfileAdapter adapter;
    private List<UserProfile> allsubordinatesList;
    private List<UserProfile> subordinatesList;
    private List<UserProfile> multiselect_list, filterlist;
    private String mail;
    private ShimmerRecyclerView shimmerRecycler;
    private boolean sortByLevel = true, ascending = true, isSelectAll = false;
    private ArrayList<UserProfile> temp;
    public static boolean isMultiselect = false;
    private LinearLayout mFilterbtn, mSortBtn;
    private SharedPreferences execSetting, tlSetting, managerSetting, sortsettings;
    private MenuItem mExecutives, mManagers, mTeamLeaders, mName, mLevel;
    public static ActionMode mActionmode = null;
    private SwipeRefreshLayout mSwipeResfresh;

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                selectallbtn(item);

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setHasOptionsMenu(boolean hasMenu) {
        super.setHasOptionsMenu(hasMenu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_locationlist, container, false);
        ((MainActivity) getActivity()).setOnBackPressedListener(this);
        ReportingToCheck();
        FindIds(view);
        query();
        setmSwipeResfresh();

        execSetting = setPrefs(true, "executiveSettings");
        tlSetting = setPrefs(true, "tlSettings");
        managerSetting = setPrefs(true, "managerSettings");
        setSortSetting();

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void setUserVisibleHint(boolean visible) {
        super.setUserVisibleHint(visible);
        if (visible && isResumed())
            onResume();
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        return super.onContextItemSelected(item);

    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, @Nullable ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.selection_menu, menu);


    }


    @Override
    public void onResume() {
        super.onResume();
        if (mActionmode != null)
            resetActionMode();
        if (!getUserVisibleHint())
            return;
        if (adapter != null)
            adapter.notifyDataSetChanged();
//        SetFAB();
        query();

        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener((v, keyCode, event) -> {

            if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                isMultiselect = false;
                resetActionMode();

                return true;

            }

            return false;
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (myProfile != null && TextUtils.isEmpty(myProfile.getReportingTo()))
            sendToReport();
    }

    private SharedPreferences setPrefs(boolean b, String settingstring) {
        SharedPreferences settings = Objects.requireNonNull(this.getActivity()).getSharedPreferences(settingstring, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("checkbox", b);
        editor.apply();
        return settings;
    }

    private Boolean handleMenuItemClicks(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.executives:
                filterfunction(item, 30, execSetting);
                return false;
            case R.id.teamLeaders:
                filterfunction(item, 20, tlSetting);
                return false;
            case R.id.managers:
                filterfunction(item, 10, managerSetting);
                return false;
            default:
                return false;
        }
    }

    private void setSortSetting() {
        sortsettings = Objects.requireNonNull(getContext()).getSharedPreferences("MyPref", 0);
        SharedPreferences.Editor editor1 = sortsettings.edit();
        editor1.putString("key_name", "level");
        editor1.apply();
    }

    private void FilterClick() {
        if (myProfile.getLevel() <= 10) {
            mFilterbtn.setOnClickListener(view -> {

                PopupMenu popup = new PopupMenu(getContext(), mFilterbtn);
                FindIds(popup);
                getSetChecked();

                if (myProfile.getLevel() == 10)
                    mManagers.setVisible(false);
                popup.setOnMenuItemClickListener(item -> {
                    FilterSettings(item);
                    return handleMenuItemClicks(item);
                });
                popup.show();
            });
        } else {
            mFilterbtn.setClickable(false);
            mFilterbtn.setEnabled(false);
            mFilterbtn.setVisibility(View.INVISIBLE);
        }
    }

    private void FilterSettings(MenuItem item) {
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        item.setActionView(new View(getContext()));
        item.setOnActionExpandListener(menuActionExpandListener);

    }

    private void getSetChecked() {
        boolean execCheck = execSetting.getBoolean("checkbox", false);
        boolean tlCheck = tlSetting.getBoolean("checkbox", false);
        boolean managerCheck = managerSetting.getBoolean("checkbox", false);
        mExecutives.setChecked(execCheck);
        mManagers.setChecked(managerCheck);
        mTeamLeaders.setChecked(tlCheck);

    }

    private void FindIds(PopupMenu popup) {
        popup.getMenuInflater().inflate(R.menu.filter_menu, popup.getMenu());
        mExecutives = popup.getMenu().findItem(R.id.executives);
        mManagers = popup.getMenu().findItem(R.id.managers);
        mTeamLeaders = popup.getMenu().findItem(R.id.teamLeaders);
    }

    private void filterfunction(MenuItem item, int level, SharedPreferences setting) {
        if (item.isChecked()) item.setChecked(false);
        else item.setChecked(true);
        SharedPreferences.Editor editor = setting.edit();
        editor.putBoolean("checkbox", item.isChecked());
        editor.apply();
        if (!item.isChecked()) {
            for (UserProfile profile : filterlist)
                if (profile.getLevel() == level)
                    filterlist.remove(profile);
        } else {
            for (UserProfile profile : subordinatesList)
                if (profile.getLevel() == level && !filterlist.contains(profile))
                    filterlist.add(profile);
        }
        sort(filterlist);
        adapter.notifyDataSetChanged();

    }

    private void fabclick() {
        MainActivity mainActivity = (MainActivity) getActivity();

        mainActivity.fab.setOnClickListener(v -> {
            isMultiselect = !isMultiselect;
            if (mActionmode == null) {
                mainActivity.fab.clearAnimation();
                Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_clockwise);
                mainActivity.fab.setImageResource(R.drawable.ic_close_white_24dp);
                mainActivity.fab.startAnimation(animation);
                mFilterbtn.setEnabled(false);
                mSortBtn.setEnabled(false);
                startActionMode(mainActivity);
            } else {
                resetActionMode();
            }
        });
    }

    private void startActionMode(MainActivity mainActivity) {
        mainActivity.mToolbar.startActionMode(actionMode);
        ((AppCompatImageView) getActivity().findViewById(R.id.action_mode_close_button)).setImageDrawable(getContext().getResources().getDrawable(R.drawable.uncheck));
        getActivity().findViewById(R.id.action_mode_close_button).setOnClickListener(view -> {
            if (!isSelectAll) {
                for (UserProfile profile : filterlist) {
                    profile.setSelected(true);
                    if (!multiselect_list.contains(profile))
                        multiselect_list.add(profile);

                }
                adapter.notifyDataSetChanged();
                isSelectAll = true;
                ((AppCompatImageView) getActivity().findViewById(R.id.action_mode_close_button)).setImageDrawable(getContext().getResources().getDrawable(R.drawable.checked));
            } else {
                for (UserProfile profile : filterlist) {
                    profile.setSelected(false);
                    multiselect_list.clear();
                }
                adapter.notifyDataSetChanged();
                isSelectAll = false;
                ((AppCompatImageView) getActivity().findViewById(R.id.action_mode_close_button)).setImageDrawable(getContext().getResources().getDrawable(R.drawable.uncheck));
            }
            setActionModeTitle();
        });

    }

    private void setSelectedTick(UserProfile selectedprofile) {
        selectedprofile.setSelected(!selectedprofile.getSelected());
        if (multiselect_list.contains(selectedprofile))
            multiselect_list.remove(selectedprofile);
        else
            multiselect_list.add(selectedprofile);
    }

    private void settingadapter(List<UserProfile> subordinatesList) {
        adapter = new UserProfileAdapter(getContext(), subordinatesList);
        mRecycler.setAdapter(adapter);
        mRecycler.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        mRecycler.setLayoutManager(layoutManager);
        adapter.notifyDataSetChanged();
    }

    private void query() {
        Query query = FirebaseDatabase.getInstance().getReference().child("Users").orderByChild("display_name");
        query.keepSynced(true);
        query.addListenerForSingleValueEvent(valueEventListener);
    }

    private void ReportingToCheck() {
        if (myProfile != null && TextUtils.isEmpty(myProfile.getReportingTo()))
            sendToReport();
    }

    private void FindIds(View view) {
        shimmerRecycler = view.findViewById(R.id.locationlist_recyclershimmer);
        shimmerRecycler.showShimmerAdapter();
        mRecycler = view.findViewById(R.id.locationlist_recycler);
        allsubordinatesList = new ArrayList<>();
        multiselect_list = new ArrayList<>();
        multiselect_list.clear();
        temp = new ArrayList<>();
        subordinatesList = new CopyOnWriteArrayList<>();
        mail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        mFilterbtn = view.findViewById(R.id.locationlist_filter);
        mSortBtn = view.findViewById(R.id.locationlist_sort);
        mSwipeResfresh = view.findViewById(R.id.locationlist_refresh);
    }

    private void setmSwipeResfresh() {
        mSwipeResfresh.setOnRefreshListener(() -> {
            query();
            mSwipeResfresh.setRefreshing(false);
        });
    }

    private void sortclick() {
        mSortBtn.setOnClickListener(view -> {
            PopupMenu popup = new PopupMenu(getContext(), mSortBtn);
            FindIds_sort(popup);
            getSet_Sort();
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.sortby_level:
                        setsortbtn(item, "level", true);
                        return false;
                    case R.id.sortyby_name:
                        setsortbtn(item, "name", false);
                        return false;
                    default:
                        return false;
                }
            });
            popup.show();
        });
    }

    private void setsortbtn(MenuItem item, String key, Boolean bool) {
        sortsettings = Objects.requireNonNull(getContext()).getSharedPreferences("MyPref", 0);
        SharedPreferences.Editor editor = sortsettings.edit();
        editor.putString("key_name", key);
        editor.apply();
        if (item.isChecked())
            ascending = !ascending;

        sortByLevel = bool;
        sort(filterlist);
        adapter.notifyDataSetChanged();
    }

    private void FindIds_sort(PopupMenu popup) {
        popup.getMenuInflater().inflate(R.menu.sort_menu, popup.getMenu());
        mName = popup.getMenu().findItem(R.id.sortyby_name);
        mLevel = popup.getMenu().findItem(R.id.sortby_level);
    }

    private void getSet_Sort() {
        sortsettings = Objects.requireNonNull(getContext()).getSharedPreferences("MyPref", 0);
        String chk = sortsettings.getString("key_name", null);
        if (chk != null) {
            if (chk.equals("level"))
                mLevel.setChecked(true);
            if (chk.equals("name"))
                mName.setChecked(true);
        }
    }

    private void sort(List<UserProfile> subordinatesList) {

        if (ascending)
            Collections.sort(subordinatesList, (t1, t2) -> t1.getDisplay_name().toLowerCase().compareTo(t2.getDisplay_name().toLowerCase()));
        else
            Collections.sort(subordinatesList, (t2, t1) -> t1.getDisplay_name().toLowerCase().compareTo(t2.getDisplay_name().toLowerCase()));

        if (sortByLevel && ascending)
            Collections.sort(subordinatesList, (t1, t2) -> t1.getLevel() - (t2.getLevel()));

        if (sortByLevel && !ascending)
            Collections.sort(subordinatesList, (t2, t1) -> t1.getLevel() - (t2.getLevel()));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        resetActionMode();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        resetActionMode();
    }

    private void itemClick() {


        adapter.addItemClickListener(pos -> {
            UserProfile selectedprofile = filterlist.get(pos);
            if (isMultiselect) {
                setSelectedTick(selectedprofile);
                setActionModeTitle();
                if (multiselect_list.isEmpty()) {
                    resetActionMode();
                }
                Functions.toast(selectedprofile.getDisplay_name() + " added", getContext());
                adapter.notifyDataSetChanged();
            } else sendToMap(selectedprofile, filterlist);
        });
    }

    private void itemLongClick() {
        Vibrator vibe = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);

        adapter.addItemLongClickListener(position -> {
            if (!isMultiselect) {
                vibe.vibrate(50);
                isMultiselect = true;
                if (mActionmode == null) {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    mFilterbtn.setEnabled(false);
                    mSortBtn.setEnabled(false);
                    startActionMode(mainActivity);
                    itemClick();
                }
            }
        });
    }

    private void sendToMap(UserProfile selected, List<UserProfile> subordinatesList) {
        Intent mapIntent = new Intent(getActivity(), MapActivity.class);
        mapIntent.putExtra("object", selected);
        mapIntent.putExtra("ismultiselect", isMultiselect);
        List<UserProfile> temp = new ArrayList<>();
        temp.clear();
        temp.addAll(subordinatesList);
        mapIntent.putParcelableArrayListExtra("objectlist", (ArrayList<? extends Parcelable>) temp);
        startActivity(mapIntent);
    }


    //IMPORTANT FUNCTION

    private void fetch(String email) {
        for (UserProfile profile : allsubordinatesList)
            if (!TextUtils.isEmpty(profile.getReportingTo()) && profile.getReportingTo().equals(email)
                    && !subordinatesList.contains(profile))

                subordinatesList.add(profile);
    }

    //IMPORANT FUCNTION ENDS


    private void sendToReport() {
        Intent startIntent = new Intent(getActivity(), ReportingToActivity.class);
        startActivity(startIntent);
        Objects.requireNonNull(getActivity()).finish();
    }

    private void SetFAB() {

        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity.getcurrenttabposition() == 1) {
//            mainActivity.fab.hide();
            Functions.toast("set in locationlist OnResume", getContext());
            mainActivity.fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_add_item, getContext().getTheme()));
        }
    }

    public void resetActionMode() {
//        MainActivity mainActivity = (MainActivity) getActivity();
//
//        mainActivity.fab.clearAnimation();
//        Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_anticlockwise);
//        mainActivity.fab.startAnimation(animation);
//        SetFAB();
        isMultiselect = false;
        if (mActionmode != null)
            mActionmode.finish();
        mFilterbtn.setEnabled(true);
        mSortBtn.setEnabled(true);
        for (UserProfile profile : filterlist)
            profile.setSelected(false);

        multiselect_list.clear();
        sort(filterlist);
        adapter.notifyDataSetChanged();
    }

    private void setActionModeTitle() {
        if (mActionmode != null) {
            if (multiselect_list.isEmpty())
                mActionmode.setTitle("Select");
            else
                mActionmode.setTitle("Selected: " + multiselect_list.size());
        }
    }

    private MenuItem.OnActionExpandListener menuActionExpandListener = new MenuItem.OnActionExpandListener() {
        @Override
        public boolean onMenuItemActionExpand(MenuItem item) {
            return false;
        }

        @Override
        public boolean onMenuItemActionCollapse(MenuItem item) {
            return false;
        }
    };

    private ValueEventListener valueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            allsubordinatesList.clear();
            subordinatesList.clear();

            if (dataSnapshot.exists()) {
                Iterable<DataSnapshot> snapshotIterator = dataSnapshot.getChildren();
                for (DataSnapshot next : snapshotIterator)
                    allsubordinatesList.add(next.getValue(UserProfile.class));

                if (mail.equals("superadmin@gmail.com")) {
                    subordinatesList = allsubordinatesList;

                } else if (myProfile != null) {
                    fetch(mail);
                    for (UserProfile profile1 : subordinatesList)
                        fetch(profile1.getEmail());

                    if (subordinatesList.isEmpty())
                        subordinatesList.add(myProfile);
                }

                sort(subordinatesList);
                filterlist = new CopyOnWriteArrayList<>(subordinatesList);
                settingadapter(filterlist);
                adapter.notifyDataSetChanged();
                shimmerRecycler.hideShimmerAdapter();
//                fabclick();
                FilterClick();
                sortclick();
                itemClick();
                itemLongClick();
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    };

    private ActionMode.Callback actionMode = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.selection_menu, menu);
            mActionmode = actionMode;
            setActionModeTitle();
            setHasOptionsMenu(true);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.action_cancel:
                    resetActionMode();
                    break;
                case R.id.action_select:
                    if (filterlist.isEmpty())
                        Functions.toast("List cannot be empty", getContext());
                    else if (multiselect_list.isEmpty())
                        Functions.toast("No items selected", getContext());
                    else
                        sendToMap(multiselect_list.get(0), multiselect_list);
                    break;
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            mActionmode = null;
            doBack();
        }
    };

    private void selectallbtn(MenuItem menuItem) {
        if (!isSelectAll) {
            for (UserProfile profile : filterlist) {
                profile.setSelected(true);
                if (!multiselect_list.contains(profile))
                    multiselect_list.add(profile);

            }
            adapter.notifyDataSetChanged();
            isSelectAll = true;
            menuItem.setIcon(R.drawable.ic_cancel_black_24dp);
        } else {
            for (UserProfile profile : filterlist) {
                profile.setSelected(false);
                multiselect_list.clear();
            }
            adapter.notifyDataSetChanged();
            isSelectAll = false;
            menuItem.setIcon(R.drawable.ic_selectall);
        }
        setActionModeTitle();
    }

    @Override
    public void doBack() {
        Toast.makeText(getActivity(), "OnBackpress Click", Toast.LENGTH_LONG).show();
        resetActionMode();
//        SetFAB();
    }
}
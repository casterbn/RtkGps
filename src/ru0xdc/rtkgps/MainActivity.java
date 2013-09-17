package ru0xdc.rtkgps;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceActivity;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;

import butterknife.InjectView;
import butterknife.Views;
import ru0xdc.rtkgps.settings.ProcessingOptions1Fragment;
import ru0xdc.rtkgps.settings.SettingsActivity;
import ru0xdc.rtkgps.settings.SettingsHelper;
import ru0xdc.rtkgps.settings.SolutionOutputSettingsFragment;
import ru0xdc.rtkgps.settings.StreamSettingsActivity;

import java.io.File;

import javax.annotation.Nonnull;

public class MainActivity extends Activity {

    private static final boolean DBG = BuildConfig.DEBUG & true;
    static final String TAG = MainActivity.class.getSimpleName();

    /**
     * The serialization (saved instance state) Bundle key representing the
     * current dropdown position.
     */
    private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";

    RtkNaviService mRtkService;
    boolean mRtkServiceBound = false;

    @InjectView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @InjectView(R.id.navigation_drawer) View mNavDrawer;

    @InjectView(R.id.navdraw_server_switch) Switch mNavDrawerServerSwitch;

    private ActionBarDrawerToggle mDrawerToggle;

    private int mNavDraverSelectedItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Views.inject(this);

        createDrawerToggle();

        if (savedInstanceState == null) {
            SettingsHelper.setDefaultValues(this, false);
            proxyIfUsbAttached(getIntent());
            selectDrawerItem(R.id.navdraw_item_status);
            mDrawerLayout.openDrawer(mNavDrawer);
        }

        mNavDrawerServerSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mDrawerLayout.closeDrawer(mNavDrawer);
                if (isChecked) {
                    startRtkService();
                }else {
                    stopRtkService();
                }
                invalidateOptionsMenu();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mRtkServiceBound) {
            final Intent intent = new Intent(this, RtkNaviService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        proxyIfUsbAttached(intent);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unbind from the service
        if (mRtkServiceBound) {
            unbindService(mConnection);
            mRtkServiceBound = false;
            mRtkService = null;
        }

    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
            mNavDraverSelectedItem = savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM);
            setNavDrawerItemChecked(mNavDraverSelectedItem);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mNavDraverSelectedItem != 0) {
            outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, mNavDraverSelectedItem);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean serviceActive = mNavDrawerServerSwitch.isChecked();
        menu.findItem(R.id.menu_start_service).setVisible(!serviceActive);
        menu.findItem(R.id.menu_stop_service).setVisible(serviceActive);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
        case R.id.menu_start_service:
            mNavDrawerServerSwitch.setChecked(true);
            break;
        case R.id.menu_stop_service:
            mNavDrawerServerSwitch.setChecked(false);
            break;
        case R.id.menu_settings:
            mDrawerLayout.openDrawer(mNavDrawer);
            break;
        case R.id.menu_about:
            startActivity(new Intent(this, AboutActivity.class));
            break;
        default:
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void proxyIfUsbAttached(Intent intent) {

        if (intent == null) return;

        if (!UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) return;

        if (DBG) Log.v(TAG, "usb device attached");

        final Intent proxyIntent = new Intent(UsbToRtklib.ACTION_USB_DEVICE_ATTACHED);
        proxyIntent.putExtras(intent.getExtras());
        sendBroadcast(proxyIntent);
    }

    private void createDrawerToggle() {
        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                R.drawable.ic_drawer,
                R.string.drawer_open,
                R.string.drawer_close
                ) {
            @Override
            public void onDrawerClosed(View view) {
                //getActionBar().setTitle(mTitle);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                //getActionBar().setTitle(mDrawerTitle);
            }

        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    private void selectDrawerItem(int itemId) {
        switch (itemId) {
        case R.id.navdraw_item_status:
        case R.id.navdraw_item_map:
            setNavDrawerItemFragment(itemId);
            break;
        case R.id.navdraw_item_input_streams:
            showInputStreamSettings();
            break;
        case R.id.navdraw_item_output_streams:
            showOutputStreamSettings();
            break;
        case R.id.navdraw_item_log_streams:
            showLogStreamSettings();
            break;
        case R.id.navdraw_item_processing_options:
        case R.id.navdraw_item_solution_options:
            showSettings(itemId);
            break;
        default:
            throw new IllegalStateException();
        }
    }

    private void setNavDrawerItemFragment(int itemId) {
        mDrawerLayout.closeDrawer(mNavDrawer);

        if (mNavDraverSelectedItem == itemId) {
            return;
        }

        switch (itemId) {
        case R.id.navdraw_item_status:
            replaceFragment(new StatusFragment(), R.id.navdraw_item_status);
            break;
        case R.id.navdraw_item_map:
        {
            String[] mapTypeMenuItems = { "Open Street Map", "Daum Map"};

            Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle("Map Select");
            dialog.setItems(mapTypeMenuItems, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which)
                    {
                        case 0: // Open Street Map
                            replaceFragment(new MapFragment(), R.id.navdraw_item_map);
                            break;
                        case 1: // Daum Map
                            replaceFragment(new DaumMapFragment(), R.id.navdraw_item_map);
                            break;
                    }
                }
            });
            dialog.show();
        }
            break;
        default:
            throw new IllegalArgumentException();
        }
    }

    private void replaceFragment(Fragment fragment, int itemId) {
        getFragmentManager()
        .beginTransaction()
        .replace(R.id.container, fragment)
        .commit();
        setNavDrawerItemChecked(itemId);
    }

    private void setNavDrawerItemChecked(int itemId) {
        final int[] items = new int[] {
            R.id.navdraw_item_status,
            R.id.navdraw_item_input_streams,
            R.id.navdraw_item_output_streams,
            R.id.navdraw_item_log_streams,
            R.id.navdraw_item_solution_options,
            R.id.navdraw_item_solution_options
        };

        for (int i: items) {
            mNavDrawer.findViewById(i).setActivated(itemId == i);
        }
        mNavDraverSelectedItem = itemId;
    }

    private void refreshServiceSwitchStatus() {
        boolean serviceActive = mRtkServiceBound && (mRtkService.isServiceStarted());
        mNavDrawerServerSwitch.setChecked(serviceActive);
    }

    private void startRtkService() {
        final Intent intent = new Intent(RtkNaviService.ACTION_START);
        intent.setClass(this, RtkNaviService.class);
        startService(intent);
    }

    private void stopRtkService() {
        final Intent intent = new Intent(RtkNaviService.ACTION_STOP);
        intent.setClass(this, RtkNaviService.class);
        startService(intent);
    }

    public RtkNaviService getRtkService() {
        return mRtkService;
    }

    private void showSettings(int itemId) {
        final Intent intent = new Intent(this, SettingsActivity.class);
        switch (itemId) {
        case R.id.navdraw_item_processing_options:
            intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT,
                    ProcessingOptions1Fragment.class.getName());
            break;
        case R.id.navdraw_item_solution_options:
            intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT,
                    SolutionOutputSettingsFragment.class.getName());
            break;
        default:
            throw new IllegalStateException();
        }
        startActivity(intent);
    }

    private void showInputStreamSettings() {
        final Intent intent = new Intent(this, StreamSettingsActivity.class);
        intent.putExtra(StreamSettingsActivity.ARG_STEAM,
                StreamSettingsActivity.STREAM_INPUT_SETTINGS);
        startActivity(intent);
    }

    private void showOutputStreamSettings() {
        final Intent intent = new Intent(this, StreamSettingsActivity.class);
        intent.putExtra(StreamSettingsActivity.ARG_STEAM,
                StreamSettingsActivity.STREAM_OUTPUT_SETTINGS);
        startActivity(intent);
    }

    private void showLogStreamSettings() {
        final Intent intent = new Intent(this, StreamSettingsActivity.class);
        intent.putExtra(StreamSettingsActivity.ARG_STEAM,
                StreamSettingsActivity.STREAM_LOG_SETTINGS);
        startActivity(intent);
    }


    public void onNavDrawevItemClicked(View v) {
        selectDrawerItem(v.getId());
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get
            // LocalService instance
            RtkNaviService.RtkNaviServiceBinder binder = (RtkNaviService.RtkNaviServiceBinder) service;
            mRtkService = binder.getService();
            mRtkServiceBound = true;
            refreshServiceSwitchStatus();
            invalidateOptionsMenu();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mRtkServiceBound = false;
            mRtkService = null;
            refreshServiceSwitchStatus();
            invalidateOptionsMenu();
        }
    };

    @Nonnull
    public static File getFileStorageDirectory() {
        return new File(Environment.getExternalStorageDirectory(), "RtkGps/");
    }

    @Nonnull
    public static File getLocalSocketPath(Context ctx, String socketName) {
        return ctx.getFileStreamPath(socketName);
    }
}

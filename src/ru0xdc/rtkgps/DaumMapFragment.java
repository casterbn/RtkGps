package ru0xdc.rtkgps;

import static junit.framework.Assert.assertNotNull;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import butterknife.InjectView;
import butterknife.Views;

import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;
import net.daum.mf.map.api.MapView.MapType;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;

import ru0xdc.rtkgps.view.GTimeView;
import ru0xdc.rtkgps.view.SolutionView;
import ru0xdc.rtkgps.view.StreamIndicatorsView;
import ru0xdc.rtklib.RtkCommon;
import ru0xdc.rtklib.RtkCommon.Position3d;
import ru0xdc.rtklib.RtkControlResult;
import ru0xdc.rtklib.RtkServerStreamStatus;
import ru0xdc.rtklib.Solution;
import ru0xdc.rtklib.constants.SolutionStatus;

import java.util.Timer;
import java.util.TimerTask;

public class DaumMapFragment extends Fragment {
    static final String TAG = DaumMapFragment.class.getSimpleName();

    private static final boolean DBG = BuildConfig.DEBUG & true;

    private static final String SHARED_PREFS_NAME = "map";
    private static final String PREFS_TITLE_SOURCE = "title_source";
    private static final String PREFS_SCROLL_X = "scroll_x";
    private static final String PREFS_SCROLL_Y = "scroll_y";
    private static final String PREFS_ZOOM_LEVEL = "zoom_level";

    private static final String MAP_MODE_STANDARD = MapType.Standard.name();
    private static final String MAP_MODE_SATELLITE = MapType.Satellite.name();
    private static final String MAP_MODE_HYBRID = MapType.Hybrid.name();
    public static final String LOG_TAG = null;

    private Timer mStreamStatusUpdateTimer;
    private RtkServerStreamStatus mStreamStatus;

    private RtkControlResult mRtkStatus;

    @InjectView(R.id.streamIndicatorsView) StreamIndicatorsView mStreamIndicatorsView;
    @InjectView(R.id.daum_map_container) ViewGroup mMapViewContainer;
    @InjectView(R.id.gtimeView) GTimeView mGTimeView;
    @InjectView(R.id.solutionView) SolutionView mSolutionView;

    private MapView mMapView = null;
    private MapEventListener mMapEventListener;
    MapPOIItem poiItem = null;

    private Context context;

    public DaumMapFragment() {
        mStreamStatus = new RtkServerStreamStatus();
        mRtkStatus = new RtkControlResult();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        //final Context context;
        View v = inflater.inflate(R.layout.fragment_daum_map, container, false);
        Views.inject(this, v);

        context = inflater.getContext();

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onActivityCreated(savedInstanceState);

        MapView.setMapTilePersistentCacheEnabled(true);

        mMapView = new MapView(context);

        mMapEventListener = new MapEventListener();

        mMapView.setDaumMapApiKey("DAUM_MMAPS_ANDROID_DEMO_APIKEY");
        mMapView.setOpenAPIKeyAuthenticationResultListener(mMapEventListener);
        mMapView.setMapViewEventListener(mMapEventListener);
        mMapView.setPOIItemEventListener(mMapEventListener);

        mMapView.setMapType(MapView.MapType.Standard);
        mMapView.setHDMapTileEnabled(true);

        mMapViewContainer.addView(mMapView, 0);

        poiItem = new MapPOIItem();
        poiItem.setItemName("로버 위치");
        poiItem.setMarkerType(MapPOIItem.MarkerType.RedPin);
        poiItem.setShowAnimationType(MapPOIItem.ShowAnimationType.NoAnimation);
        poiItem.setTag(1000);
    }

    @Override
    public void onStart() {
        super.onStart();

        // XXX
        mStreamStatusUpdateTimer = new Timer();
        mStreamStatusUpdateTimer.scheduleAtFixedRate(
                new TimerTask() {
                    Runnable updateStatusRunnable = new Runnable() {
                        @Override
                        public void run() {
                            DaumMapFragment.this.updateStatus();
                        }
                    };
                    @Override
                    public void run() {
                        Activity a = getActivity();
                        if (a == null) return;
                        a.runOnUiThread(updateStatusRunnable);
                    }
                }, 200, 2500);
    }

    @Override
    public void onPause() {
        super.onPause();
        saveMapPreferences();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_daum_map, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        final int checked;

        if (mMapView == null) return;

        final String providerName = getTileSourceName();
        if (MAP_MODE_SATELLITE.equals(providerName)) {
            checked = R.id.menu_map_mode_satellite;
        }else if (MAP_MODE_HYBRID.equals(providerName)) {
            checked = R.id.menu_map_mode_hybrid;
        }else {
            checked = R.id.menu_map_mode_standard;
        }

        menu.findItem(checked).setChecked(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMapPreferences();
    }

    @Override
    public void onStop() {
        super.onStop();
        mStreamStatusUpdateTimer.cancel();
        mStreamStatusUpdateTimer = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mMapView = null;
        poiItem = null;
        Views.reset(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        final String tileSource;

        switch (item.getItemId()) {
        case R.id.menu_map_mode_standard:
            tileSource = MAP_MODE_STANDARD;
            break;
        case R.id.menu_map_mode_satellite:
            tileSource = MAP_MODE_SATELLITE;
            break;
        case R.id.menu_map_mode_hybrid:
            tileSource = MAP_MODE_HYBRID;
            break;
        default:
            return super.onOptionsItemSelected(item);
        }

        setTileSource(tileSource);

        return true;
    }

    void updateStatus() {
        MainActivity ma;
        RtkNaviService rtks;
        int serverStatus;

        // XXX
        ma = (MainActivity)getActivity();

        if (ma == null) return;

        rtks = ma.getRtkService();
        if (rtks == null) {
            serverStatus = RtkServerStreamStatus.STATE_CLOSE;
            mStreamStatus.clear();
        }else {
            rtks.getStreamStatus(mStreamStatus);
            rtks.getRtkStatus(mRtkStatus);
            serverStatus = rtks.getServerStatus();
            appendSolutions(rtks.readSolutionBuffer());
            mMapEventListener.setStatus(mRtkStatus, true);
            mGTimeView.setTime(mRtkStatus.getSolution().getTime());
            mSolutionView.setStats(mRtkStatus);
        }

        assertNotNull(mStreamStatus.mMsg);

        mStreamIndicatorsView.setStats(mStreamStatus, serverStatus);
    }

    private void saveMapPreferences() {

        getActivity()
            .getSharedPreferences(SHARED_PREFS_NAME,Context.MODE_PRIVATE)
            .edit()
            .putString(PREFS_TITLE_SOURCE, getTileSourceName())
            .putInt(PREFS_SCROLL_X, mMapView.getScrollX())
            .putInt(PREFS_SCROLL_Y, mMapView.getScrollY())
            .putInt(PREFS_ZOOM_LEVEL, mMapView.getZoomLevel())
            .commit();

    }

    private void loadMapPreferences() {
        SharedPreferences prefs = getActivity().getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);

        final String tileSourceName = prefs.getString(PREFS_TITLE_SOURCE, TileSourceFactory.DEFAULT_TILE_SOURCE.name());
        setTileSource(tileSourceName);

        mMapView.setZoomLevel(prefs.getInt(PREFS_ZOOM_LEVEL, 1), true);

        mMapView.scrollTo(
                prefs.getInt(PREFS_SCROLL_X, 0),
                prefs.getInt(PREFS_SCROLL_Y, 0)
                );
    }

    private void setTileSource(String name) {
        if(MapType.Satellite.name().equals(name))
            mMapView.setMapType(MapType.Satellite);
        else if(MapType.Hybrid.name().equals(name))
            mMapView.setMapType(MapType.Hybrid);
        else
            mMapView.setMapType(MapType.Standard);
    }

    private String getTileSourceName() {
        final MapType provider = mMapView.getMapType();

        return provider.name();
    }

    private void appendSolutions(Solution solutions[]) {
//        mPathOverlay.addSolutions(solutions);
    }

    class MapEventListener implements MapView.OpenAPIKeyAuthenticationResultListener, MapView.MapViewEventListener,  MapView.POIItemEventListener {
        private Location mLastLocation = new Location("");

        private void setSolution(Solution s, boolean notifyConsumer) {
            if (s.getSolutionStatus() == SolutionStatus.NONE) {
                return;
            }

            final Position3d pos = RtkCommon.ecef2pos(s.getPosition());

            mLastLocation.setTime(s.getTime().getUtcTimeMillis());
            mLastLocation.setLatitude(Math.toDegrees(pos.getLat()));
            mLastLocation.setLongitude(Math.toDegrees(pos.getLon()));
            mLastLocation.setAltitude(pos.getHeight());

            if (mMapView != null) {
                if (notifyConsumer) {
                    MapPoint mp = MapPoint.mapPointWithGeoCoord(mLastLocation.getLatitude(),mLastLocation.getLongitude());
                    mMapView.setMapCenterPointAndZoomLevel(mp, mMapView.getZoomLevel(), true);

                    mMapView.removeAllPOIItems();
                    poiItem.setMapPoint(mp);
                    mMapView.addPOIItem(poiItem);
                }
            }
        }

        public void setStatus(RtkControlResult status, boolean notifyConsumer) {
            setSolution(status.getSolution(), notifyConsumer);
        }

        @Override
        public void onCalloutBalloonOfPOIItemTouched(MapView arg0, MapPOIItem arg1) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onDraggablePOIItemMoved(MapView arg0, MapPOIItem arg1, MapPoint arg2) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onPOIItemSelected(MapView arg0, MapPOIItem arg1) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onMapViewCenterPointMoved(MapView arg0, MapPoint arg1) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onMapViewDoubleTapped(MapView arg0, MapPoint arg1) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onMapViewInitialized(MapView mapView) {
            // TODO Auto-generated method stub
            //mapView.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(36.35, 127.38), 7, true);
        }

        @Override
        public void onMapViewLongPressed(MapView arg0, MapPoint arg1) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onMapViewSingleTapped(MapView arg0, MapPoint arg1) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onMapViewZoomLevelChanged(MapView arg0, int arg1) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onDaumMapOpenAPIKeyAuthenticationResult(MapView mapView, int resultCode, String resultMessage) {
            // TODO Auto-generated method stub
            Log.d(LOG_TAG,  String.format("Open API Key Authentication Result : code=%d, message=%s", resultCode, resultMessage));
        }
    };
}

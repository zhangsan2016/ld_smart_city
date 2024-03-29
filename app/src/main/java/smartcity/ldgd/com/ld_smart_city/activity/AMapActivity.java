package smartcity.ldgd.com.ld_smart_city.activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapOptions;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.CustomMapStyleOptions;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import smartcity.ldgd.com.ld_smart_city.R;
import smartcity.ldgd.com.ld_smart_city.base.BaseMapActivity;
import smartcity.ldgd.com.ld_smart_city.cluster.ClusterClickListener;
import smartcity.ldgd.com.ld_smart_city.cluster.ClusterItem;
import smartcity.ldgd.com.ld_smart_city.cluster.ClusterOverlay;
import smartcity.ldgd.com.ld_smart_city.cluster.ClusterRender;
import smartcity.ldgd.com.ld_smart_city.entity.LoginJson;
import smartcity.ldgd.com.ld_smart_city.util.LogUtil;

public class AMapActivity extends BaseMapActivity implements ClusterRender, AMap.OnMapLoadedListener, ClusterClickListener {

    private MapView mMapView = null;
    private AMap mAMap = null;
    private CustomMapStyleOptions mapStyleOptions = new CustomMapStyleOptions();

    private ClusterOverlay mClusterOverlay;
    private UiSettings mUiSettings;
    private Map<Integer, Drawable> mBackDrawAbles = new HashMap<Integer, Drawable>();
    // 登录返回参数
    private LoginJson loginJson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 去掉窗口标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 隐藏顶部的状态栏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);
        //获取地图控件引用
        mMapView = (MapView) findViewById(R.id.map);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mMapView.onCreate(savedInstanceState);


        // 获取传递过来的参数
        getData();

        // 初始化地图
        initMap();


    }

    private void initMap() {
        if (mAMap == null) {
            // 初始化地图
            mAMap = mMapView.getMap();
            mUiSettings = mAMap.getUiSettings();
            mAMap.setOnMapLoadedListener(this);
            // 设置地图样式
            setMapCustomStyleFile(this);
            // 设置地图logo显示在右下方
            mUiSettings.setLogoPosition(AMapOptions.LOGO_POSITION_BOTTOM_RIGHT);
            // 设置地图默认的缩放按钮是否显示
            mUiSettings.setZoomControlsEnabled(false);
            // 设置地图缩放比例
            mAMap.moveCamera(CameraUpdateFactory.zoomTo(5f));


        }
    }

    public void getData() {
        loginJson = (LoginJson) getIntent().getSerializableExtra("loginInfo");
        LogUtil.e("xxx loginInfo = " + loginJson.getData().getToken().getToken());
    }


    private void setMapCustomStyleFile(Context context) {
        String styleName = "styleMap/style.data";
        InputStream inputStream = null;
        try {
            inputStream = context.getAssets().open(styleName);
            byte[] b = new byte[inputStream.available()];
            inputStream.read(b);

            if (mapStyleOptions != null) {
                // 设置自定义样式
                mapStyleOptions.setEnable(false);
                // 设置自定义样式
                mapStyleOptions.setStyleData(b);
                mAMap.setCustomMapStyle(mapStyleOptions);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        mMapView.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    public void onMapLoaded() {
        //添加测试数据
        new Thread() {
            public void run() {
              /*  double[] latList = new double[]{23.058877, 26.725366, 31.970214};
                double[] lonList = new double[]{115.800238, 107.011176, 114.437933};

                mClusterOverlay = new ClusterOverlay(mAMap, dp2px(getApplicationContext(), 0), getApplicationContext());
                mClusterOverlay.setClusterRenderer(AMapActivity.this);
                mClusterOverlay.setOnClusterClickListener(AMapActivity.this);

                // 居中定位
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                //随机
                for (int i = 0; i < 3; i++) {
                    double latA = latList[i];
                    double lonB = lonList[i];
                    Cluster cluster = new Cluster(new LatLng(latA, lonB, false));
                    builder.include(new LatLng(latA, lonB, false));

                    int random = (int) (Math.random() * 10 + 2);
                    for (int j = 0; j < random; j++) {

                        double lat = Math.random() + latA;
                        double lon = Math.random() + lonB;

                        LatLng latLng = new LatLng(lat, lon, false);
                        RegionItem regionItem = new RegionItem(latLng);
                        cluster.addClusterItem(regionItem);
                    }

                    mClusterOverlay.addClusterGroup(cluster);
                }

                LatLngBounds latLngBounds = builder.build();
                mAMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 100));
                mAMap.moveCamera(CameraUpdateFactory.zoomTo(5f));
                // mAMap.moveCamera(CameraUpdateFactory.zoomTo(3));*/


                // 通过网络获取项目列表
                mClusterOverlay = new ClusterOverlay(mAMap, dp2px(getApplicationContext(), 0), getApplicationContext());
                mClusterOverlay.setClusterRenderer(AMapActivity.this);
                mClusterOverlay.setOnClusterClickListener(AMapActivity.this);
                String token = loginJson.getData().getToken().getToken();
                getProject(token, mAMap, mClusterOverlay);

            }

        }.start();
    }


    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public int dp2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    @Override
    public Drawable getDrawAble(int clusterNum) {
        int radius = dp2px(getApplicationContext(), 80);

        Drawable bitmapDrawable = null;
        if (clusterNum == 1) {
            bitmapDrawable = mBackDrawAbles.get(clusterNum);
            if (bitmapDrawable == null) {
                bitmapDrawable = getApplication().getResources().getDrawable(
                        R.drawable.bian);
                mBackDrawAbles.put(1, bitmapDrawable);
            }

        } else if (clusterNum == 2) {
            bitmapDrawable = mBackDrawAbles.get(clusterNum);
            if (bitmapDrawable == null) {
                // bitmapDrawable = getApplication().getResources().getDrawable(R.drawable.icon_openmap_mark);
                bitmapDrawable = getApplication().getResources().getDrawable(R.drawable.icon_gcoding);
                mBackDrawAbles.put(2, bitmapDrawable);
            }

        } else if (clusterNum == 3) {
            bitmapDrawable = mBackDrawAbles.get(clusterNum);
            if (bitmapDrawable == null) {
                // bitmapDrawable = getApplication().getResources().getDrawable(R.drawable.icon_openmap_mark);
                bitmapDrawable = getApplication().getResources().getDrawable(R.drawable.amap_ebox);
                mBackDrawAbles.put(3, bitmapDrawable);
            }

        }
        return bitmapDrawable;
    }


    private Bitmap drawCircle(int radius, int color) {

        Bitmap bitmap = Bitmap.createBitmap(radius * 2, radius * 2,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        RectF rectF = new RectF(0, 0, radius * 2, radius * 2);
        paint.setColor(color);
        canvas.drawArc(rectF, 0, 360, true, paint);
        return bitmap;
    }

    @Override
    public void onClick(Marker marker, List<ClusterItem> clusterItems) {


        if (clusterItems.size() != 1) {
            marker.setInfoWindowEnable(false);
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (ClusterItem clusterItem : clusterItems) {
                if (clusterItem.getPosition().equals("") || clusterItem.getPosition() == null) {
                    LogUtil.e("onClick  clusterItem.getPosition() = null ");
                    builder.include(clusterItem.getPosition());

                }
                builder.include(clusterItem.getPosition());
            }
            LatLngBounds latLngBounds = builder.build();
            mAMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 100));
        } else {
            marker.setInfoWindowEnable(true);
            marker.showInfoWindow();
              /*  // 将marker所在的经纬度的信息转化成屏幕上的坐标
            Point p = mBaiduMap.getProjection().toScreenLocation(
                    position);
            p.y -= 120;
            LatLng llInfo = mBaiduMap.getProjection()
                    .fromScreenLocation(p);
            View location = View.inflate(BaiduMapActivity.this,
                    R.layout.baidu_map_marker_info_item, null);
            mInfoWindow = new InfoWindow(location, llInfo, 0);
            // 显示InfoWindow
            mBaiduMap.showInfoWindow(mInfoWindow);*/

        }


    }
}

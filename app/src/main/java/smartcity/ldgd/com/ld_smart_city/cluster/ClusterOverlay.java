package smartcity.ldgd.com.ld_smart_city.cluster;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.animation.AlphaAnimation;
import com.amap.api.maps.model.animation.Animation;

import java.util.ArrayList;
import java.util.List;

import smartcity.ldgd.com.ld_smart_city.R;
import smartcity.ldgd.com.ld_smart_city.util.LogUtil;


/**
 * Created by yiyi.qi on 16/10/10.
 * 整体设计采用了两个线程,一个线程用于计算组织聚合数据,一个线程负责处理Marker相关操作
 */
public class ClusterOverlay implements AMap.OnCameraChangeListener,
        AMap.OnMarkerClickListener, AMap.OnMapClickListener, AMap.InfoWindowAdapter {
    private AMap mAMap;
    private Context mContext;
    private List<List<ClusterItem>> mClusterss;
    private List<ClusterItem> mClusterItems;
    private List<Cluster> mClustersBulk;
    private List<Cluster> mClusters;
    private int mClusterSize;
    private ClusterClickListener mClusterClickListener;
    private ClusterRender mClusterRender;
    private List<Marker> mAddMarkers = new ArrayList<Marker>();
    private double mClusterDistance;
    private LruCache<String, BitmapDescriptor> mLruCache;
    private HandlerThread mMarkerHandlerThread = new HandlerThread("addMarker");
    private HandlerThread mSignClusterThread = new HandlerThread("calculateCluster");
    private Handler mMarkerhandler;
    private Handler mSignClusterHandler;
    private float mPXInMeters;
    private boolean mIsCanceled = false;

    // 当前点击的 marker
    private Marker currentMarker;

    /**
     * 构造函数
     *
     * @param amap
     * @param clusterSize 聚合范围的大小（指点像素单位距离内的点会聚合到一个点显示）
     * @param context
     */
    public ClusterOverlay(AMap amap, int clusterSize, Context context) {
        //   this(amap, null, clusterSize, context);

        //默认最多会缓存80张图片作为聚合显示元素图片,根据自己显示需求和app使用内存情况,可以修改数量
        mLruCache = new LruCache<String, BitmapDescriptor>(80) {
            protected void entryRemoved(boolean evicted, Integer key, BitmapDescriptor oldValue, BitmapDescriptor newValue) {
                oldValue.getBitmap().recycle();
            }
        };
        mContext = context;
        mClusters = new ArrayList<Cluster>();
        mClustersBulk = new ArrayList<Cluster>();
        this.mAMap = amap;
        mClusterSize = clusterSize;
        mPXInMeters = mAMap.getScalePerPixel();
        mClusterDistance = mPXInMeters * mClusterSize;
        amap.setOnCameraChangeListener(this);
        amap.setOnMarkerClickListener(this);
        amap.setInfoWindowAdapter(this);// 设置自定义InfoWindow样式
        amap.setOnMapClickListener(this); // 地图点击事件
        initThreadHandler();

    }

    /**
     * 构造函数,批量添加聚合元素时,调用此构造函数
     *
     * @param amap
     * @param clusterItems 聚合元素
     * @param clusterSize
     * @param context
     */
    public ClusterOverlay(AMap amap, List<ClusterItem> clusterItems,
                          int clusterSize, Context context) {
        //默认最多会缓存80张图片作为聚合显示元素图片,根据自己显示需求和app使用内存情况,可以修改数量
        mLruCache = new LruCache<String, BitmapDescriptor>(80) {
            protected void entryRemoved(boolean evicted, Integer key, BitmapDescriptor oldValue, BitmapDescriptor newValue) {
                oldValue.getBitmap().recycle();
            }
        };
        if (clusterItems != null) {
            mClusterItems = clusterItems;
        } else {
            mClusterItems = new ArrayList<ClusterItem>();
        }
        mContext = context;
        mClusters = new ArrayList<Cluster>();
        this.mAMap = amap;
        mClusterSize = clusterSize;
        mPXInMeters = mAMap.getScalePerPixel();
        mClusterDistance = mPXInMeters * mClusterSize;
        amap.setOnCameraChangeListener(this);
        amap.setOnMarkerClickListener(this);
        amap.setOnMapClickListener(this);
        initThreadHandler();
        assignClusters();
    }

    /**
     * 设置聚合点的点击事件
     *
     * @param clusterClickListener
     */
    public void setOnClusterClickListener(
            ClusterClickListener clusterClickListener) {
        mClusterClickListener = clusterClickListener;
    }

    /**
     * 添加一个聚合点
     *
     * @param item
     */
    public void addClusterItem(ClusterItem item) {
        Message message = Message.obtain();
        message.what = SignClusterHandler.CALCULATE_SINGLE_CLUSTER;
        message.obj = item;
        mSignClusterHandler.sendMessage(message);
    }

    /**
     * 添加一个聚合
     *
     * @param cluster
     */
    public void addClusterGroup(Cluster cluster) {

        mClusters.add(cluster);

        List<ClusterItem> clusterItems = cluster.getClusterItems();
        for (int i = 0; i < clusterItems.size(); i++) {
            RegionItem regionItem = (RegionItem) clusterItems.get(i);
            Cluster clusterItem = new Cluster(regionItem.getPosition());
       String name =    regionItem.getDeviceLamp().getNAME();
            if(name == null){
                LogUtil.e("xxxxxxxxxxxxxxxxxx");
            }

            clusterItem.setTitle(regionItem.getDeviceLamp().getNAME());
            clusterItem.addClusterItem(regionItem);
            mClustersBulk.add(clusterItem);
        }

        Message message = Message.obtain();
        message.what = SignClusterHandler.CALCULATE_CLUSTER;
        mSignClusterHandler.sendMessage(message);
    }


    /**
     * 设置聚合元素的渲染样式，不设置则默认为气泡加数字形式进行渲染
     *
     * @param render
     */
    public void setClusterRenderer(ClusterRender render) {
        mClusterRender = render;
    }

    public void onDestroy() {
        mIsCanceled = true;
        mSignClusterHandler.removeCallbacksAndMessages(null);
        mMarkerhandler.removeCallbacksAndMessages(null);
        mSignClusterThread.quit();
        mMarkerHandlerThread.quit();
        for (Marker marker : mAddMarkers) {
            marker.remove();
        }
        mAddMarkers.clear();
        mLruCache.evictAll();
    }

    //初始化Handler
    private void initThreadHandler() {
        mMarkerHandlerThread.start();
        mSignClusterThread.start();
        mMarkerhandler = new MarkerHandler(mMarkerHandlerThread.getLooper());
        mSignClusterHandler = new SignClusterHandler(mSignClusterThread.getLooper());
    }

    @Override
    public void onCameraChange(CameraPosition arg0) {


    }

    private int preRoom = 0;

    @Override
    public void onCameraChangeFinish(CameraPosition arg0) {
        // 获取当前缩放级别下，地图上1像素点对应的长度，单位米。
        mPXInMeters = mAMap.getScalePerPixel();
        mClusterDistance = mPXInMeters * mClusterSize;

        int zoom = (int) mAMap.getCameraPosition().zoom;
        if (preRoom != zoom) {
            preRoom = zoom;
            assignClusters();
        }
    }

    //点击事件
    @Override
    public boolean onMarkerClick(Marker arg0) {

        if (mClusterClickListener == null) {
            return true;
        }
        Cluster cluster = (Cluster) arg0.getObject();
        if (cluster != null) {
            // 保存当前点击的 Marker
            currentMarker = arg0;
            mClusterClickListener.onClick(arg0, cluster.getClusterItems());
            return true;
        }
        return true;
    }


    @Override
    public void onMapClick(LatLng latLng) {
        if (currentMarker != null && currentMarker.isInfoWindowShown()) {
            //这个是隐藏infowindow窗口的方法
            currentMarker.hideInfoWindow();
        }
    }


    /**
     * 将聚合元素添加至地图上
     */
    private void addClusterToMap(List<Cluster> clusters) {

        ArrayList<Marker> removeMarkers = new ArrayList<>();
        removeMarkers.addAll(mAddMarkers);
        AlphaAnimation alphaAnimation = new AlphaAnimation(1, 0);
        MyAnimationListener myAnimationListener = new MyAnimationListener(removeMarkers);
        for (Marker marker : removeMarkers) {
            marker.setAnimation(alphaAnimation);
            marker.setAnimationListener(myAnimationListener);
            marker.startAnimation();
        }

        for (Cluster cluster : clusters) {
            addSingleClusterToMap(cluster);
        }
    }

    private AlphaAnimation mADDAnimation = new AlphaAnimation(0, 1);

    /**
     * 将单个聚合元素添加至地图显示
     *
     * @param cluster
     */
    private void addSingleClusterToMap(Cluster cluster) {
        LatLng latlng = cluster.getCenterLatLng();
        MarkerOptions markerOptions = new MarkerOptions();

        // 类型 1 等于 电箱，2等于路灯
        BitmapDescriptor bd = null;
        if (cluster.getClusterCount() > 1){
             bd = getBitmapDes(1,cluster.getClusterCount(),cluster.getTitle());
        }else {
          RegionItem clusterItem = (RegionItem) cluster.getClusterItems().get(0);
            // 判断路灯还是电箱
            if(clusterItem.getDeviceLamp().getTYPE()  == 2){
                bd = getBitmapDes(2,0,cluster.getTitle());
            }else if(clusterItem.getDeviceLamp().getTYPE()  == 1){
                bd = getBitmapDes(3,0,cluster.getTitle());
            }

        }

        markerOptions.anchor(0.5f, 0.5f)
                .icon(bd).position(latlng);
        Marker marker = mAMap.addMarker(markerOptions);
        marker.setAnimation(mADDAnimation);
        marker.setObject(cluster);

        marker.startAnimation();
        cluster.setMarker(marker);
        mAddMarkers.add(marker);

    }


    private void calculateClusters() {
        mIsCanceled = false;
        //  mClusters.clear();
        //  LatLngBounds visibleBounds = mAMap.getProjection().getVisibleRegion().latLngBounds;

        LogUtil.e("xxx mAMap.getCameraPosition().zoom  = " + mAMap.getCameraPosition().zoom);
        if (mAMap.getCameraPosition().zoom < 7) {

            upView(mClusters);

        } else {

            upView(mClustersBulk);
        }

    }

    private void upView(List<Cluster> clusters) {
        Message message = Message.obtain();
        message.what = MarkerHandler.ADD_CLUSTER_LIST;
        message.obj = clusters;
        if (mIsCanceled) {
            return;
        }
        mMarkerhandler.sendMessage(message);
    }

    /**
     * 对点进行聚合
     */
    private void assignClusters() {
        mIsCanceled = true;
        mSignClusterHandler.removeMessages(SignClusterHandler.CALCULATE_CLUSTER);
        mSignClusterHandler.sendEmptyMessage(SignClusterHandler.CALCULATE_CLUSTER);
    }

    /**
     * 在已有的聚合基础上，对添加的单个元素进行聚合
     *
     * @param clusterItem
     */
    private void calculateSingleCluster(ClusterItem clusterItem) {
        LatLngBounds visibleBounds = mAMap.getProjection().getVisibleRegion().latLngBounds;
        LatLng latlng = clusterItem.getPosition();
        if (!visibleBounds.contains(latlng)) {
            return;
        }
        Cluster cluster = getCluster(latlng, mClusters);
        if (cluster != null) {
            cluster.addClusterItem(clusterItem);
            Message message = Message.obtain();
            message.what = MarkerHandler.UPDATE_SINGLE_CLUSTER;

            message.obj = cluster;
            mMarkerhandler.removeMessages(MarkerHandler.UPDATE_SINGLE_CLUSTER);
            mMarkerhandler.sendMessageDelayed(message, 5);


        } else {

            cluster = new Cluster(latlng);
            mClusters.add(cluster);
            cluster.addClusterItem(clusterItem);
            Message message = Message.obtain();
            message.what = MarkerHandler.ADD_SINGLE_CLUSTER;
            message.obj = cluster;
            mMarkerhandler.sendMessage(message);

        }
    }

    /**
     * 根据一个点获取是否可以依附的聚合点，没有则返回null
     *
     * @param latLng
     * @return
     */
    private Cluster getCluster(LatLng latLng, List<Cluster> clusters) {
        for (Cluster cluster : clusters) {
            LatLng clusterCenterPoint = cluster.getCenterLatLng();
            double distance = AMapUtils.calculateLineDistance(latLng, clusterCenterPoint);
            if (mAMap.getCameraPosition().zoom < 7) {
                return cluster;
            }

        }

        return null;
    }


    /**
     * 获取每个聚合点的绘制样式
     */
    private BitmapDescriptor getBitmapDes(int type,int num,String title) {
        if(title == null){
            LogUtil.e("xxxxxxxxxxxxxxxxxx");
        }
        BitmapDescriptor bitmapDescriptor = mLruCache.get(title);
        if (bitmapDescriptor == null) {
            TextView textView = new TextView(mContext);
           if (num > 1) {
                String tile = String.valueOf(num);
                textView.setText(tile);
            }
            textView.setGravity(Gravity.CENTER);
            textView.setTextColor(Color.BLACK);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            if (mClusterRender != null && mClusterRender.getDrawAble(type) != null) {
                textView.setBackgroundDrawable(mClusterRender.getDrawAble(type));
            } else {
                textView.setBackgroundResource(R.drawable.defaultcluster);
            }
            bitmapDescriptor = BitmapDescriptorFactory.fromView(textView);
            mLruCache.put(title, bitmapDescriptor);

        }
        return bitmapDescriptor;
    }

    /**
     * 更新已加入地图聚合点的样式
     */
    private void updateCluster(Cluster cluster) {

        LogUtil.e("updateCluster 执行");

        Marker marker = cluster.getMarker();
        marker.setIcon(getBitmapDes(cluster.getClusterCount(),0,cluster.getTitle()));


    }



    @Override
    public View getInfoWindow(Marker marker) {

        View view = LayoutInflater.from(mContext).inflate(R.layout.amap_view_infowindow, null);
        LinearLayout navigation = (LinearLayout) view.findViewById(R.id.navigation_LL);
        LinearLayout call = (LinearLayout) view.findViewById(R.id.call_LL);
        TextView nameTV = (TextView) view.findViewById(R.id.agent_name);
        TextView addrTV = (TextView) view.findViewById(R.id.agent_addr);

        Cluster cluster = (Cluster) marker.getObject();
        List<ClusterItem> clusterItems = cluster.getClusterItems();
        final RegionItem regionItem = (RegionItem) clusterItems.get(0);

        nameTV.setText(regionItem.getDeviceLamp().getNAME());

        navigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogUtil.e("xxxx navigation 被点击 = " + regionItem.getDeviceLamp().toString());
                Toast.makeText(mContext, "查看", Toast.LENGTH_SHORT).show();
            }
        });
        call.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        LogUtil.e("xxxx call 被点击");
                                        Toast.makeText(mContext, "控制", Toast.LENGTH_SHORT).show();
                                    }
                                }
        );
        return view;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }


//-----------------------辅助内部类用---------------------------------------------

    /**
     * marker渐变动画，动画结束后将Marker删除
     */
    class MyAnimationListener implements Animation.AnimationListener {
        private List<Marker> mRemoveMarkers;

        MyAnimationListener(List<Marker> removeMarkers) {
            mRemoveMarkers = removeMarkers;
        }

        @Override
        public void onAnimationStart() {

        }

        @Override
        public void onAnimationEnd() {
            for (Marker marker : mRemoveMarkers) {
                marker.remove();
            }
            mRemoveMarkers.clear();
        }
    }

    /**
     * 处理market添加，更新等操作
     */
    class MarkerHandler extends Handler {

        static final int ADD_CLUSTER_LIST = 0;

        static final int ADD_SINGLE_CLUSTER = 1;

        static final int UPDATE_SINGLE_CLUSTER = 2;

        MarkerHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message message) {

            switch (message.what) {
                case ADD_CLUSTER_LIST:
                    List<Cluster> clusters = (List<Cluster>) message.obj;
                    addClusterToMap(clusters);
                    break;
                case ADD_SINGLE_CLUSTER:
                    Cluster cluster = (Cluster) message.obj;
                    addSingleClusterToMap(cluster);
                    break;
                case UPDATE_SINGLE_CLUSTER:
                    Cluster updateCluster = (Cluster) message.obj;
                    updateCluster(updateCluster);
                    break;
            }
        }
    }

    /**
     * 处理聚合点算法线程
     */
    class SignClusterHandler extends Handler {
        static final int CALCULATE_CLUSTER = 0;
        static final int CALCULATE_SINGLE_CLUSTER = 1;

        SignClusterHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message message) {
            switch (message.what) {
                case CALCULATE_CLUSTER:
                    calculateClusters();
                    break;
                case CALCULATE_SINGLE_CLUSTER:
                    ClusterItem item = (ClusterItem) message.obj;
                    mClusterItems.add(item);
                    Log.i("yiyi.qi", "calculate single cluster");
                    calculateSingleCluster(item);
                    break;
            }
        }
    }
}
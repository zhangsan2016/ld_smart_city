package smartcity.ldgd.com.ld_smart_city.base;


import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
import smartcity.ldgd.com.ld_smart_city.cluster.Cluster;
import smartcity.ldgd.com.ld_smart_city.cluster.ClusterOverlay;
import smartcity.ldgd.com.ld_smart_city.cluster.RegionItem;
import smartcity.ldgd.com.ld_smart_city.entity.DeviceLampJson;
import smartcity.ldgd.com.ld_smart_city.entity.ProjectJson;
import smartcity.ldgd.com.ld_smart_city.util.HttpUtil;
import smartcity.ldgd.com.ld_smart_city.util.LogUtil;
import smartcity.ldgd.com.ld_smart_city.util.MapHttpConfiguration;

/**
 * Created by ldgd on 2019/9/23.
 * 功能：
 * 说明：自定义Activity 基类
 */

public abstract class BaseMapActivity extends AppCompatActivity {
    protected ProgressDialog mProgress;

    protected void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(BaseMapActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    protected void stopProgress() {
        mProgress.cancel();
    }

    protected void showProgress() {
        mProgress = ProgressDialog.show(this, "", "Loading...");
    }


    /**
     * 获取项目列表
     */
    public void getProject(final String token, final AMap mAMap, final ClusterOverlay mClusterOverlay) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                String url = MapHttpConfiguration.PROJECT_LIST_URL;

                HttpUtil.sendHttpRequest(url, new Callback() {

                    @Override
                    public void onFailure(Call call, IOException e) {
                        showToast("连接服务器异常！");
                        stopProgress();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {

                        String json = response.body().string();
                        LogUtil.e("xxx" + "成功" + json);

                        // 解析返回过来的json
                        Gson gson = new Gson();
                        ProjectJson project = gson.fromJson(json, ProjectJson.class);
                        List<ProjectJson.DataBeanX.ProjectInfo> projectList = project.getData().getData();


                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        for (ProjectJson.DataBeanX.ProjectInfo projectInfo : projectList) {

                            try {
                                final CountDownLatch latch = new CountDownLatch(1);
                                // 用于计算当前显示范围
                                LatLng ll = new LatLng(Double.parseDouble(projectInfo.getLat()), Double.parseDouble(projectInfo.getLng()));
                                Cluster cluster = new Cluster(ll, projectInfo.getTitle());
                                builder.include(ll);

                                // 获取当前项目下的所有路灯
                                getDeviceLampList(projectInfo.getTitle(), token, cluster, latch);

                                //阻塞当前线程直到latch中数值为零才执行
                                latch.await();

                                mClusterOverlay.addClusterGroup(cluster);

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        }

                        LatLngBounds bounds = builder.build();
                        // 设置显示在屏幕中的地图地理范围
                        mAMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
                        mAMap.moveCamera(CameraUpdateFactory.zoomTo(5f));


                    }
                }, token, null);


            }
        }).start();

    }

    /**
     * 获取设备下管理的所有路灯
     */
    public void getDeviceLampList(final String title, final String token, final Cluster cluster, final CountDownLatch latch) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                String url = MapHttpConfiguration.DEVICE_LAMP_LIST_URL;

                // 创建请求的参数body
                //   String postBody = "{\"where\":{\"PROJECT\":" + title + "},\"size\":5000}";
                String postBody = "{\"where\":{\"PROJECT\":\"" + title + "\"},\"size\":5000}";
                RequestBody body = FormBody.create(MediaType.parse("application/json"), postBody);

                LogUtil.e("xxx postBody = " + postBody);

                HttpUtil.sendHttpRequest(url, new Callback() {

                    @Override
                    public void onFailure(Call call, IOException e) {
                        LogUtil.e("xxx" + "失败" + e.toString());
                        showToast("连接服务器异常！");
                        stopProgress();

                        //让latch中的数值减一
                        latch.countDown();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {

                        String json = response.body().string();
                        LogUtil.e("xxx" + "成功" + json);

                        // 解析返回过来的json
                        Gson gson = new Gson();
                        DeviceLampJson deviceLampJson = gson.fromJson(json, DeviceLampJson.class);
                        List<DeviceLampJson.DataBeanX.DeviceLamp> projectList = deviceLampJson.getData().getData();

                        for (DeviceLampJson.DataBeanX.DeviceLamp deviceLamp : projectList) {

                            if (deviceLamp.getLAT().equals("") || deviceLamp.getLNG().equals("")) {
                                break;
                            }
                            if (deviceLamp.getNAME().contains("米泉路")) {
                                LogUtil.e("xxx 米泉路" + deviceLamp.getLAT() + "  " + deviceLamp.getLNG());
                                break;
                            }

                            LatLng ll = new LatLng(Double.parseDouble(deviceLamp.getLAT()), Double.parseDouble(deviceLamp.getLNG()), false);
                            RegionItem regionItem = new RegionItem(ll, deviceLamp);
                            cluster.addClusterItem(regionItem);
                        }

                        //让latch中的数值减一
                        latch.countDown();

                    }
                }, token, body);
            }
        }).start();

    }

}

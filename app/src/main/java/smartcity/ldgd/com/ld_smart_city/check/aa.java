package smartcity.ldgd.com.ld_smart_city.check;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
import smartcity.ldgd.com.ld_smart_city.util.HttpUtil;
import smartcity.ldgd.com.ld_smart_city.util.MapHttpConfiguration;

/**
 * Created by ldgd on 2019/10/30.
 * 功能：
 * 说明：
 */

public class aa {


    public static void main(String[] args) {
        // 获取当前项目下的所有路灯

        //   getDeviceLampList("中科洛丁展示项目/深圳展厅", "5a2acbf0-fbb4-11e9-807b-c164eeb87e5f");
        // getDeviceLampList("中科洛丁展示项目/深圳展厅", "a9db04c0-faf7-11e9-807b-c164eeb87e5f");

        gethuibao("中科洛丁展示项目/深圳展厅", "5a2acbf0-fbb4-11e9-807b-c164eeb87e5f");
    }


    /**
     * 获取设备下管理的所有路灯
     */
    public static void getDeviceLampList(final String title, final String token) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                String url = MapHttpConfiguration.DEVICE_LAMP_LIST_URL;

                // 创建请求的参数body
                //   String postBody = "{\"where\":{\"PROJECT\":" + title + "},\"size\":5000}";
                String postBody = "{\"where\":{\"PROJECT\":\"" + title + "\"},\"size\":5000}";
                RequestBody body = FormBody.create(MediaType.parse("application/json"), postBody);

                System.out.println("xxx postBody = " + postBody);

                HttpUtil.sendHttpRequest(url, new Callback() {

                    @Override
                    public void onFailure(Call call, IOException e) {
                        System.out.println("xxx" + "失败" + e.toString());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {

                        String json = response.body().string();
                        System.out.println("xxx" + "成功" + json);


                    }
                }, token, body);
            }
        }).start();

    }


    /**
     * 汇报设备配置
     *
     * @param title
     * @param token
     */
    public static void gethuibao(final String title, final String token) {

        new Thread(new Runnable() {
            @Override
            public void run() {

               /* String url = "https://iot.sz-luoding.com:888/api/" + "device/reportConfig";
                String postBody = "{\"UUID\":\"2016C0312000001200001192\",\"config\": {\"xml_config\": \"21351515615sdf1sd61fs651d65f465sd46f54s6d54f33998\"}}";*/

                String url = "https://iot.sz-luoding.com:888/api/" + "device/viewByUUID";
                String postBody = "{\"UUID\":\"2016C0312000001200001192\"}";


                RequestBody body = FormBody.create(MediaType.parse("application/json"), postBody);


                HttpUtil.sendHttpRequest(url, new Callback() {

                    @Override
                    public void onFailure(Call call, IOException e) {
                        System.out.println("" + "失败" + e.toString());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {

                        String json = response.body().string();
                        System.out.println("" + "成功" + json);


                    }
                }, token, body);
            }
        }).start();

    }

}

package smartcity.ldgd.com.ld_smart_city.cluster;

import com.amap.api.maps.model.LatLng;

import smartcity.ldgd.com.ld_smart_city.entity.DeviceLampJson;

/**
 * Created by yiyi.qi on 16/10/10.
 */

public class RegionItem implements ClusterItem {
    private LatLng mLatLng;
    private DeviceLampJson.DataBeanX.DeviceLamp deviceLamp;

    public RegionItem(LatLng mLatLng) {
        this.mLatLng = mLatLng;
    }

    public RegionItem(LatLng latLng, DeviceLampJson.DataBeanX.DeviceLamp deviceLamp) {
        mLatLng = latLng;
        this.deviceLamp = deviceLamp;
    }

    @Override
    public LatLng getPosition() {
        // TODO Auto-generated method stub
        return mLatLng;
    }

    public DeviceLampJson.DataBeanX.DeviceLamp getDeviceLamp() {
        return deviceLamp;
    }
}

package com.gmail.uwriegel.superfit.AntPlusSensors

import android.content.Context
import com.dsi.ant.plugins.antplus.pcc.MultiDeviceSearch
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceType
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult
import java.util.*

/**
 * Created by urieg on 05.08.2017.
 */
fun searchHeartRate(context: Context, result: (device: com.dsi.ant.plugins.antplus.pccbase.MultiDeviceSearch.MultiDeviceSearchResult)->Unit) = searchDevice(context,"HEARTRATE", result)

private fun searchDevice(context: Context, deviceType: String, result: (device: com.dsi.ant.plugins.antplus.pccbase.MultiDeviceSearch.MultiDeviceSearchResult)->Unit)
{
    val devices = EnumSet.of(DeviceType.valueOf(deviceType))
    search = MultiDeviceSearch(context, devices, object: MultiDeviceSearch.SearchCallbacks {
        override fun onSearchStopped(p0: RequestAccessResult?) {

        }

        override fun onSearchStarted(p0: MultiDeviceSearch.RssiSupport?) {

        }

        override fun onDeviceFound(deviceFound: com.dsi.ant.plugins.antplus.pccbase.MultiDeviceSearch.MultiDeviceSearchResult?) {
            result(deviceFound!!)
        }
    })
}

var search: MultiDeviceSearch? = null
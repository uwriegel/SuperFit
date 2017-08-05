package com.gmail.uwriegel.superfit.AntPlusSensors

import android.content.Context
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeSpeedDistancePcc
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceType
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle
import java.math.BigDecimal
import java.util.*


/**
 * Created by urieg on 05.08.2017.
 */
class BikeMonitor {
    constructor(context: Context, onTest: (test: String)->Unit) {
        searchBike(context, {
            deviceName = it.deviceDisplayName
            deviceNumber = it.antDeviceNumber
            subscribe(context)
        })
    }

    private fun subscribe(context: Context) {
        bsdReleaseHandle = AntPlusBikeSpeedDistancePcc.requestAccess(context, deviceNumber, 0, true, { bikeController, resultCode, _ ->
            when (resultCode) {
                RequestAccessResult.SUCCESS ->
                    run {
                        subScribeToBike(bikeController)
                        if (bikeController.isSpeedAndCadenceCombinedSensor) {

                        }
                    }
            }
        }, {
        })
    }

    private fun subScribeToBike(bikeController: AntPlusBikeSpeedDistancePcc) = bikeController.subscribeCalculatedSpeedEvent(object:
            AntPlusBikeSpeedDistancePcc.CalculatedSpeedReceiver(BigDecimal(2.096)) {
        override fun onNewCalculatedSpeed(estTimestamp: Long, flags: EnumSet<EventFlag>?, calculatedSpeedInMs: BigDecimal?) {
            val calculatedSpeed = calculatedSpeedInMs?.toFloat()!! * 3.6
        }
    })

    private fun destroy() {
        bsdReleaseHandle?.close();
    }

    private var bsdReleaseHandle: PccReleaseHandle<AntPlusBikeSpeedDistancePcc>? = null
    private var deviceName = ""
    private var deviceNumber = 0
}
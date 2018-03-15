/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.lcz.com.bluetoothadvertisements

import android.bluetooth.le.AdvertiseCallback
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.Toast

/**
 * Allows user to start & stop Bluetooth LE Advertising of their device.
 */
class AdvertiserFragment : Fragment(), View.OnClickListener {
    /**
     * Lets user toggle BLE Advertising.
     */
    private var mSwitch: Switch? = null
    /**
     * Listens for notifications that the `AdvertiserService` has failed to start advertising.
     * This Receiver deals with Fragment UI elements and only needs to be active when the Fragment
     * is on-screen, so it's defined and registered in code instead of the Manifest.
     */
    private var advertisingFailureReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        advertisingFailureReceiver = object : BroadcastReceiver() {
            /**
             * Receives Advertising error codes from `AdvertiserService` and displays error messages
             * to the user. Sets the advertising toggle to 'false.'
             */
            override fun onReceive(context: Context, intent: Intent) {
                val errorCode = intent.getIntExtra(AdvertiserService.ADVERTISING_FAILED_EXTRA_CODE, -1)
                mSwitch!!.isChecked = false
                var errorMessage = getString(R.string.start_error_prefix)
                when (errorCode) {
                    AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> errorMessage += " " + getString(R.string.start_error_already_started)
                    AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> errorMessage += " " + getString(R.string.start_error_too_large)
                    AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> errorMessage += " " + getString(R.string.start_error_unsupported)
                    AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> errorMessage += " " + getString(R.string.start_error_internal)
                    AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> errorMessage += " " + getString(R.string.start_error_too_many)
                    AdvertiserService.ADVERTISING_TIMED_OUT -> errorMessage = " " + getString(R.string.advertising_timedout)
                    else -> errorMessage += " " + getString(R.string.start_error_unknown)
                }
                Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_advertiser, container, false)
        mSwitch = view.findViewById<View>(R.id.advertise_switch) as Switch
        mSwitch!!.setOnClickListener(this)
        return view
    }

    /**
     * When app comes on screen, check if BLE Advertisements are running, set switch accordingly,
     * and register the Receiver to be notified if Advertising fails.
     */
    override fun onResume() {
        super.onResume()
        mSwitch!!.isChecked = AdvertiserService.running
        val failureFilter = IntentFilter(AdvertiserService.ADVERTISING_FAILED)
        activity.registerReceiver(advertisingFailureReceiver, failureFilter)
    }

    /**
     * When app goes off screen, unregister the Advertising failure Receiver to stop memory leaks.
     * (and because the app doesn't care if Advertising fails while the UI isn't active)
     */
    override fun onPause() {
        super.onPause()
        activity.unregisterReceiver(advertisingFailureReceiver)
    }

    /**
     * Returns Intent addressed to the `AdvertiserService` class.
     */
    private fun getServiceIntent(c: Context): Intent {
        return Intent(c, AdvertiserService::class.java)
    }

    /**
     * Called when switch is toggled - starts or stops advertising.
     */
    override fun onClick(v: View) {
        // Is the toggle on?
        val on = (v as Switch).isChecked
        if (on) {
            startAdvertising()
        } else {
            stopAdvertising()
        }
    }

    /**
     * Starts BLE Advertising by starting `AdvertiserService`.
     */
    private fun startAdvertising() {
        val c = activity
        c.startService(getServiceIntent(c))
    }

    /**
     * Stops BLE Advertising by stopping `AdvertiserService`.
     */
    private fun stopAdvertising() {
        val c = activity
        c.stopService(getServiceIntent(c))
        mSwitch!!.isChecked = false
    }
}
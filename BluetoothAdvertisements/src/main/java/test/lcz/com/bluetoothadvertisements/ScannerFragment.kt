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

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ListFragment
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import test.lcz.com.bluetoothadvertisements.R.id.refresh
import java.util.ArrayList
import java.util.concurrent.TimeUnit


/**
 * Scans for Bluetooth Low Energy Advertisements matching a filter and displays them to the user.
 */
class ScannerFragment : ListFragment() {
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothLeScanner: BluetoothLeScanner? = null
    private var mScanCallback: ScanCallback? = null
    private var mAdapter: ScanResultAdapter? = null
    private var mHandler: Handler? = null

    /**
     * Must be called after object creation by MainActivity.
     *
     * @param btAdapter the local BluetoothAdapter
     */
    fun setBluetoothAdapter(btAdapter: BluetoothAdapter) {
        this.mBluetoothAdapter = btAdapter
        mBluetoothLeScanner = mBluetoothAdapter!!.bluetoothLeScanner
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        retainInstance = true
        // Use getActivity().getApplicationContext() instead of just getActivity() because this
        // object lives in a fragment and needs to be kept separate from the Activity lifecycle.
        //
        // We could get a LayoutInflater from the ApplicationContext but it messes with the
        // default theme, so generate it from getActivity() and pass it in separately.
        mAdapter = ScanResultAdapter(activity.applicationContext, LayoutInflater.from(activity))
        mHandler = Handler()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        listAdapter = mAdapter
        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.divider = null
        listView.dividerHeight = 0
        setEmptyText(getString(R.string.empty_list))
        // Trigger refresh on app's 1st load
        startScanning()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater!!.inflate(R.menu.menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item!!.itemId) {
            refresh -> {
                startScanning()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Start scanning for BLE Advertisements (& set it up to stop after a set period of time).
     */
    private fun startScanning() {
        if (mScanCallback == null) {
            Log.d(TAG, "Starting Scanning")
            // Will stop the scanning after a set time.
            mHandler!!.postDelayed({ stopScanning() }, SCAN_PERIOD)
            // Kick off a new scan.
            mScanCallback = SampleScanCallback()
            mBluetoothLeScanner!!.startScan(buildScanFilters(), buildScanSettings(), mScanCallback)
            val toastText = (getString(R.string.scan_start_toast) + " "
                    + TimeUnit.SECONDS.convert(SCAN_PERIOD, TimeUnit.MILLISECONDS) + " "
                    + getString(R.string.seconds))
            Toast.makeText(activity, toastText, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(activity, R.string.already_scanning, Toast.LENGTH_SHORT)
        }
    }

    /**
     * Stop scanning for BLE Advertisements.
     */
    private fun stopScanning() {
        Log.d(TAG, "Stopping Scanning")
        // Stop the scan, wipe the callback.
        mBluetoothLeScanner!!.stopScan(mScanCallback)
        mScanCallback = null
        // Even if no new results, update 'last seen' times.
        mAdapter!!.notifyDataSetChanged()
    }

    /**
     * Return a List of [ScanFilter] objects to filter by Service UUID.
     */
    private fun buildScanFilters(): List<ScanFilter> {
        val scanFilters = ArrayList<ScanFilter>()
        val builder = ScanFilter.Builder()
        // Comment out the below line to see all BLE devices around you
        builder.setServiceUuid(Constants.Service_UUID)
        scanFilters.add(builder.build())
        return scanFilters
    }

    /**
     * Return a [ScanSettings] object set to use low power (to preserve battery life).
     */
    private fun buildScanSettings(): ScanSettings {
        val builder = ScanSettings.Builder()
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
        return builder.build()
    }

    /**
     * Custom ScanCallback object - adds to adapter on success, displays error on failure.
     */
    private inner class SampleScanCallback : ScanCallback() {
        override fun onBatchScanResults(results: List<ScanResult>) {
            super.onBatchScanResults(results)
            for (result in results) {
                mAdapter!!.add(result)
            }
            mAdapter!!.notifyDataSetChanged()
        }

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            mAdapter!!.add(result)
            mAdapter!!.notifyDataSetChanged()
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Toast.makeText(activity, "Scan failed with error: " + errorCode, Toast.LENGTH_LONG)
                    .show()
        }
    }

    companion object {
        private val TAG = ScannerFragment::class.java.simpleName

        /**
         * Stops scanning after 5 seconds.
         */
        private val SCAN_PERIOD: Long = 5000
    }
}

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

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.widget.TextView
import android.widget.Toast
import test.lcz.com.bluetoothadvertisements.R.id.error_textview

/**
 * Setup display fragments and ensure the device supports Bluetooth.
 */
class MainActivity : FragmentActivity() {
    private var mBluetoothAdapter: BluetoothAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setTitle(R.string.activity_main_title)
        if (savedInstanceState == null) {
            mBluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
            // Is Bluetooth supported on this device?
            if (mBluetoothAdapter != null) {
                // Is Bluetooth turned on?
                if (mBluetoothAdapter!!.isEnabled) {
                    // Are Bluetooth Advertisements supported on this device?
                    if (mBluetoothAdapter!!.isMultipleAdvertisementSupported) {
                        // Everything is supported and enabled, load the fragments.
                        setupFragments()
                    } else {
                        // Bluetooth Advertisements are not supported.
                        showErrorText(R.string.bt_ads_not_supported)
                    }
                } else {
                    // Prompt user to turn on Bluetooth (logic continues in onActivityResult()).
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT)
                }
            } else {
                // Bluetooth is not supported.
                showErrorText(R.string.bt_not_supported)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            Constants.REQUEST_ENABLE_BT -> {
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now Enabled, are Bluetooth Advertisements supported on
                    // this device?
                    if (mBluetoothAdapter!!.isMultipleAdvertisementSupported) {
                        // Everything is supported and enabled, load the fragments.
                        setupFragments()
                    } else {
                        // Bluetooth Advertisements are not supported.
                        showErrorText(R.string.bt_ads_not_supported)
                    }
                } else {
                    // User declined to enable Bluetooth, exit the app.
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show()
                    finish()
                }
                super.onActivityResult(requestCode, resultCode, data)
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun setupFragments() {
        val transaction = supportFragmentManager.beginTransaction()
        val scannerFragment = ScannerFragment()
        // Fragments can't access system services directly, so pass it the BluetoothAdapter
        scannerFragment.setBluetoothAdapter(mBluetoothAdapter!!)
        transaction.replace(R.id.scanner_fragment_container, scannerFragment)
        val advertiserFragment = AdvertiserFragment()
        transaction.replace(R.id.advertiser_fragment_container, advertiserFragment)
        transaction.commit()
    }

    private fun showErrorText(messageId: Int) {
        (error_textview as TextView).text = getString(messageId)
    }
}
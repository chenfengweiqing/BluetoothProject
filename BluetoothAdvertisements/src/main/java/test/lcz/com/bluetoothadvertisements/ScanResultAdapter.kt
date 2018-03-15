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

import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

import java.util.ArrayList
import java.util.concurrent.TimeUnit

/**
 * Holds and displays [ScanResult]s, used by [ScannerFragment].
 */
class ScanResultAdapter internal constructor(private val mContext: Context, private val mInflater: LayoutInflater) : BaseAdapter() {

    private val mArrayList: ArrayList<ScanResult>

    init {
        mArrayList = ArrayList()
    }

    override fun getCount(): Int {
        return mArrayList.size
    }

    override fun getItem(position: Int): Any {
        return mArrayList[position]
    }

    override fun getItemId(position: Int): Long {
        return mArrayList[position].device.address.hashCode().toLong()
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        var view = view
        // Reuse an old view if we can, otherwise create a new one.
        if (view == null) {
            view = mInflater.inflate(R.layout.listitem_scanresult, null)
        }
        val deviceNameView = view!!.findViewById<View>(R.id.device_name) as TextView
        val deviceAddressView = view.findViewById<View>(R.id.device_address) as TextView
        val lastSeenView = view.findViewById<View>(R.id.last_seen) as TextView
        val scanResult = mArrayList[position]
        var name: String? = scanResult.device.name
        if (name == null) {
            name = mContext.resources.getString(R.string.no_name)
        }
        deviceNameView.text = name
        deviceAddressView.text = scanResult.device.address
        lastSeenView.text = getTimeSinceString(mContext, scanResult.timestampNanos)
        return view
    }

    /**
     * Search the adapter for an existing device address and return it, otherwise return -1.
     */
    private fun getPosition(address: String): Int {
        return mArrayList.indices.firstOrNull { mArrayList[it].device.address == address } ?: -1
    }


    /**
     * Add a ScanResult item to the adapter if a result from that device isn't already present.
     * Otherwise updates the existing position with the new ScanResult.
     */
    fun add(scanResult: ScanResult) {
        val existingPosition = getPosition(scanResult.device.address)
        if (existingPosition >= 0) {
            // Device is already in list, update its record.
            mArrayList[existingPosition] = scanResult
        } else {
            // Add new Device's ScanResult to list.
            mArrayList.add(scanResult)
        }
    }

    /**
     * Clear out the adapter.
     */
    fun clear() {
        mArrayList.clear()
    }

    companion object {
        /**
         * Takes in a number of nanoseconds and returns a human-readable string giving a vague
         * description of how long ago that was.
         */
        fun getTimeSinceString(context: Context, timeNanoseconds: Long): String {
            var lastSeenText = context.resources.getString(R.string.last_seen) + " "
            val timeSince = SystemClock.elapsedRealtimeNanos() - timeNanoseconds
            val secondsSince = TimeUnit.SECONDS.convert(timeSince, TimeUnit.NANOSECONDS)
            if (secondsSince < 5) {
                lastSeenText += context.resources.getString(R.string.just_now)
            } else if (secondsSince < 60) {
                lastSeenText += secondsSince.toString() + " " + context.resources
                        .getString(R.string.seconds_ago)
            } else {
                val minutesSince = TimeUnit.MINUTES.convert(secondsSince, TimeUnit.SECONDS)
                if (minutesSince < 60) {
                    if (minutesSince == 1L) {
                        lastSeenText += minutesSince.toString() + " " + context.resources
                                .getString(R.string.minute_ago)
                    } else {
                        lastSeenText += minutesSince.toString() + " " + context.resources
                                .getString(R.string.minutes_ago)
                    }
                } else {
                    val hoursSince = TimeUnit.HOURS.convert(minutesSince, TimeUnit.MINUTES)
                    lastSeenText += if (hoursSince == 1L) {
                        hoursSince.toString() + " " + context.resources
                                .getString(R.string.hour_ago)
                    } else {
                        hoursSince.toString() + " " + context.resources
                                .getString(R.string.hours_ago)
                    }
                }
            }
            return lastSeenText
        }
    }
}

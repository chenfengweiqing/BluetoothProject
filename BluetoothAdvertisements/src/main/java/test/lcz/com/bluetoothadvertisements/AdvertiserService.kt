package test.lcz.com.bluetoothadvertisements

import android.app.Service
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast

import java.util.concurrent.TimeUnit

/**
 * Manages BLE Advertising independent of the main app.
 * If the app goes off screen (or gets killed completely) advertising can continue because this
 * Service is maintaining the necessary Callback in memory.
 */
class AdvertiserService : Service() {
    private var mBluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var mAdvertiseCallback: AdvertiseCallback? = null
    private var mHandler: Handler? = null
    private var timeoutRunnable: Runnable? = null

    /**
     * Length of time to allow advertising before automatically shutting off. (10 minutes)
     */
    private val TIMEOUT = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES)

    override fun onCreate() {
        running = true
        initialize()
        startAdvertising()
        setTimeout()
        super.onCreate()
    }

    override fun onDestroy() {
        /**
         * Note that onDestroy is not guaranteed to be called quickly or at all. Services exist at
         * the whim of the system, and onDestroy can be delayed or skipped entirely if memory need
         * is critical.
         */
        running = false
        stopAdvertising()
        mHandler!!.removeCallbacks(timeoutRunnable)
        super.onDestroy()
    }

    /**
     * Required for extending service, but this will be a Started Service only, so no need for
     * binding.
     */
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    /**
     * Get references to system Bluetooth objects if we don't have them already.
     */
    private fun initialize() {
        if (mBluetoothLeAdvertiser == null) {
            val mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (mBluetoothManager != null) {
                val mBluetoothAdapter = mBluetoothManager.adapter
                if (mBluetoothAdapter != null) {
                    mBluetoothLeAdvertiser = mBluetoothAdapter.bluetoothLeAdvertiser
                } else {
                    Toast.makeText(this, getString(R.string.bt_null), Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, getString(R.string.bt_null), Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Starts a delayed Runnable that will cause the BLE Advertising to timeout and stop after a
     * set amount of time.
     */
    private fun setTimeout() {
        mHandler = Handler()
        timeoutRunnable = Runnable {
            Log.d(TAG, "AdvertiserService has reached timeout of $TIMEOUT milliseconds, stopping advertising.")
            sendFailureIntent(ADVERTISING_TIMED_OUT)
            stopSelf()
        }
        mHandler!!.postDelayed(timeoutRunnable, TIMEOUT)
    }

    /**
     * Starts BLE Advertising.
     */
    private fun startAdvertising() {
        Log.d(TAG, "Service: Starting Advertising")
        if (mAdvertiseCallback == null) {
            val settings = buildAdvertiseSettings()
            val data = buildAdvertiseData()
            mAdvertiseCallback = SampleAdvertiseCallback()

            if (mBluetoothLeAdvertiser != null) {
                mBluetoothLeAdvertiser!!.startAdvertising(settings, data,
                        mAdvertiseCallback)
            }
        }
    }

    /**
     * Stops BLE Advertising.
     */
    private fun stopAdvertising() {
        Log.d(TAG, "Service: Stopping Advertising")
        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser!!.stopAdvertising(mAdvertiseCallback)
            mAdvertiseCallback = null
        }
    }

    /**
     * Returns an AdvertiseData object which includes the Service UUID and Device Name.
     */
    private fun buildAdvertiseData(): AdvertiseData {

        /**
         * Note: There is a strict limit of 31 Bytes on packets sent over BLE Advertisements.
         * This includes everything put into AdvertiseData including UUIDs, device info, &
         * arbitrary service or manufacturer data.
         * Attempting to send packets over this limit will result in a failure with error code
         * AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE. Catch this error in the
         * onStartFailure() method of an AdvertiseCallback implementation.
         */
        val dataBuilder = AdvertiseData.Builder()
        dataBuilder.addServiceUuid(Constants.Service_UUID)
        dataBuilder.setIncludeDeviceName(true)
        /* For example - this will cause advertising to fail (exceeds size limit) */
        //String failureData = "asdghkajsghalkxcjhfa;sghtalksjcfhalskfjhasldkjfhdskf";
        //dataBuilder.addServiceData(Constants.Service_UUID, failureData.getBytes());
        return dataBuilder.build()
    }

    /**
     * Returns an AdvertiseSettings object set to use low power (to help preserve battery life)
     * and disable the built-in timeout since this code uses its own timeout runnable.
     */
    private fun buildAdvertiseSettings(): AdvertiseSettings {
        val settingsBuilder = AdvertiseSettings.Builder()
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
        settingsBuilder.setTimeout(0)
        return settingsBuilder.build()
    }

    /**
     * Custom callback after Advertising succeeds or fails to start. Broadcasts the error code
     * in an Intent to be picked up by AdvertiserFragment and stops this Service.
     */
    private inner class SampleAdvertiseCallback : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.d(TAG, "Advertising failed")
            sendFailureIntent(errorCode)
            stopSelf()

        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            Log.d(TAG, "Advertising successfully started")
        }
    }

    /**
     * Builds and sends a broadcast intent indicating Advertising has failed. Includes the error
     * code as an extra. This is intended to be picked up by the `AdvertiserFragment`.
     */
    private fun sendFailureIntent(errorCode: Int) {
        val failureIntent = Intent()
        failureIntent.action = ADVERTISING_FAILED
        failureIntent.putExtra(ADVERTISING_FAILED_EXTRA_CODE, errorCode)
        sendBroadcast(failureIntent)
    }

    companion object {

        private val TAG = AdvertiserService::class.java.simpleName
        /**
         * A global variable to let AdvertiserFragment check if the Service is running without needing
         * to start or bind to it.
         * This is the best practice method as defined here:
         * https://groups.google.com/forum/#!topic/android-developers/jEvXMWgbgzE
         */
        var running = false
        val ADVERTISING_FAILED = "com.example.android.bluetoothadvertisements.advertising_failed"
        val ADVERTISING_FAILED_EXTRA_CODE = "failureCode"
        val ADVERTISING_TIMED_OUT = 6
    }

}
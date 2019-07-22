package com.dome.fingerprintscan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.fingerprint.FingerprintManager
import android.os.CancellationSignal
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat


/**
 * Created by dome
 */
class FingerprintHelper(context: Context) : FingerprintManager.AuthenticationCallback() {
    private var cancellationSignal: CancellationSignal? = null
    private var context: Context? = context
    private lateinit var myInterface: MyInterface


    fun myInterface(myInterface: MyInterface) {
        this.myInterface = myInterface
    }

    interface MyInterface {
        fun onSuccess(isSuccess: Boolean)
    }

    fun startAuth(
        manager: FingerprintManager,
        cryptoObject: FingerprintManager.CryptoObject
    ) {

        cancellationSignal = CancellationSignal()

        if (ActivityCompat.checkSelfPermission(
                this!!.context!!,
                Manifest.permission.USE_FINGERPRINT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        manager.authenticate(cryptoObject, cancellationSignal, 0, this, null)
    }

    fun stopListening() {
        if (cancellationSignal != null) {
            cancellationSignal!!.cancel()
            cancellationSignal = null
        }
    }

    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        super.onAuthenticationError(errorCode, errString)
        Log.e("FingerprintHelper", "onAuthenticationError:$errString")
    }

    override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence) {
        super.onAuthenticationHelp(helpCode, helpString)
        Toast.makeText(
            context,
            "Authentication help\n$helpString",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult) {
        super.onAuthenticationSucceeded(result)
        Toast.makeText(
            context,
            "Is Correct",
            Toast.LENGTH_SHORT
        ).show()
        myInterface.onSuccess(true)
    }

    override fun onAuthenticationFailed() {
        super.onAuthenticationFailed()
        Toast.makeText(
            context,
            "Is Wrong",
            Toast.LENGTH_SHORT
        ).show()
        myInterface!!.onSuccess(false)

    }
}
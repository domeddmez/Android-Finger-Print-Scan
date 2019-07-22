package com.dome.fingerprintscan

import android.Manifest
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.pm.PackageManager
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.security.*
import java.security.cert.CertificateException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey


open class MainActivity : AppCompatActivity(), FingerprintHelper.MyInterface {
    lateinit var mFingerprintManager: FingerprintManager
    lateinit var mKeyguardManager: KeyguardManager

    private val KEY_NAME = "key_finger"
    private var mKeyStore: KeyStore? = null
    private lateinit var mKeyGenerator: KeyGenerator
    lateinit var cipher: Cipher
    private var mFingerprintHelper: FingerprintHelper? = null
    private var mCryptoObject: FingerprintManager.CryptoObject? = null


    @SuppressLint("ServiceCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mKeyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        mFingerprintManager = getSystemService(FINGERPRINT_SERVICE) as FingerprintManager
        init()
    }

    private fun init() {
        // Checking fingerprint permission.
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.USE_FINGERPRINT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Fingerprint authentication permission not enabled", Toast.LENGTH_LONG).show()
        }

        // Checking Device support fingerprint or not.
        if (mFingerprintManager.isHardwareDetected) {

            // Not setting lock screen with imprint.
            if (!mKeyguardManager.isKeyguardSecure) {
                Toast.makeText(this, "Open Finger Print In Your Device", Toast.LENGTH_LONG).show()
            }

            // Not registered at least one fingerprint in Settings.
            if (!mFingerprintManager.hasEnrolledFingerprints()) {
                Toast.makeText(this, "Add Finger Print In Your Device", Toast.LENGTH_LONG).show()
            }

            generateKey()
            if (initCipher()) {
                mCryptoObject = FingerprintManager.CryptoObject(cipher)
                mFingerprintHelper = FingerprintHelper(this)
                mFingerprintHelper!!.myInterface(this)

            }
        }
    }

    private fun generateKey() {
        try {
            mKeyStore = KeyStore.getInstance("AndroidKeyStore")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            mKeyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(
                "Failed to get KeyGenerator instance", e
            )
        } catch (e: NoSuchProviderException) {
            throw RuntimeException("Failed to get KeyGenerator instance", e)
        }

        try {
            mKeyStore!!.load(null)
            mKeyGenerator.init(
                KeyGenParameterSpec.Builder(
                    KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(
                        KeyProperties.ENCRYPTION_PADDING_PKCS7
                    )
                    .build()
            )
            mKeyGenerator.generateKey()
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: InvalidAlgorithmParameterException) {
            throw RuntimeException(e)
        } catch (e: CertificateException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    }

    private fun initCipher(): Boolean {
        try {
            cipher = Cipher.getInstance(
                KeyProperties.KEY_ALGORITHM_AES + "/"
                        + KeyProperties.BLOCK_MODE_CBC + "/"
                        + KeyProperties.ENCRYPTION_PADDING_PKCS7
            )
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to get Cipher", e)
        } catch (e: NoSuchPaddingException) {
            throw RuntimeException("Failed to get Cipher", e)
        }

        try {
            mKeyStore!!.load(null)
            val key = mKeyStore!!.getKey(KEY_NAME, null) as SecretKey
            cipher.init(Cipher.ENCRYPT_MODE, key)
            return true
        } catch (e: KeyPermanentlyInvalidatedException) {
            return false
        } catch (e: KeyStoreException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: CertificateException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: UnrecoverableKeyException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: IOException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: InvalidKeyException) {
            throw RuntimeException("Failed to init Cipher", e)
        }
    }

    override fun onResume() {
        super.onResume()
        if (mCryptoObject != null) {
            mFingerprintHelper?.startAuth(mFingerprintManager, mCryptoObject!!)
        }
    }

    override fun onPause() {
        super.onPause()
        mFingerprintHelper?.stopListening()
    }

    override fun onSuccess(isSuccess: Boolean) {
        if (isSuccess) {
            imgCorrect.visibility = View.VISIBLE
            imgWrong.visibility = View.INVISIBLE
        } else {
            imgCorrect.visibility = View.INVISIBLE
            imgWrong.visibility = View.VISIBLE
        }
    }

}

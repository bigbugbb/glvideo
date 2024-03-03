package com.binbo.glvideo.sample_app

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.binbo.glvideo.sample_app.databinding.ActivityMainBinding
import com.binbo.glvideo.sample_app.utils.rxbus.HeartBeatManager
import com.tbruyelle.rxpermissions3.RxPermissions

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.navigation_capture, R.id.navigation_video, R.id.navigation_advanced)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.viewNav.setupWithNavController(navController)

        RxPermissions(this)
            .request(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .subscribe { granted ->
                if (granted) {
                    Log.i(TAG, "permission granted")
                } else {
                    Log.i(TAG, "permission NOT granted")
                }
            }
    }

    companion object {
        private const val TAG = "MainActivity"

        init {
            System.loadLibrary("ffmpeg_player")
        }
    }
}
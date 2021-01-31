package com.example.demo

import android.Manifest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import com.master.permissionhelper.PermissionHelper

class MainActivityKt : AppCompatActivity() {
    private val TAG = "MainActivity"

    internal var permissionHelper: PermissionHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        permissionHelper = PermissionHelper(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CALL_PHONE), 100)

        //Short Way
        permissionHelper?.denied {
            if (it) {
                Log.d(TAG, "onPermissionDeniedBySystem() called")
                permissionHelper?.openAppDetailsActivity()
            } else {
                Log.d(TAG, "onPermissionDenied() called")
            }
        }

        permissionHelper?.requestIndividual {
            Log.d(TAG, "Individual Permission Granted")
        }

        permissionHelper?.requestAll {
            Log.d(TAG, "All permission granted")
        }


    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHelper?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
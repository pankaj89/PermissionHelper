package com.master.permissionhelper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.util.Log
import java.util.ArrayList

class PermissionHelper {

    private val TAG = "PermissionHelperJava"
    private val permissionHelper: PermissionHelperJava? = null
    private var REQUEST_CODE: Int = 0

    private var activity: Activity? = null
    private var fragment: Fragment? = null
    private var permissions: Array<String>? = null
    private var mPermissionCallback: PermissionCallback? = null
    private var showRational: Boolean = false

    //=========Constructors - START=========
    constructor(activity: Activity, fragment: Fragment, permissions: Array<String>, requestCode: Int) {
        this.activity = activity
        this.fragment = fragment
        this.permissions = permissions
        this.REQUEST_CODE = requestCode
        checkIfPermissionPresentInAndroidManifest()
    }

    constructor(activity: Activity, permissions: Array<String>, requestCode: Int = 100) {
        this.activity = activity
        this.permissions = permissions
        this.REQUEST_CODE = requestCode
        checkIfPermissionPresentInAndroidManifest()
    }

    constructor(fragment: Fragment, permissions: Array<String>, requestCode: Int = 100) {
        this.fragment = fragment
        this.permissions = permissions
        this.REQUEST_CODE = requestCode
        checkIfPermissionPresentInAndroidManifest()
    }

    private fun checkIfPermissionPresentInAndroidManifest() {
        for (permission in permissions!!) {
            if (hasPermissionInManifest(permission) == false) {
                throw RuntimeException("Permission ($permission) Not Declared in manifest")
            }
        }
    }

    //=========Constructors- END=========
    fun request(permissionCallback: PermissionCallback?) {
        this.mPermissionCallback = permissionCallback
        if (hasPermission() == false) {
            showRational = shouldShowRational(permissions!!)
            if (activity != null)
                ActivityCompat.requestPermissions(activity!!, filterNotGrantedPermission(permissions!!), REQUEST_CODE)
            else
                fragment!!.requestPermissions(filterNotGrantedPermission(permissions!!), REQUEST_CODE)
        } else {
            Log.i(TAG, "PERMISSION: Permission Granted")
            mPermissionCallback?.onPermissionGranted()
            requestAllCallback()
        }
    }

    private var requestAllCallback: () -> Unit? = fun() {};
    fun requestAll(callback: () -> Unit) {
        this.requestAllCallback = callback;
        request(null)
    }

    private var requestIndividualCallback: (grantedPermission: Array<String>) -> Unit? = fun(grantedPermission: Array<String>) {};
    fun requestIndividual(callback: (grantedPermission: Array<String>) -> Unit) {
        this.requestIndividualCallback = callback;
        request(null)
    }

    private var deniedCallback: (isSystem: Boolean) -> Unit? = fun(isSystem: Boolean) {};
    fun denied(callback: (isSystem: Boolean) -> Unit) {
        this.deniedCallback = callback;
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {

        if (requestCode == REQUEST_CODE) {
            var denied = false
            var i = 0
            val grantedPermissions = ArrayList<String>()
            for (grantResult in grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    denied = true
                } else {
                    grantedPermissions.add(permissions[i])
                }
                i++
            }

            if (denied) {
                val currentShowRational = shouldShowRational(permissions)
                if (showRational == false && currentShowRational == false) {
                    Log.d(TAG, "PERMISSION: Permission Denied By System")
                    mPermissionCallback?.onPermissionDeniedBySystem()
                    deniedCallback(true)
                } else {
                    Log.i(TAG, "PERMISSION: Permission Denied")
                    //Checking if any single individual permission is granted then show user that permission
                    if (!grantedPermissions.isEmpty()) {

                        mPermissionCallback?.onIndividualPermissionGranted(grantedPermissions.toTypedArray())
                        requestIndividualCallback(grantedPermissions.toTypedArray())
                    }

                    mPermissionCallback?.onPermissionDenied()
                    deniedCallback(false)

                }
            } else {
                Log.i(TAG, "PERMISSION: Permission Granted")

                mPermissionCallback?.onPermissionGranted()
                requestAllCallback()
            }
        }
    }

    //====================================
    //====================================

    interface PermissionCallback {
        fun onPermissionGranted()

        fun onIndividualPermissionGranted(grantedPermission: Array<String>)

        fun onPermissionDenied()

        fun onPermissionDeniedBySystem()
    }


    private fun <T : Context> getContext(): T? {
        return if (activity != null) activity as T? else fragment!!.context as T
    }

    /**
     * Return list that is not granted and we need to ask for permission
     *
     * @param permissions
     * @return
     */
    private fun filterNotGrantedPermission(permissions: Array<String>): Array<String> {
        val notGrantedPermission = ArrayList<String>()
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(getContext()!!, permission) != PackageManager.PERMISSION_GRANTED) {
                notGrantedPermission.add(permission)
            }
        }
        return notGrantedPermission.toTypedArray()
    }

    /**
     * Check permission is there or not
     *
     * @param permissions
     * @return
     */
    fun hasPermission(): Boolean {
        for (permission in permissions!!) {
            if (ContextCompat.checkSelfPermission(getContext()!!, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    /**
     * Check permission is there or not for group of permissions
     *
     * @param permissions
     * @return
     */
    fun checkSelfPermission(permissions: Array<String>?): Boolean {
        for (permission in permissions!!) {
            if (ContextCompat.checkSelfPermission(getContext()!!, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }


    /**
     * Checking if there is need to show rational for group of permissions
     *
     * @param permissions
     * @return
     */
    private fun shouldShowRational(permissions: Array<String>): Boolean {
        var currentShowRational = false
        for (permission in permissions) {

            if (activity != null) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity!!, permission) == true) {
                    currentShowRational = true
                    break
                }
            } else {
                if (fragment!!.shouldShowRequestPermissionRationale(permission) == true) {
                    currentShowRational = true
                    break
                }
            }
        }
        return currentShowRational
    }

    //===================
    private fun hasPermissionInManifest(permission: String): Boolean {
        try {
            val context = if (activity != null) activity else fragment!!.activity
            val info = context?.getPackageManager()?.getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS)
            if (info?.requestedPermissions != null) {
                for (p in info?.requestedPermissions) {
                    if (p == permission) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    /**
     * Open current application detail activity so user can change permission manually.
     */
    fun openAppDetailsActivity() {
        if (getContext<Context>() == null) {
            return
        }
        val i = Intent()
        i.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        i.addCategory(Intent.CATEGORY_DEFAULT)
        i.data = Uri.parse("package:" + getContext<Context>()!!.packageName)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        getContext<Context>()!!.startActivity(i)
    }
}
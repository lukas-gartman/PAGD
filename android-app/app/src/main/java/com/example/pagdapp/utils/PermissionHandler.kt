package com.example.pagdapp.utils

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.pagdapp.utils.Constants.COARSE_PERMISSION_CODE
import com.example.pagdapp.utils.Constants.CUSTOM_PERMISSION_GRANTED
import com.example.pagdapp.utils.Constants.FINE_PERMISSION_CODE
import com.example.pagdapp.utils.Constants.NOTIFICATION_PERMISSION_CODE
import com.example.pagdapp.utils.Constants.READ_EXTERNAL_STORAGE_CODE
import com.example.pagdapp.utils.Constants.RECORD_PERMISSION_CODE
import com.example.pagdapp.utils.Constants.WRITE_EXTERNAL_STORAGE_CODE

class PermissionHandler(private val activity: AppCompatActivity) {


    fun requestAllPermissions() {
        requestMultiplePermissions.launch(requiredPermissions)
    }

    fun requestSinglePermission(permissionCode: Int) {
        val permission = getPermissionFromRequestCode(permissionCode)
        if (permission != CUSTOM_PERMISSION_GRANTED) {
            requestPermission(permission)
        }
    }

    fun areAllPermissionsGranted(): Boolean {
        requiredPermissions.forEach { permission ->
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                Log.e("initButtons", "$permission not granted")
                return false
            }
            Log.e("initButtons", "$permission granted")
        }
        return true
    }

    fun isPermissionGranted(permissionCode: Int) : Boolean{
        val permission = getPermissionFromRequestCode(permissionCode)
        if(permission == CUSTOM_PERMISSION_GRANTED) {
            return true
        }
        return checkPermission(permission)
    }

    private fun checkPermission(permissionToCheck: String): Boolean {
        return ContextCompat.checkSelfPermission(activity, permissionToCheck) == PackageManager.PERMISSION_GRANTED
    }

    private val requiredPermissions =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) else arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        )

    private val requestMultiplePermissions =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            permissions.entries.forEach {
                handlePermissionResult(it.key, it.value)
            }
        }


    private fun handlePermissionResult(permission: String, isGranted: Boolean) {
        if (!isGranted) {
            requestPermission(permission)
        }
    }

    private fun requestPermission(permission: String) {
        if (ContextCompat.checkSelfPermission(
                activity,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                val message = getPermissionRationaleMessage(permission)
                AlertDialog.Builder(activity)
                    .setTitle("Permission required")
                    .setMessage(message)
                    .setPositiveButton("Ok") { _, _ ->
                        ActivityCompat.requestPermissions(
                            activity,
                            arrayOf(permission),
                            getPermissionRequestCode(permission)
                        )
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }.create().show()
            } else {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(permission),
                    getPermissionRequestCode(permission)
                )
            }
        }
    }

    private fun getPermissionRationaleMessage(permission: String): String {
        return when (permission) {
            Manifest.permission.RECORD_AUDIO ->
                "This permission is needed to record the surrounding of potential gunshots."
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION ->
                "This permission is needed to allow localisation of potential gunshots. Your location will be anonymous."
            Manifest.permission.POST_NOTIFICATIONS ->
                "This permission is needed to notify you of potential gunshots."
            else -> "This permission is required for the app to function properly."
        }
    }

    private fun getPermissionRequestCode(permission: String): Int {
        return when (permission) {
            Manifest.permission.RECORD_AUDIO -> RECORD_PERMISSION_CODE
            Manifest.permission.ACCESS_FINE_LOCATION -> FINE_PERMISSION_CODE
            Manifest.permission.ACCESS_COARSE_LOCATION -> COARSE_PERMISSION_CODE
            Manifest.permission.POST_NOTIFICATIONS -> NOTIFICATION_PERMISSION_CODE
            Manifest.permission.READ_EXTERNAL_STORAGE -> READ_EXTERNAL_STORAGE_CODE
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> WRITE_EXTERNAL_STORAGE_CODE
            else -> throw IllegalArgumentException("Permission code not implemented")
        }
    }

    private fun getPermissionFromRequestCode(requestCode: Int): String {
        return when (requestCode) {
            RECORD_PERMISSION_CODE -> Manifest.permission.RECORD_AUDIO
            FINE_PERMISSION_CODE -> Manifest.permission.ACCESS_FINE_LOCATION
            COARSE_PERMISSION_CODE -> Manifest.permission.ACCESS_COARSE_LOCATION
            NOTIFICATION_PERMISSION_CODE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.POST_NOTIFICATIONS
            } else {
                CUSTOM_PERMISSION_GRANTED
            }
            READ_EXTERNAL_STORAGE_CODE -> Manifest.permission.READ_EXTERNAL_STORAGE
            WRITE_EXTERNAL_STORAGE_CODE -> Manifest.permission.WRITE_EXTERNAL_STORAGE
            else -> throw IllegalArgumentException("Permission does not exist")
        }
    }
}

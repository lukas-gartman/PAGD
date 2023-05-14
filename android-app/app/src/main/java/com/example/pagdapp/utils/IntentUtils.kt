package com.example.pagdapp.utils

import android.content.Intent
import android.os.Bundle
import com.example.pagdapp.data.model.GunshotData
import com.example.pagdapp.services.FirebaseGunshotEventService
import com.example.pagdapp.services.FirebaseGunshotEventService.Companion.EXTRA_COORD_ALT
import com.example.pagdapp.services.FirebaseGunshotEventService.Companion.EXTRA_GUN
import com.example.pagdapp.services.FirebaseGunshotEventService.Companion.EXTRA_GUNSHOT_ID
import com.example.pagdapp.services.FirebaseGunshotEventService.Companion.EXTRA_IS_UPDATE
import com.example.pagdapp.services.FirebaseGunshotEventService.Companion.EXTRA_LATITUDE
import com.example.pagdapp.services.FirebaseGunshotEventService.Companion.EXTRA_LONGITUDE
import com.example.pagdapp.services.FirebaseGunshotEventService.Companion.EXTRA_SHOTS_FIRED
import com.example.pagdapp.services.FirebaseGunshotEventService.Companion.EXTRA_TIMESTAMP

object IntentUtils {

    fun createBundleFromIntent(intent: Intent): Bundle {
        val bundle = Bundle()
        bundle.putInt(EXTRA_IS_UPDATE, intent.getIntExtra(EXTRA_IS_UPDATE, -1))
        bundle.putInt(EXTRA_GUNSHOT_ID, intent.getIntExtra(EXTRA_GUNSHOT_ID, -1))
        bundle.putLong(EXTRA_TIMESTAMP, intent.getLongExtra(EXTRA_TIMESTAMP, -1L))
        bundle.putDouble(EXTRA_LATITUDE, intent.getDoubleExtra(EXTRA_LATITUDE, 0.0))
        bundle.putDouble(EXTRA_LONGITUDE, intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0))
        bundle.putDouble(EXTRA_COORD_ALT, intent.getDoubleExtra(EXTRA_COORD_ALT, 0.0))
        bundle.putString(EXTRA_GUN, intent.getStringExtra(EXTRA_GUN))
        bundle.putInt(EXTRA_SHOTS_FIRED, intent.getIntExtra(EXTRA_SHOTS_FIRED, -1))
        return bundle
    }

    fun createGunShotFromIntent(intent: Intent): GunshotData? {
        val isUpdate = intent.getIntExtra(EXTRA_IS_UPDATE, -1)
        val gunshotId = intent.getIntExtra(EXTRA_GUNSHOT_ID, -1)
        val timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, -1L)
        val latitude = intent.getDoubleExtra(EXTRA_LATITUDE, Double.NaN)
        val longitude = intent.getDoubleExtra(EXTRA_LONGITUDE, Double.NaN)
        val coordAlt = intent.getDoubleExtra(EXTRA_COORD_ALT, Double.NaN)
        val gun = intent.getStringExtra(EXTRA_GUN)
        val shotsFired = intent.getIntExtra(EXTRA_SHOTS_FIRED, -1)

        return if (isUpdate != -1 && gunshotId != -1 && timestamp != -1L && !latitude.isNaN() &&
            !longitude.isNaN() && !coordAlt.isNaN() && gun != null && shotsFired != -1) {
            GunshotData(
                isUpdate = isUpdate,
                gunshot_id = gunshotId,
                timestamp = timestamp,
                coordLat = latitude,
                coordLong = longitude,
                coordAlt = coordAlt,
                gun = gun,
                shotsFired = shotsFired
            )
        } else {
            null
        }
    }


}
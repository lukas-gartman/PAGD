package com.example.pagdapp.utils

object Constants {

    const val RECORD_PERMISSION_CODE = 100
    const val COARSE_PERMISSION_CODE = 200
    const val FINE_PERMISSION_CODE = 201
    const val NOTIFICATION_PERMISSION_CODE = 300
    const val WRITE_EXTERNAL_STORAGE_CODE = 400
    const val READ_EXTERNAL_STORAGE_CODE = 401
    const val CUSTOM_PERMISSION_GRANTED = "No permission needed"

    const val AI_MODEL = "lite-model_yamnet_classification_tflite_1.tflite"

    const val ACTION_SHOW_MAP_FRAGMENT = "ACTION_SHOW_MAP_FRAGMENT"


    const val CONTINUOUS_NETWORK_CALL_LOW = 15000L
    const val CONTINUOUS_NETWORK_CALL_MIDDLE = 10000L
    const val CONTINUOUS_NETWORK_CALL_HIGH = 2000L

    const val ADD_GUN_SUCCESS = "Added gun to database"
    const val ADD_REPORT_SUCCESS = "Added report to database"
    const val ADD_MULTIPLE_REPORTS_SUCCESS = "Added multiple reports to database"
    const val GET_GUNS_SUCCESS = "Successfully retrieved guns"
    const val GET_REPORTS_SUCCESS = "Successfully retrieved reports"

    const val GET_GUNS_ERROR = "Failed to get guns"
    const val GET_REPORT_ERROR = "Failed to get reports"
    const val GET_GUNSHOTS_ERROR = "Failed to get gunshots"
    const val ADD_GUN_ERROR = "Failed to add gun"
    const val ADD_REPORT_ERROR = "Failed to add report"
    const val GET_ELEVATION_ERROR = "Failed to fetch elevation"

}
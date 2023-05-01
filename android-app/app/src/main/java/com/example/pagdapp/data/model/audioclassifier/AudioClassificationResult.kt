package com.example.pagdapp.data.model.audioclassifier

import java.sql.Timestamp

data class AudioClassificationResult(
    val timestamp: Long,
    val category: String,
    val specificType: String?,
    val score: Float
)

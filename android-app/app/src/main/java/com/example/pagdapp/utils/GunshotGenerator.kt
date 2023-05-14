package com.example.pagdapp.utils

import com.example.pagdapp.data.model.dbModels.Gun
import com.example.pagdapp.data.model.dbModels.Gunshot
import kotlin.random.Random

object GunshotGenerator {

    private val gunExamples = listOf(
        Gun("AK-47", "Assault Rifle"),
        Gun("AR-15", "Rifle"),
        Gun("Bolt action", "Rifle"),
        Gun("Colt 1911", "Handgun"),
        Gun("Finger snapping", "Finger snapping"),
        Gun("Glock", "Handgun"),
        Gun("Hands", "Hands"),
        Gun("HK USP", "Handgun"),
        Gun("Kimber", "Handgun"),
        Gun("Lorcin", "Handgun"),
        Gun("M16", "Rifle"),
        Gun("MP40", "Rifle"),
        Gun("Remington 700", "Rifle"),
        Gun("Ruger", "Handgun"),
        Gun("SIG Sauer", "Handgun"),
        Gun("SpKing", "Handgun"),
        Gun("SW22", "Handgun"),
        Gun("SW38sp", "Handgun"),
        Gun("WASR", "Rifle"),
        Gun("Win M14", "Rifle")
    )

    fun generateGunshotSamples(count: Int, lat: Float, long: Float): List<Gunshot> {
        val samples = mutableListOf<Gunshot>()

        for (i in 1..count) {
            val timestamp = System.currentTimeMillis()
            val coordLat = lat + nextFloat(-0.001f, 0.001f)
            val coordLong = long + nextFloat(-0.001f, 0.001f)
            val coordAlt = nextFloat(0.0f, 100.0f)
            val gun = gunExamples.random().name
            val shotsFired = Random.nextInt(1, 11)

            val gunshot = Gunshot(timestamp, coordLat, coordLong, coordAlt, gun, shotsFired)
            samples.add(gunshot)
        }

        return samples
    }

    private fun nextFloat(from: Float, until: Float): Float {
        require(until >= from) { "Random range is empty: [$from, $until)." }
        val size = until - from
        return from + (Random.nextFloat() * size)
    }


}
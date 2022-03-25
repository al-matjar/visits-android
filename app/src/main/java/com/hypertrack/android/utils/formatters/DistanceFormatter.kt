package com.hypertrack.android.utils.formatters

import android.util.Log
import com.hypertrack.android.models.Imperial
import com.hypertrack.android.models.Metric
import com.hypertrack.android.models.Unspecified
import com.hypertrack.android.repository.MeasurementUnitsRepository
import com.hypertrack.android.utils.DistanceValue
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.Steps
import com.hypertrack.logistics.android.github.R
import java.util.*

interface DistanceFormatter {
    //todo remove legacy
    fun formatDistance(meters: Int): String
    fun formatDistance(distance: DistanceValue): String
    fun formatSteps(steps: Steps): String
}

open class LocalizedDistanceFormatter(
    private val osUtilsProvider: OsUtilsProvider,
    private val measurementUnitsRepository: MeasurementUnitsRepository,
) : DistanceFormatter {
    private val shouldUseImperial: Boolean
        get() {
            return when (measurementUnitsRepository.getMeasurementUnits()) {
                Metric -> {
                    false
                }
                Imperial -> {
                    true
                }
                Unspecified -> {
                    Locale.getDefault().country in listOf("US", "LR", "MM")
                }
            }
        }

    override fun formatDistance(meters: Int): String {
        return if (shouldUseImperial) {
            val miles = meters / 1609.0
            when {
                miles >= 100 -> {
                    osUtilsProvider.stringFromResource(
                        R.string.miles,
                        "%.0f".format(miles.toFloat())
                    )
                }
                miles >= 1 -> {
                    osUtilsProvider.stringFromResource(
                        R.string.miles,
                        "%.1f".format(miles.toFloat())
                    )
                }
                else -> {
                    val feet = 0.3048 * meters
                    osUtilsProvider.stringFromResource(R.string.feet, "%.0f".format(feet.toFloat()))
                }
            }
        } else {
            val kms = meters / 1000.0
            when {
                kms >= 100 -> {
                    osUtilsProvider.stringFromResource(R.string.kms, "%.0f".format(kms.toFloat()))
                }
                kms >= 1 -> {
                    osUtilsProvider.stringFromResource(R.string.kms, "%.1f".format(kms.toFloat()))
                }
                else -> {
                    osUtilsProvider.stringFromResource(
                        R.string.meters,
                        "%.0f".format(meters.toFloat())
                    )
                }
            }
        }
    }

    override fun formatDistance(distance: DistanceValue): String {
        return formatDistance(distance.meters)
    }

    override fun formatSteps(steps: Steps): String {
        return osUtilsProvider.stringFromResource(R.string.steps, steps.steps)
    }
}

class MetersDistanceFormatter(
    private val osUtilsProvider: OsUtilsProvider,
    private val measurementUnitsRepository: MeasurementUnitsRepository,
) : LocalizedDistanceFormatter(
    osUtilsProvider,
    measurementUnitsRepository,
) {
    override fun formatDistance(meters: Int): String {
        return osUtilsProvider.stringFromResource(
            R.string.meters,
            meters
        )
    }

    override fun formatDistance(distance: DistanceValue): String {
        return formatDistance(distance.meters)
    }

}

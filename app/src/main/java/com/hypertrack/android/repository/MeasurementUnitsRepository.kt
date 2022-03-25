package com.hypertrack.android.repository

import com.hypertrack.android.models.Imperial
import com.hypertrack.android.models.MeasurementUnits
import com.hypertrack.android.models.Metric
import com.hypertrack.android.models.Unspecified

//todo destroy with user scope
class MeasurementUnitsRepository(
    private val preferencesRepository: PreferencesRepository
) {

    fun getMeasurementUnits(): MeasurementUnits {
        return when (preferencesRepository.measurementUnitsImperial) {
            false -> Metric
            true -> Imperial
            null -> Unspecified
        }
    }

    fun setMeasurementUnits(units: MeasurementUnits) {
        preferencesRepository.measurementUnitsImperial = when (units) {
            Imperial -> true
            Metric -> false
            Unspecified -> null
        }
    }

}

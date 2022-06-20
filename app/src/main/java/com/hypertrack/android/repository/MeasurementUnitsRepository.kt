package com.hypertrack.android.repository

import com.hypertrack.android.models.Imperial
import com.hypertrack.android.models.MeasurementUnits
import com.hypertrack.android.models.Metric
import com.hypertrack.android.models.Unspecified
import com.hypertrack.android.repository.preferences.PreferencesRepository
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.toNullable
import com.hypertrack.android.utils.toNullableWithErrorReporting

class MeasurementUnitsRepository(
    private val preferencesRepository: PreferencesRepository,
    private val crashReportsProvider: CrashReportsProvider
) {

    fun getMeasurementUnits(): MeasurementUnits {
        val measurementUnitsImperial = preferencesRepository
            .measurementUnitsImperial
            .load()
            .toNullableWithErrorReporting(crashReportsProvider)
        return when (measurementUnitsImperial) {
            false -> Metric
            true -> Imperial
            null -> Unspecified
        }
    }

    fun setMeasurementUnits(units: MeasurementUnits) {
        when (units) {
            Imperial -> true
            Metric -> false
            Unspecified -> null
        }.let { preferencesRepository.measurementUnitsImperial.save(it) }
    }

}

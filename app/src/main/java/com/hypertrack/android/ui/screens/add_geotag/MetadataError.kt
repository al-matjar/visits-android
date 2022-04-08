package com.hypertrack.android.ui.screens.add_geotag

sealed class MetadataError
object NoMetadata : MetadataError()
object EmptyMetadata : MetadataError()
data class DuplicateKeys(val keys: List<String>) : MetadataError()
data class EmptyValues(val keys: List<String>) : MetadataError()
data class EmptyKeys(val values: List<String>) : MetadataError()

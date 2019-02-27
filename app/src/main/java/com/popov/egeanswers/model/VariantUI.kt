package com.popov.egeanswers.model

import java.util.*

data class VariantUI(
        val number: Int,
        val year: Int,
        val publicationDate: Date,
        val isOffline: Boolean,
        val type: VariantType
)
package com.popov.egeanswers.model

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.RealmClass
import io.realm.annotations.Required
import java.util.*

@RealmClass
open class LarinOGEVariant(
        var number: Int,
        var year: Int,
        @Required
        var pdf: ByteArray,
        @Required
        var answers: RealmList<String>,
        @Required
        var publicationDate: Date = Calendar.getInstance().apply { set(Calendar.YEAR, year) }.time,
        @Ignore
        val type: VariantType = VariantType.OGE
) : RealmObject(), LarinVariant {

    constructor() : this(
            0,
            0,
            ByteArray(0),
            RealmList(),
            Date()
    )
}
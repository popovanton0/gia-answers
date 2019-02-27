package com.popov.egeanswers.model

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.RealmClass
import io.realm.annotations.Required
import java.util.*

@RealmClass
open class LarinEGEVariant constructor(
        var number: Int,
        var year: Int,
        @Required
        var pdf: ByteArray,
        @Required
        var part1Answers: RealmList<String>,
        var publicationDate: Date = Calendar.getInstance()
                .apply { set(Calendar.YEAR, year) }.time,
        @Required
        var part2Answers: ByteArray
) : RealmObject(), LarinVariant {

    constructor() : this(
            0,
            0,
            ByteArray(0),
            RealmList(),
            Date(),
            ByteArray(0)
    )
}


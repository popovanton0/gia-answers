package com.popov.egeanswers.dbMigrations

import io.realm.DynamicRealm
import io.realm.FieldAttribute
import io.realm.RealmMigration
import java.util.*

class Migration11 : RealmMigration {
    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        val schema = realm.schema
        var oldVersion = oldVersion

        if (oldVersion == 0L) {
            schema.create("LarinOGEVariant")
                    .addField("number", Int::class.javaPrimitiveType)
                    .addField("year", Int::class.javaPrimitiveType)
                    .addField("pdf", ByteArray::class.java, FieldAttribute.REQUIRED)
                    .addRealmListField("answers", String::class.java)
                    .setRequired("answers", true)
                    .addField("publicationDate", Date::class.java)
                    .setRequired("publicationDate", true)
            oldVersion++
        }
    }

}
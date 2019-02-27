package com.popov.egeanswers.dao

import com.popov.egeanswers.model.LarinOGEVariant
import com.popov.egeanswers.util.asLiveData
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmList
import io.realm.RealmResults
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class LarinOGEVariantDao {
    private val realm = Realm.getDefaultInstance()

    /*suspend fun getAllRaw() = suspendCoroutine<List<LarinOGEVariant>> { cont ->
        realm.where<LarinOGEVariant>().findAllAsync().addChangeListener { it ->
            if (it.isValid)
                cont.resume(it.map { it })
            else cont.resumeWithException(Exception("Not found"))
        }
    }*/

    suspend fun getAll() = suspendCoroutine<List<LarinOGEVariant>> { cont ->
        val findAllAsync = realm.where<LarinOGEVariant>().findAllAsync()
        var changeListener: RealmChangeListener<RealmResults<LarinOGEVariant>>? = null
        changeListener = RealmChangeListener {
            findAllAsync.removeChangeListener(changeListener)
            if (it.isValid)
                cont.resume(it)
            else cont.resumeWithException(Exception("Not found"))
        }
        findAllAsync.addChangeListener(changeListener)
    }

    fun getAllLiveData() = realm.where<LarinOGEVariant>().findAllAsync().asLiveData()

    suspend fun setAnswers(number: Int, answers: List<String>) = suspendCoroutine<Boolean> { cont ->
        realm.executeTransactionAsync(Realm.Transaction {
            val variant = it.where<LarinOGEVariant>()
                    .equalTo("number", number)
                    .findFirst()
            if (variant == null) {
                cont.resume(false)
                return@Transaction
            }
            variant.answers = RealmList(*answers.toTypedArray())
        }, Realm.Transaction.OnSuccess {
            try {
                cont.resume(true)
            } catch (ignored: Exception) {
            }
        }, Realm.Transaction.OnError {
            try {
                cont.resume(false)
            } catch (ignored: Exception) {
            }
        })
    }

    fun getVariant(number: Int) = realm.where<LarinOGEVariant>()
            .equalTo("number", number)
            .findFirstAsync()
            .asLiveData()

    suspend fun createVariant(
            number: Int,
            year: Int,
            pdf: ByteArray,
            answers: MutableList<String>,
            publicationDate: Date
    ) = suspendCoroutine<LarinOGEVariant> { cont ->
        realm.executeTransaction {
            val variant = realm.createObject<LarinOGEVariant>()
            variant.number = number
            variant.year = year
            variant.pdf = pdf
            variant.answers = RealmList(*answers.toTypedArray())
            variant.publicationDate = publicationDate
            cont.resume(variant)
        }
    }


    suspend fun deleteVariant(number: Int) = suspendCoroutine<Boolean> { cont ->
        realm.executeTransactionAsync(Realm.Transaction {
            it.where<LarinOGEVariant>()
                    .equalTo("number", number)
                    .findAll()
                    .deleteAllFromRealm()
        }, Realm.Transaction.OnSuccess {
            cont.resume(true)
        }, Realm.Transaction.OnError {
            cont.resume(false)
        })
    }

    fun deleteAll() = try {
        val vars = realm.where<LarinOGEVariant>().findAll()
        realm.executeTransaction {
            vars.deleteAllFromRealm()
        }
        true
    } catch (t: Throwable) {
        false
    }

    fun close() {
        realm.close()
    }
}
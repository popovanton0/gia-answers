package com.popov.egeanswers.dao

import com.popov.egeanswers.model.LarinEGEVariant
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

class LarinEGEVariantDao {
    private val realm = Realm.getDefaultInstance()

    /*suspend fun getAllRaw() = suspendCoroutine<List<LarinEGEVariant>> { cont ->
        realm.where<LarinEGEVariant>().findAllAsync().addChangeListener { it ->
            if (it.isValid)
                cont.resume(it.map { it })
            else cont.resumeWithException(Exception("Not found"))
        }
    }*/

    suspend fun getAll() = suspendCoroutine<List<LarinEGEVariant>> { cont ->
        val findAllAsync = realm.where<LarinEGEVariant>().findAllAsync()
        var changeListener: RealmChangeListener<RealmResults<LarinEGEVariant>>? = null
        changeListener = RealmChangeListener {
            findAllAsync.removeChangeListener(changeListener)
            if (it.isValid)
                cont.resume(it)
            else cont.resumeWithException(Exception("Not found"))
        }
        findAllAsync.addChangeListener(changeListener)
    }

    fun getAllLiveData() = realm.where<LarinEGEVariant>().findAllAsync().asLiveData()

    suspend fun setPart1Answers(number: Int, part1Answers: List<String>) = suspendCoroutine<Boolean> { cont ->
        realm.executeTransactionAsync(Realm.Transaction {
            val variant = it.where<LarinEGEVariant>()
                    .equalTo("number", number)
                    .findFirst()
            if (variant == null) {
                cont.resume(false)
                return@Transaction
            }
            variant.part1Answers = RealmList(*part1Answers.toTypedArray())
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

    suspend fun setPart2Answers(number: Int, part2Answers: ByteArray) = suspendCoroutine<Boolean> { cont ->
        realm.executeTransactionAsync(Realm.Transaction {
            val variant = it.where<LarinEGEVariant>()
                    .equalTo("number", number)
                    .findFirst()
            if (variant == null) {
                cont.resume(false)
                return@Transaction
            }
            variant.part2Answers = part2Answers
        }, Realm.Transaction.OnSuccess {
            cont.resume(true)
        }, Realm.Transaction.OnError {
            cont.resume(false)
        })
    }

    fun getVariant(number: Int) = realm.where<LarinEGEVariant>()
            .equalTo("number", number)
            .findFirstAsync()
            .asLiveData()

    suspend fun createVariant(
            number: Int,
            year: Int,
            pdf: ByteArray,
            part1Answers: MutableList<String>,
            publicationDate: Date,
            part2Answers: ByteArray
    ) = suspendCoroutine<LarinEGEVariant> { cont ->
        realm.executeTransaction {
            val variant = realm.createObject<LarinEGEVariant>()
            variant.number = number
            variant.year = year
            variant.pdf = pdf
            variant.part1Answers = RealmList(*part1Answers.toTypedArray())
            variant.publicationDate = publicationDate
            variant.part2Answers = part2Answers
            cont.resume(variant)
        }
    }


    suspend fun deleteVariant(number: Int) = suspendCoroutine<Boolean> { cont ->
        realm.executeTransactionAsync(Realm.Transaction {
            it.where<LarinEGEVariant>()
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
        val vars = realm.where<LarinEGEVariant>().findAll()
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
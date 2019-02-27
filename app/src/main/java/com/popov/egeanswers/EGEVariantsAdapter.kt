package com.popov.egeanswers

import android.os.Bundle
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import com.google.firebase.analytics.FirebaseAnalytics
import com.popov.egeanswers.model.VariantUI
import com.popov.egeanswers.ui.EGEVariantActivity
import org.jetbrains.anko.support.v4.startActivityForResult
import java.text.SimpleDateFormat
import java.util.*

class EGEVariantsAdapter(private val fragment: android.support.v4.app.Fragment, private var items: MutableList<VariantUI>) : RecyclerView.Adapter<EGEVariantsAdapter.ViewHolder>() {

    private val firebaseAnalytics = FirebaseAnalytics.getInstance(fragment.context!!)

    override fun getItemCount(): Int = items.size

    // Inflates the item views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.variant_item, parent, false))

    // Binds each answer in the List to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val variant = items[position]
        holder.variantNameTextView.text = fragment.getString(R.string.variant_ege) + variant.number.toString()
        holder.publicationDateTextView.text = SimpleDateFormat/*dd.MM.*/("yyyy", Locale.US).format(variant.publicationDate)
        if (variant.isOffline)
            holder.downloadButton.apply {
                setImageResource(R.drawable.ic_offline_pin_grey_600)
                holder.downloadButton.visibility = View.VISIBLE
                /*setOnClickListener {
                    context.alert(R.string.delete_confirmation) {
                        positiveButton(android.R.string.yes) {
                            if (variant.delete()) {
                                context.toast(context.getString(R.string.deletion_successful))
                                holder.downloadButton.visibility = View.GONE
                            }
                            else context.toast(context.getString(R.string.deletion_failed))
                        }
                        negativeButton(android.R.string.no) {}
                    }.show()
                }*/
            }
        else holder.downloadButton.visibility = View.GONE

        holder.cardView.setOnClickListener {
            val bundle = Bundle()
            bundle.putInt(FirebaseAnalytics.Param.ITEM_ID, variant.number)
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "ege")
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)

            fragment.startActivityForResult<EGEVariantActivity>(0, "varNumber" to variant.number, "varYear" to variant.year)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.cardView)
        val variantNameTextView: TextView = view.findViewById(R.id.variantNameTextView)
        val publicationDateTextView: TextView = view.findViewById(R.id.publicationDateTextView)
        val downloadButton: ImageButton = view.findViewById(R.id.downloadButton)
    }
}
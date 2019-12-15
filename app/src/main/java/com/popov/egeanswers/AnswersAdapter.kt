package com.popov.egeanswers

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class AnswersAdapter(private var items : MutableList<String>) : RecyclerView.Adapter<AnswersAdapter.ViewHolder>() {

    override fun getItemCount(): Int = items.size

    // Inflates the item views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.answer_item, parent, false))

    // Binds each answer in the List to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.answerTextView.text = "${position + 1}. ${items[position]}"
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Holds the TextView that will add each answer to
        val answerTextView: TextView = view.findViewById(R.id.answer_text)
    }
}


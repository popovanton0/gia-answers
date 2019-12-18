package com.popov.egeanswers.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.popov.egeanswers.OGEVariantsAdapter
import com.popov.egeanswers.R
import com.popov.egeanswers.model.VariantUI
import com.popov.egeanswers.util.observeNotNull
import com.popov.egeanswers.viewmodel.MainViewModel
import com.popov.egeanswers.viewmodel.OGEVariantsViewModel
import kotlinx.android.synthetic.main.fragment_variants.*

open class OGEVariantsFragment : Fragment() {

    protected var isOfflineOnly = false
    private lateinit var m: OGEVariantsViewModel

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this.context!!)
        firebaseAnalytics.setCurrentScreen(this.activity!!, "oge-fragment", null)

        m = ViewModelProviders.of(this.activity!!)
                .get(MainViewModel::class.java)
                .getOGEViewModel(isOfflineOnly)

        val variants = mutableListOf<VariantUI>()

        variantsView.layoutManager = LinearLayoutManager(this.context)
        variantsView.adapter = OGEVariantsAdapter(this, variants)

        m.variants.observeNotNull(this, Observer {
            variants.clear()
            variants.addAll(it)

            variantsLoadingProgressBar.visibility = View.GONE
            if (it.isEmpty()) {
                noVarsTextView.visibility = View.VISIBLE
                noVarsImageView.visibility = View.VISIBLE
            } else {
                noVarsTextView.visibility = View.GONE
                noVarsImageView.visibility = View.GONE
            }

            variantsView.adapter?.notifyDataSetChanged()
        })

        m.varsLoadingErrorSnackbar.observeNotNull(this, Observer {
            variantsLoadingError(it)
            variantsLoadingProgressBar.visibility = View.GONE
        })

        searchFAB.setOnClickListener {
            val searchEditText = EditText(context)
            searchEditText.setPadding(10, 10, 10, 10)
            searchEditText.inputType = InputType.TYPE_CLASS_NUMBER
            searchEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    try {
                        val text = searchEditText.text.toString()
                        if (text.isEmpty()) return
                        text.toInt()
                    } catch (e: Exception) {
                        searchEditText.setText(searchEditText.text.substring(0, searchEditText.text.length - 1))
                    }
                }

                override fun afterTextChanged(s: Editable?) {
                }

            })
            val inputMethodManager = this.activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            AlertDialog.Builder(this.context!!)
                    .setTitle(android.R.string.search_go)
                    .setMessage(R.string.variant_search)
                    .setView(searchEditText)
                    .setNegativeButton(android.R.string.cancel) { dialog, which -> }
                    .setPositiveButton(android.R.string.search_go) { dialog, which ->
                        inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
                        val loadingAlert = AlertDialog.Builder(context!!)
                                .setTitle(R.string.loading_title)
                                .setMessage(R.string.searching_message)
                                .setCancelable(false)
                                .create()
                        loadingAlert.setCanceledOnTouchOutside(false)
                        loadingAlert.show()

                        if (searchEditText.text.toString().isEmpty()) {
                            loadingAlert.setCancelable(true)
                            loadingAlert.cancel()
                            return@setPositiveButton
                        }

                        val varNumber = searchEditText.text.toString().toInt()
                        m.search(varNumber) { isSuccess, isNotFound, varYear ->
                            loadingAlert.setCancelable(true)
                            loadingAlert.cancel()

                            if (isSuccess && !isNotFound) {
                                val intent = Intent(this.context!!, OGEVariantActivity::class.java).apply {
                                    putExtra("varNumber", varNumber)
                                    putExtra("varYear", varYear)
                                }
                                startActivityForResult(intent, 0)
                            } else if (isNotFound)
                                Snackbar.make(variantsRootLayout, R.string.varinat_not_found, Snackbar.LENGTH_LONG).show()
                            else Snackbar.make(variantsRootLayout, R.string.variants_search_error, Snackbar.LENGTH_LONG).show()
                        }
                    }.show()
        }
    }

    private fun variantsLoadingError(errorMessage: String) {
        Snackbar.make(variantsView, R.string.variants_loading_error, Snackbar.LENGTH_LONG)
                .setAction(R.string.error_details) {
                    AlertDialog.Builder(this.context!!)
                            .setTitle(getString(R.string.error_details))
                            .setMessage(errorMessage)
                            .show()
                }.show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_variants, container, false)
    }
}

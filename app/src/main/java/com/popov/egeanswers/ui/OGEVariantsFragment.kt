package com.popov.egeanswers.ui

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.google.firebase.analytics.FirebaseAnalytics
import com.popov.egeanswers.OGEVariantsAdapter
import com.popov.egeanswers.R
import com.popov.egeanswers.model.VariantUI
import com.popov.egeanswers.viewmodel.OGEVariantsViewModel
import com.popov.egeanswers.viewmodel.VariantsViewModelFactory
import kotlinx.android.synthetic.main.fragment_variants.*
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.dip
import org.jetbrains.anko.support.v4.startActivityForResult


open class OGEVariantsFragment : Fragment() {

    protected var isOfflineOnly = false
    private lateinit var m: OGEVariantsViewModel

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this.context!!)
        firebaseAnalytics.setCurrentScreen(this.activity!!, "oge-fragment", null)

        m = ViewModelProviders
                .of(this, VariantsViewModelFactory(isOfflineOnly, this.activity!!.application))
                .get(OGEVariantsViewModel::class.java)

        val variants = mutableListOf<VariantUI>()

        variantsView.layoutManager = LinearLayoutManager(this.context)
        variantsView.adapter = OGEVariantsAdapter(this, variants)

        m.getVariantsLiveData().observe(this, Observer {
            if (it == null) return@Observer
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

        m.varsLoadingErrorSnackbar.observe(this, Observer {
            if (it != null) {
                variantsLoadingError(it)
                variantsLoadingProgressBar.visibility = View.GONE
            }
        })

        searchFAB.setOnClickListener {
            val searchEditText = EditText(context)
            searchEditText.setPadding(dip(10), dip(10), dip(10), dip(10))
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
            alert(R.string.variant_search, android.R.string.search_go) {
                customView = searchEditText
                negativeButton(android.R.string.cancel) {}
                positiveButton(android.R.string.search_go) {
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
                        return@positiveButton
                    }

                    val varNumber = searchEditText.text.toString().toInt()
                    m.search(varNumber) { isSuccess, isNotFound, varYear ->
                        loadingAlert.setCancelable(true)
                        loadingAlert.cancel()

                        if (isSuccess && !isNotFound)
                            startActivityForResult<OGEVariantActivity>(0, "varNumber" to varNumber, "varYear" to varYear)
                        else if (isNotFound)
                            Snackbar.make(variantsRootLayout, R.string.varinat_not_found, Snackbar.LENGTH_LONG).show()
                        else Snackbar.make(variantsRootLayout, R.string.variants_search_error, Snackbar.LENGTH_LONG).show()
                    }
                }
            }.show()
        }
    }

    private fun variantsLoadingError(errorMessage: String) {
        Snackbar.make(variantsView, R.string.variants_loading_error, Snackbar.LENGTH_LONG)
                .setAction(R.string.error_details) {
                    alert(errorMessage, getString(R.string.error_details)).show()
                }.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return
        val varNumber = data?.getIntExtra("varNumber", 0) ?: 0
        val isOffline = data?.getBooleanExtra("isOffline", false) ?: false
        //m.changeIsOfflineForVariant()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_variants, container, false)
    }
}

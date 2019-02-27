package com.popov.egeanswers.ui

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.popov.egeanswers.R
import com.popov.egeanswers.model.UserType
import com.popov.egeanswers.viewmodel.IntroUserParamsViewModel
import kotlinx.android.synthetic.main.fragment_intro_user_params.*


class IntroUserParamsFragment : Fragment() {

    companion object {
        fun newInstance() = IntroUserParamsFragment()
    }

    private lateinit var m: IntroUserParamsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_intro_user_params, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        m = ViewModelProviders.of(this).get(IntroUserParamsViewModel::class.java)

        userTypeSpinner.adapter = ArrayAdapter<String>(this.context!!, R.layout.spinner_item,
                resources.getStringArray(R.array.list_user_role)
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }


        userTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                m.userType.postValue(
                        when (position) {
                            0 -> {
                                userClassSpinner.visibility = View.VISIBLE
                                UserType.STUDENT
                            }
                            1 -> {
                                userClassSpinner.visibility = View.INVISIBLE
                                UserType.TEACHER
                            }
                            else -> throw IllegalStateException("Only 2 elements in a userType spinner")
                        }
                )
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        userClassSpinner.adapter = ArrayAdapter<String>(this.context!!, R.layout.spinner_item,
                resources.getStringArray(R.array.list_user_class))
                .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        userClassSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                m.userClass.postValue(position + 8)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        m.doneButton.sendAction(0)
    }
}

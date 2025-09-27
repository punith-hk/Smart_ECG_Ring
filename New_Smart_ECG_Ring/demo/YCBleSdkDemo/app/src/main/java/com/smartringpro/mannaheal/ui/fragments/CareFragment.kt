package com.smartringpro.mannaheal.ui.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.ui.activities.HomeActivity
import com.smartringpro.mannaheal.adapter.LinkedAccountAdapter
import com.smartringpro.mannaheal.api.linkedAccountData.LinkedAccountInfo
import com.smartringpro.mannaheal.api.linkedAccountData.LinkedAccountRepository
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CareFragment : Fragment() {

    private lateinit var noLinkedLayout: LinearLayout
    private lateinit var linkedLayout: LinearLayout
    private lateinit var linkAccountButton: Button
    private lateinit var btnAddAssociation: Button
    private lateinit var recyclerView: RecyclerView

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_care, container, false)

        noLinkedLayout = view.findViewById(R.id.noLinkedLayout)
        linkedLayout = view.findViewById(R.id.linkedLayout)
        linkAccountButton = view.findViewById(R.id.linkAccountButton)
        btnAddAssociation = view.findViewById(R.id.btnAddAssociation)
        recyclerView = view.findViewById(R.id.linkedAccountsRecyclerView)

        linkAccountButton.setOnClickListener {
            showLinkAccountDialog()
        }
        btnAddAssociation.setOnClickListener {
            showLinkAccountDialog()
        }

        return view
    }

    override fun onResume() {
        super.onResume()

        val sharedPreferences =
            requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("id", -1)

        if (userId != -1) {
            fetchLinkedAccountsFromApi(userId)
        } else {
            showEmptyState()
        }
    }

    private fun showLinkAccountDialog() {
        val fragment = LinkAccountFragment()
        (activity as HomeActivity).openFragment(fragment, "Add a new account", true)
    }

    private fun fetchLinkedAccountsFromApi(userId: Int) {
        val repository = LinkedAccountRepository()

        repository.getLinkedAccountData(userId)
            .enqueue(object : Callback<List<LinkedAccountInfo>> {
                override fun onResponse(
                    call: Call<List<LinkedAccountInfo>>,
                    response: Response<List<LinkedAccountInfo>>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val apiList = response.body()!!
                        if (apiList.isNotEmpty()) {
                            val linkedAccounts = apiList.map {
                                LinkedAccount(
                                    id = it.id,
                                    name = it.name,
                                    mobile = it.phone_number,
                                    relation = it.relation ?: ""
                                )
                            }

                            setupRecyclerView(linkedAccounts)

                            noLinkedLayout.visibility = View.GONE
                            linkedLayout.visibility = View.VISIBLE
                        } else {
                            showEmptyState()
                        }
                    } else {
                        showEmptyState()
                    }
                }

                override fun onFailure(call: Call<List<LinkedAccountInfo>>, t: Throwable) {
                    showEmptyState()
                }
            })
    }

    private fun setupRecyclerView(linkedAccounts: List<LinkedAccount>) {
        noLinkedLayout.visibility = View.GONE
        linkedLayout.visibility = View.VISIBLE

        val adapter = LinkedAccountAdapter(linkedAccounts) { selectedAccount ->
            val fragment = LinkedAccountDetailsFragment().apply {
                arguments = Bundle().apply {
                    putInt("linked_id", selectedAccount.id)
                    putString("linked_name", selectedAccount.name)
                }
            }
            (activity as HomeActivity).openFragment(fragment, "Linked Account", true)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun showEmptyState() {
        noLinkedLayout.visibility = View.VISIBLE
        linkedLayout.visibility = View.GONE
    }
}

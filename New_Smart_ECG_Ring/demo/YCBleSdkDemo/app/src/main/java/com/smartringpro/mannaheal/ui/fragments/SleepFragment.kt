package com.smartringpro.mannaheal.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.smartringpro.mannaheal.R
import com.smartringpro.mannaheal.databinding.FragmentSleepBinding

class SleepFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sleep, container, false)
    }
}


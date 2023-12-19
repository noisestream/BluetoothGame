package com.ballofknives.bluetoothmeatball

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.ballofknives.bluetoothmeatball.databinding.FragmentDriverConnectedBinding


class DriverConnectedFragment : Fragment() {
    private var _binding : FragmentDriverConnectedBinding? = null
    private val binding get() = _binding!!
    private lateinit var meatball: String
    private val sharedViewModel: BluetoothSharedViewModel by activityViewModels()


    companion object {
        const val MEATBALL = "meatball"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            meatball = it.getString(MEATBALL).toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDriverConnectedBinding.inflate(inflater, container, false)
        return binding.root
    }


}
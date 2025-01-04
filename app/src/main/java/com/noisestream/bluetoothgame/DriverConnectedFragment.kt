package com.noisestream.bluetoothgame

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.noisestream.bluetoothgame.databinding.FragmentDriverConnectedBinding


class DriverConnectedFragment : Fragment() {
    private var _binding : FragmentDriverConnectedBinding? = null
    private val binding get() = _binding!!
    private lateinit var game: String
    private val sharedViewModel: BluetoothSharedViewModel by activityViewModels()


    companion object {
        const val MEATBALL = "game"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            game = it.getString(MEATBALL).toString()
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
package com.binbo.glvideo.sample_app.ui.advanced

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.binbo.glvideo.sample_app.databinding.FragmentAdvancedBinding

class AdvancedFragment : Fragment() {

    private var _binding: FragmentAdvancedBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val advancedViewModel =
            ViewModelProvider(this).get(AdvancedViewModel::class.java)

        _binding = FragmentAdvancedBinding.inflate(inflater, container, false)
        val root: View = binding.root

//        val textView: TextView = binding.textAdvanced
//        advancedViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.binbo.glvideo.sample_app.ui.advanced.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.binbo.glvideo.core.ext.singleClick
import com.binbo.glvideo.sample_app.databinding.FragmentAdvancedBinding
import com.binbo.glvideo.sample_app.ui.advanced.activity.NameCardActivity
import com.binbo.glvideo.sample_app.ui.advanced.activity.NameCardWithVideoActivity
import com.binbo.glvideo.sample_app.ui.advanced.activity.VideoCutSelectActivity
import com.binbo.glvideo.sample_app.ui.advanced.viewmodel.AdvancedViewModel

class AdvancedFragment : Fragment() {

    private var _binding: FragmentAdvancedBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val advancedViewModel = ViewModelProvider(this).get(AdvancedViewModel::class.java)
        _binding = FragmentAdvancedBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnNamecard.singleClick {
            startActivity(Intent(requireContext(), NameCardActivity::class.java))
        }

        binding.btnNamecardWithVideoBkg.singleClick {
            startActivity(Intent(requireContext(), NameCardWithVideoActivity::class.java))
        }

        binding.btnVideoCutWidget.singleClick {
            startActivity(Intent(requireContext(), VideoCutSelectActivity::class.java))
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.binbo.glvideo.sample_app.ui.advanced.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.binbo.glvideo.core.ext.singleClick
import com.binbo.glvideo.core.graph.GraphJob
import com.binbo.glvideo.sample_app.databinding.FragmentNameCardBinding
import com.binbo.glvideo.sample_app.impl.advanced.namecard.graph.NameCardGraphManager

class NameCardFragment : Fragment() {

    private var _binding: FragmentNameCardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val graphJob = GraphJob(object : GraphJob.GraphManagerProvider {
        override fun onGraphManagerRequested() = NameCardGraphManager()
    })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentNameCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        graphJob.run()

        binding.imageBack.singleClick {
            activity?.finish()
        }
    }

    override fun onStart() {
        super.onStart()
        binding.viewMissionCard.onResume()
    }

    override fun onStop() {
        super.onStop()
        binding.viewMissionCard.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        graphJob.cancel()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = NameCardFragment()
    }
}
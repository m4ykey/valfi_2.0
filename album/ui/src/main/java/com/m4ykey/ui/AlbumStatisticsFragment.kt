package com.m4ykey.ui

import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.m4ykey.core.Constants.ALBUM
import com.m4ykey.core.Constants.COMPILATION
import com.m4ykey.core.Constants.EP
import com.m4ykey.core.Constants.SINGLE
import com.m4ykey.core.views.BottomNavigationVisibility
import com.m4ykey.ui.databinding.FragmentAlbumStatisticsBinding
import com.patrykandpatrick.vico.core.entry.entryModelOf
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AlbumStatisticsFragment : Fragment() {

    private var _binding : FragmentAlbumStatisticsBinding? = null
    private val binding get() = _binding
    private var bottomNavigationVisibility : BottomNavigationVisibility? = null
    private val viewModel : AlbumViewModel by viewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is BottomNavigationVisibility) {
            bottomNavigationVisibility = context
        } else {
            throw RuntimeException("$context ${getString(R.string.must_implement_bottom_navigation)}")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlbumStatisticsBinding.inflate(inflater, container, false)
        return binding?.root ?: throw IllegalStateException("Binding root is null")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomNavigationVisibility?.hideBottomNavigation()

        binding?.apply {
            toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

            lifecycleScope.launch {
                val album = viewModel.getAlbumTypeCount(ALBUM).firstOrNull() ?: 0
                val single = viewModel.getAlbumTypeCount(SINGLE).firstOrNull() ?: 0
                val compilation = viewModel.getAlbumTypeCount(COMPILATION).firstOrNull() ?: 0
                val ep = viewModel.getAlbumTypeCount(EP).firstOrNull() ?: 0
                val chartEntryModel = entryModelOf(album, single, compilation, ep)
                chart.setModel(chartEntryModel)

                val albumCount = viewModel.getAlbumCount().firstOrNull() ?: 0
                val tracksCount = viewModel.getTotalTracksCount().firstOrNull() ?: 0
                startAnimation(albumCount, tracksCount)
            }
        }
    }

    private fun startAnimation(albumCount : Int, tracksCount : Int) {
        val animator = ValueAnimator.ofInt(albumCount, tracksCount).apply {
            duration = 2000
            addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Int
                binding?.txtAlbumCount?.text = if (animatedValue <= albumCount) animatedValue.toString() else albumCount.toString()
                binding?.txtTotalSongsPlayed?.text = if (animatedValue <= tracksCount) animatedValue.toString() else tracksCount.toString()
            }
        }
        animator.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
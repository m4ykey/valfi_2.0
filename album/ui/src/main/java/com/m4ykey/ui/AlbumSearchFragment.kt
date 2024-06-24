package com.m4ykey.ui

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.m4ykey.core.Constants.SPACE_BETWEEN_ITEMS
import com.m4ykey.core.network.ErrorState
import com.m4ykey.core.views.BaseFragment
import com.m4ykey.core.views.recyclerview.CenterSpaceItemDecoration
import com.m4ykey.core.views.recyclerview.convertDpToPx
import com.m4ykey.core.views.recyclerview.scrollListener
import com.m4ykey.core.views.recyclerview.setupGridLayoutManager
import com.m4ykey.core.views.utils.showToast
import com.m4ykey.ui.adapter.SearchAlbumAdapter
import com.m4ykey.ui.databinding.FragmentAlbumSearchBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class AlbumSearchFragment : BaseFragment<FragmentAlbumSearchBinding>(
    FragmentAlbumSearchBinding::inflate
) {

    private var isClearButtonVisible = false
    private val viewModel by viewModels<AlbumViewModel>()
    private val searchAdapter by lazy { createSearchAdapter() }

    private val speechRecognizerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val spokenText = matches[0]
                binding.etSearch.setText(spokenText)
            }
        }
    }

    private var originalWidth : Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomNavigationVisibility?.hideBottomNavigation()

        setupToolbar()
        setupRecyclerView()
        searchAlbums()
        handleRecyclerViewButton()

        lifecycleScope.launch {
            viewModel.albums.collect { searchAdapter.submitList(it) }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { binding.progressbar.isVisible = it }
        }

        lifecycleScope.launch {
            viewModel.isError.collect { errorState ->
                when (errorState) {
                    is ErrorState.Error -> showToast(requireContext(), errorState.message.toString())
                    else -> {}
                }
            }
        }

        binding.etSearch.post {
            originalWidth = binding.etSearch.width
        }
    }

    private fun createSearchAdapter() : SearchAlbumAdapter {
        return SearchAlbumAdapter(
            onAlbumClick = { album ->
                val action = AlbumSearchFragmentDirections.actionAlbumSearchFragmentToAlbumDetailFragment(album.id)
                findNavController().navigate(action)
            }
        )
    }

    private fun handleRecyclerViewButton() {
        binding.rvSearchAlbums.addOnScrollListener(scrollListener(binding.btnToTop))

        binding.btnToTop.setOnClickListener {
            binding.rvSearchAlbums.smoothScrollToPosition(0)
        }
    }

    private fun resetSearchWidth() {
        val params = binding.etSearch.layoutParams
        params.width = originalWidth
        binding.etSearch.layoutParams = params
    }

    private fun searchAlbums() {
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    val searchQuery = binding.etSearch.text?.toString()
                    if (searchQuery?.isNotEmpty() == true) {
                        viewModel.resetSearch()
                        lifecycleScope.launch { viewModel.searchAlbums(searchQuery) }
                    } else {
                        showToast(requireContext(), getString(R.string.empty_search))
                    }
                }
            }
            actionId == EditorInfo.IME_ACTION_SEARCH
        }
    }

    private fun setupRecyclerView() {
        binding.rvSearchAlbums.apply {
            addItemDecoration(CenterSpaceItemDecoration(convertDpToPx(SPACE_BETWEEN_ITEMS)))
            adapter = searchAdapter
            layoutManager = setupGridLayoutManager(requireContext(), 110f)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (!recyclerView.canScrollVertically(1)) {
                        val searchQuery = binding.etSearch.text.toString()
                        if (!viewModel.isPaginationEnded && !viewModel.isLoading.value && searchQuery.isNotEmpty()) {
                            lifecycleScope.launch { viewModel.searchAlbums(searchQuery) }
                        }
                    }
                }
            })
        }
    }

    private fun setupToolbar() {
        binding.apply {
            toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
            etSearch.doOnTextChanged { text, _, _, _ ->
                val isSearchEmpty = text.isNullOrBlank()
                handleClearButtonVisibility(isSearchEmpty)
            }
            imgMicrophone.setOnClickListener { recordAudio() }

            imgClear.setOnClickListener {
                etSearch.setText(getString(R.string.empty_string))
                hideClearButtonWithAnimation()
            }
        }
    }

    private fun recordAudio() {
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            showToast(requireContext(), getString(R.string.speech_not_recognition))
        } else {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.enter_album_name))
            }
            speechRecognizerLauncher.launch(intent)
        }
    }

    private fun handleClearButtonVisibility(isSearchEmpty: Boolean) {
        if (!isSearchEmpty && !isClearButtonVisible) {
            showClearButtonWithAnimation()
            isClearButtonVisible = true
        } else if (isSearchEmpty && isClearButtonVisible) {
            hideClearButtonWithAnimation()
            isClearButtonVisible = false
        }
    }

    private fun View.animationProperties(
        translationXValue: Float,
        alphaValue: Float,
        interpolator: Interpolator
    ) {
        animate()
            .translationX(translationXValue)
            .alpha(alphaValue)
            .setInterpolator(interpolator)
            .start()
    }

    private fun showClearButtonWithAnimation() {
        binding.imgClear.apply {
            translationX = 100f
            alpha = 0f
            isVisible = true

            animationProperties(0f, 1f, DecelerateInterpolator())
        }
    }

    private fun hideClearButtonWithAnimation() {
        binding.imgClear.apply {
            animationProperties(width.toFloat(), 0f, AccelerateInterpolator())
            visibility = View.GONE
        }
        resetSearchWidth()
    }

    private fun resetSearchState() {
        binding.apply {
            if (etSearch.text.isNullOrBlank()) {
                imgClear.isVisible = false
                isClearButtonVisible = false
            } else {
                imgClear.isVisible = true
                isClearButtonVisible = true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resetSearchState()
    }
}
package com.m4ykey.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import com.m4ykey.core.Constants.SPACE_BETWEEN_ITEMS
import com.m4ykey.core.views.BottomNavigationVisibility
import com.m4ykey.core.views.recyclerview.CenterSpaceItemDecoration
import com.m4ykey.core.views.recyclerview.convertDpToPx
import com.m4ykey.core.views.show
import com.m4ykey.core.views.utils.showToast
import com.m4ykey.data.domain.model.album.AlbumItem
import com.m4ykey.core.views.recyclerview.adapter.LoadStateAdapter
import com.m4ykey.ui.adapter.SearchAlbumPagingAdapter
import com.m4ykey.ui.databinding.FragmentAlbumSearchBinding
import com.m4ykey.ui.uistate.AlbumListUiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AlbumSearchFragment : Fragment() {

    private var _binding : FragmentAlbumSearchBinding? = null
    private val binding get() = _binding
    private var bottomNavigationVisibility : BottomNavigationVisibility? = null
    private var isClearButtonVisible = false
    private val viewModel : AlbumViewModel by viewModels()
    private lateinit var searchAdapter : SearchAlbumPagingAdapter
    private val debouncingSearch = DebouncingSearch()

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
        _binding = FragmentAlbumSearchBinding.inflate(inflater, container, false)
        return binding?.root ?: throw IllegalStateException("Binding root is null")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomNavigationVisibility?.hideBottomNavigation()

        setupToolbar()
        setupRecyclerView()
        searchAlbums()

        lifecycleScope.launch {
            viewModel.albums.observe(viewLifecycleOwner) { state -> handleSearchState(state) }
        }
    }

    private fun handleSearchState(state : AlbumListUiState?) {
        state ?: return
        binding?.apply {
            progressbar.isVisible = state.isLoading
            rvSearchAlbums.isVisible = !state.isLoading
            state.error?.let { showToast(requireContext(), it) }
            state.albumList?.let { search ->
                searchAdapter.submitData(lifecycle, search)
            }
        }
    }

    private fun searchAlbums() {
        binding?.etSearch?.apply {
            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_SEARCH -> {
                        val searchQuery = text.toString().trim()
                        if (searchQuery.isNotEmpty()) {
                            debouncingSearch.submit(searchQuery)
                            binding?.rvSearchAlbums?.isEnabled = false
                        } else {
                            showToast(requireContext(), getString(R.string.empty_search))
                        }
                    }
                }
                actionId == EditorInfo.IME_ACTION_SEARCH
            }
        }
    }

    private fun setupRecyclerView() {
        binding?.rvSearchAlbums?.apply {
            addItemDecoration(CenterSpaceItemDecoration(convertDpToPx(SPACE_BETWEEN_ITEMS)))

            val onAlbumClick : (AlbumItem) -> Unit = { album ->
                val action = AlbumSearchFragmentDirections.actionAlbumSearchFragmentToAlbumDetailFragment(album.id)
                findNavController().navigate(action)
            }

            searchAdapter = SearchAlbumPagingAdapter(onAlbumClick)

            val headerAdapter = LoadStateAdapter { searchAdapter.retry() }
            val footerAdapter = LoadStateAdapter { searchAdapter.retry() }

            adapter = searchAdapter.withLoadStateHeaderAndFooter(
                header = headerAdapter,
                footer = footerAdapter
            )

            val layoutManager = GridLayoutManager(requireContext(), 3)
            layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when {
                        position == 0 && headerAdapter.itemCount > 0 -> 3
                        position == adapter?.itemCount?.minus(1) && footerAdapter.itemCount > 0 -> 3
                        else -> 1
                    }
                }
            }

            this.layoutManager = layoutManager

            searchAdapter.addLoadStateListener { loadState ->
                handleLoadState(loadState)
            }
        }
    }

    private fun handleLoadState(loadState : CombinedLoadStates) {
        binding?.apply {
            progressbar.isVisible = loadState.source.refresh is LoadState.Loading

            val isNothingFound = loadState.source.refresh is LoadState.NotLoading &&
                    loadState.append.endOfPaginationReached &&
                    searchAdapter.itemCount < 1

            rvSearchAlbums.isVisible = !isNothingFound
            layoutNothingFound.root.isVisible = isNothingFound
        }
    }

    private fun setupToolbar() {
        binding?.apply {
            toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
            etSearch.doOnTextChanged { text, _, _, _ ->
                val isSearchEmpty = text.isNullOrBlank()
                handleClearButtonVisibility(isSearchEmpty)
            }

            imgClear.setOnClickListener {
                etSearch.setText("")
                hideClearButtonWithAnimation()
            }
        }
    }

    private fun handleClearButtonVisibility(isSearchEmpty : Boolean) {
        if (!isSearchEmpty && !isClearButtonVisible) {
            showClearButtonWithAnimation()
            isClearButtonVisible = true
        } else if (isSearchEmpty && isClearButtonVisible) {
            hideClearButtonWithAnimation()
            isClearButtonVisible = false
        }
    }

    private fun View.animationProperties(translationXValue : Float, alphaValue : Float, interpolator : Interpolator) {
        animate()
            .translationX(translationXValue)
            .alpha(alphaValue)
            .setInterpolator(interpolator)
            .start()
    }

    private fun showClearButtonWithAnimation() {
        binding?.imgClear?.apply {
            translationX = 100f
            alpha = 0f
            show()

            animationProperties(0f, 1f, DecelerateInterpolator())
        }
    }

    private fun hideClearButtonWithAnimation() {
        binding?.imgClear?.apply {
            animationProperties(width.toFloat(), 0f, AccelerateInterpolator())
        }
    }

    private fun resetSearchState() {
        binding?.apply {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class DebouncingSearch {
        private var searchJob : Job? = null

        fun submit(searchQuery : String) {
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                delay(500L)
                viewModel.searchAlbums(searchQuery)
                binding?.rvSearchAlbums?.isEnabled = true
            }
        }
    }

}
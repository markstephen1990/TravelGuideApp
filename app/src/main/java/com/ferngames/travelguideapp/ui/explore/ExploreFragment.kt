package com.ferngames.travelguideapp.ui.explore

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ferngames.travelguideapp.DestinationAdapter
import com.ferngames.travelguideapp.R
import com.ferngames.travelguideapp.data.local.TravelDatabase
import com.ferngames.travelguideapp.data.model.Destination
import com.ferngames.travelguideapp.data.remote.DestinationEnricher
import com.ferngames.travelguideapp.data.remote.PlacesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ExploreFragment : Fragment() {

    private lateinit var adapter: DestinationAdapter
    private lateinit var database: TravelDatabase
    private lateinit var placesRepository: PlacesRepository
    private var searchJob: Job? = null
    private var lastQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_explore, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = TravelDatabase.getDatabase(requireContext())
        placesRepository = PlacesRepository()

        val rvExplore = view.findViewById<RecyclerView>(R.id.rvExplore)
        val tvResultsCount = view.findViewById<TextView>(R.id.tvResultsCount)
        val etSearch = view.findViewById<EditText>(R.id.etSearch)

        adapter = DestinationAdapter(
            emptyList(),
            onItemClick = { destination ->
                val action = ExploreFragmentDirections
                    .actionExploreToDetail(destination.id)
                findNavController().navigate(action)
            },
            onWishlistClick = { destination ->
                toggleWishlist(destination)
            }
        )
        rvExplore.layoutManager = LinearLayoutManager(requireContext())
        rvExplore.adapter = adapter

        // Restore last search query or load all
        if (lastQuery.isNotEmpty()) {
            etSearch.setText(lastQuery)
            etSearch.setSelection(lastQuery.length)
            viewLifecycleOwner.lifecycleScope.launch {
                searchDestinations(lastQuery, tvResultsCount)
            }
        } else {
            database.destinationDao().getAllDestinations()
                .observe(viewLifecycleOwner) { destinations ->
                    adapter.updateList(destinations)
                    tvResultsCount.text = "${destinations.size} destinations found"
                }
        }

        // Search with debounce
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                lastQuery = query
                searchJob?.cancel()
                if (query.isEmpty()) {
                    database.destinationDao().getAllDestinations()
                        .observe(viewLifecycleOwner) { destinations ->
                            adapter.updateList(destinations)
                            tvResultsCount.text = "${destinations.size} destinations found"
                        }
                } else {
                    searchJob = viewLifecycleOwner.lifecycleScope.launch {
                        delay(500)
                        adapter.updateList(emptyList())
                        tvResultsCount.text = "Searching..."
                        searchDestinations(query, tvResultsCount)
                    }
                }
            }
        })

        // Category filters
        setupFilters(view, tvResultsCount)
    }

    private suspend fun searchDestinations(
        query: String,
        tvResultsCount: TextView
    ) {
        try {
            // First check local database
            var localFound = false
            database.destinationDao().searchDestinations(query)
                .observe(viewLifecycleOwner) { localResults ->
                    if (localResults.isNotEmpty()) {
                        localFound = true
                        adapter.updateList(localResults)
                        tvResultsCount.text = "${localResults.size} destinations found"
                    }
                }

            val predictions = placesRepository.searchPlaces(query)

            predictions.forEach { prediction ->
                android.util.Log.d("SEARCH", "Found: ${prediction.name} | ${prediction.address} | ${prediction.type}")
            }

            val filteredPredictions = predictions.filter { prediction ->
                prediction.name.startsWith(query, ignoreCase = true)
            }

            android.util.Log.d("SEARCH", "Filtered: ${filteredPredictions.size} results")

            if (filteredPredictions.isNotEmpty()) {
                val enricher = DestinationEnricher()

                filteredPredictions.take(3).forEach { prediction ->
                    tvResultsCount.text = "Loading ${prediction.name}..."

                    val country = prediction.address
                        .split(",").lastOrNull()?.trim() ?: ""

                    val photo = enricher.getUnsplashPhoto(prediction.name)
                    val countryInfo = enricher.getCountryInfo(country)
                    val aiDescription = enricher.getAIDescription(
                        prediction.name, country
                    )

                    val imageUrl = if (photo.isNotEmpty()) photo
                    else prediction.imageUrl

                    val description = if (aiDescription.isNotEmpty())
                        aiDescription
                    else
                        "Discover ${prediction.name} — a wonderful destination!"

                    val destination = Destination(
                        name = prediction.name,
                        country = country,
                        description = description,
                        imageUrl = imageUrl,
                        category = when (prediction.type) {
                            "beach", "coastline" -> "Beach"
                            "peak", "mountain", "national_park" -> "Adventure"
                            else -> "City"
                        },
                        rating = 4.0,
                        latitude = prediction.latitude,
                        longitude = prediction.longitude,
                        bestTimeToVisit = "Check local guides",
                        currency = countryInfo?.currency ?: "Local currency",
                        language = countryInfo?.language ?: "Local language"
                    )

                    database.destinationDao().insertDestination(destination)
                }

                database.destinationDao().searchDestinations(query)
                    .observe(viewLifecycleOwner) { results ->
                        adapter.updateList(results)
                        tvResultsCount.text = "${results.size} destinations found"
                    }
            } else if (!localFound) {
                tvResultsCount.text = "No results found for \"$query\""
                adapter.updateList(emptyList())
            }
        } catch (e: Exception) {
            android.util.Log.e("SEARCH", "Error: ${e.message}")
            tvResultsCount.text = "Search error — check your connection"
        }
    }

    private fun setupFilters(view: View, tvResultsCount: TextView) {
        val chipAll = view.findViewById<TextView>(R.id.chipAll)
        val chipBeach = view.findViewById<TextView>(R.id.chipBeach)
        val chipCity = view.findViewById<TextView>(R.id.chipCity)
        val chipAdventure = view.findViewById<TextView>(R.id.chipAdventure)

        chipAll.setOnClickListener {
            lastQuery = ""
            database.destinationDao().getAllDestinations()
                .observe(viewLifecycleOwner) {
                    adapter.updateList(it)
                    tvResultsCount.text = "${it.size} destinations found"
                }
            setActiveChip(view, chipAll)
        }

        chipBeach.setOnClickListener {
            lastQuery = ""
            database.destinationDao().getDestinationsByCategory("Beach")
                .observe(viewLifecycleOwner) {
                    adapter.updateList(it)
                    tvResultsCount.text = "${it.size} destinations found"
                }
            setActiveChip(view, chipBeach)
        }

        chipCity.setOnClickListener {
            lastQuery = ""
            database.destinationDao().getDestinationsByCategory("City")
                .observe(viewLifecycleOwner) {
                    adapter.updateList(it)
                    tvResultsCount.text = "${it.size} destinations found"
                }
            setActiveChip(view, chipCity)
        }

        chipAdventure.setOnClickListener {
            lastQuery = ""
            database.destinationDao().getDestinationsByCategory("Adventure")
                .observe(viewLifecycleOwner) {
                    adapter.updateList(it)
                    tvResultsCount.text = "${it.size} destinations found"
                }
            setActiveChip(view, chipAdventure)
        }
    }

    private fun setActiveChip(view: View, activeChip: TextView) {
        val chips = listOf(
            view.findViewById<TextView>(R.id.chipAll),
            view.findViewById<TextView>(R.id.chipBeach),
            view.findViewById<TextView>(R.id.chipCity),
            view.findViewById<TextView>(R.id.chipAdventure)
        )
        chips.forEach { chip ->
            chip.setBackgroundColor(
                if (chip == activeChip)
                    android.graphics.Color.parseColor("#6C63FF")
                else
                    android.graphics.Color.parseColor("#16213E")
            )
        }
    }

    private fun toggleWishlist(destination: Destination) {
        val newStatus = !destination.isWishlisted
        viewLifecycleOwner.lifecycleScope.launch {
            database.destinationDao()
                .updateWishlistStatus(destination.id, newStatus)
        }
    }
}
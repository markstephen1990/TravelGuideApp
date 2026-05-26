package com.ferngames.travelguideapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.ferngames.travelguideapp.data.remote.RecommendationService
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var adapter: DestinationAdapter
    private lateinit var database: TravelDatabase
    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = TravelDatabase.getDatabase(requireContext())

        // Setup RecyclerView
        val rvDestinations = view.findViewById<RecyclerView>(R.id.rvDestinations)
        adapter = DestinationAdapter(
            emptyList(),
            onItemClick = { destination ->
                val action = HomeFragmentDirections
                    .actionHomeToDetail(destination.id)
                findNavController().navigate(action)
            },
            onWishlistClick = { destination ->
                toggleWishlist(destination)
            }
        )
        rvDestinations.layoutManager = LinearLayoutManager(requireContext())
        rvDestinations.adapter = adapter

        // Observe local destinations
        database.destinationDao().getAllDestinations()
            .observe(viewLifecycleOwner) { destinations ->
                when {
                    isLoading -> {
                        // Don't update while loading
                    }
                    destinations.isEmpty() -> {
                        // No destinations — fetch from AI
                        loadAIRecommendations(view)
                    }
                    else -> {
                        // Show destinations
                        adapter.updateList(destinations)
                    }
                }
            }

        // Category filters
        setupCategoryFilters(view)

        // Search hint click
        view.findViewById<TextView>(R.id.tvSearchHint).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_explore)
        }

        // Refresh button
        view.findViewById<TextView>(R.id.tvRefresh).setOnClickListener {
            loadAIRecommendations(view)
        }
    }

    private fun loadAIRecommendations(view: View) {
        if (isLoading) return
        isLoading = true

        val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
        tvStatus.visibility = View.VISIBLE
        tvStatus.text = "🤖 Getting AI recommendations..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Clear adapter immediately
                adapter.updateList(emptyList())

                // Clear only non-wishlisted destinations
                database.destinationDao().deleteNonWishlistedDestinations()

                val recommendationService = RecommendationService()
                val enricher = DestinationEnricher()
                val placesRepo = PlacesRepository()

                // Get AI recommended destinations
                val recommendations = recommendationService.getRecommendedDestinations()
                tvStatus.text = "📸 Loading destination photos..."

                recommendations.forEach { (city, country) ->
                    tvStatus.text = "Loading $city..."

                    try {
                        val photo = enricher.getUnsplashPhoto("$city travel landmark")
                        val description = enricher.getAIDescription(city, country)
                        val countryInfo = enricher.getCountryInfo(country)
                        val predictions = placesRepo.searchPlaces(city)
                        val coords = predictions.firstOrNull()

                        val destination = Destination(
                            name = city,
                            country = country,
                            description = description.ifEmpty {
                                "Discover $city — a wonderful destination waiting to be explored!"
                            },
                            imageUrl = photo.ifEmpty {
                                "https://picsum.photos/seed/${city.hashCode()}/800/600"
                            },
                            category = "City",
                            rating = 4.5,
                            latitude = coords?.latitude ?: 0.0,
                            longitude = coords?.longitude ?: 0.0,
                            bestTimeToVisit = "Check local guides",
                            currency = countryInfo?.currency ?: "Local currency",
                            language = countryInfo?.language ?: "Local language"
                        )

                        database.destinationDao().insertDestination(destination)
                    } catch (e: Exception) {
                        android.util.Log.e("HOME", "Error loading $city: ${e.message}")
                    }
                }

                tvStatus.text = "✅ Destinations loaded!"
                tvStatus.postDelayed({
                    if (isAdded) tvStatus.visibility = View.GONE
                }, 2000)

            } catch (e: Exception) {
                tvStatus.text = "❌ Error loading recommendations"
                android.util.Log.e("HOME", "Error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    private fun setupCategoryFilters(view: View) {
        val btnAll = view.findViewById<TextView>(R.id.btnAll)
        val btnBeach = view.findViewById<TextView>(R.id.btnBeach)
        val btnCity = view.findViewById<TextView>(R.id.btnCity)
        val btnAdventure = view.findViewById<TextView>(R.id.btnAdventure)

        btnAll.setOnClickListener {
            database.destinationDao().getAllDestinations()
                .observe(viewLifecycleOwner) { adapter.updateList(it) }
            setActiveFilter(view, btnAll)
        }

        btnBeach.setOnClickListener {
            database.destinationDao().getDestinationsByCategory("Beach")
                .observe(viewLifecycleOwner) { adapter.updateList(it) }
            setActiveFilter(view, btnBeach)
        }

        btnCity.setOnClickListener {
            database.destinationDao().getDestinationsByCategory("City")
                .observe(viewLifecycleOwner) { adapter.updateList(it) }
            setActiveFilter(view, btnCity)
        }

        btnAdventure.setOnClickListener {
            database.destinationDao().getDestinationsByCategory("Adventure")
                .observe(viewLifecycleOwner) { adapter.updateList(it) }
            setActiveFilter(view, btnAdventure)
        }
    }

    private fun setActiveFilter(view: View, activeBtn: TextView) {
        val buttons = listOf(
            view.findViewById<TextView>(R.id.btnAll),
            view.findViewById<TextView>(R.id.btnBeach),
            view.findViewById<TextView>(R.id.btnCity),
            view.findViewById<TextView>(R.id.btnAdventure)
        )
        buttons.forEach { btn ->
            btn.setBackgroundColor(
                if (btn == activeBtn)
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
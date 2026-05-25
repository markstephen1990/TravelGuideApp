package com.ferngames.travelguideapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ferngames.travelguideapp.DestinationAdapter
import com.ferngames.travelguideapp.R
import com.ferngames.travelguideapp.data.local.TravelDatabase
import com.ferngames.travelguideapp.data.model.Destination
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var adapter: DestinationAdapter
    private lateinit var database: TravelDatabase

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

        // Observe destinations
        database.destinationDao().getAllDestinations().observe(viewLifecycleOwner) { destinations ->
            adapter.updateList(destinations)
        }

        // Category filters
        setupCategoryFilters(view)

        // Search hint click
        view.findViewById<TextView>(R.id.tvSearchHint).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_explore)
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
                    android.graphics.Color.parseColor("#2E2E3E")
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
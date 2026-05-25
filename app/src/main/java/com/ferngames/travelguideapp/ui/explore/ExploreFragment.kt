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
import kotlinx.coroutines.launch

class ExploreFragment : Fragment() {

    private lateinit var adapter: DestinationAdapter
    private lateinit var database: TravelDatabase

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

        // Setup RecyclerView
        val rvExplore = view.findViewById<RecyclerView>(R.id.rvExplore)
        val tvResultsCount = view.findViewById<TextView>(R.id.tvResultsCount)

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

        // Observe all destinations
        database.destinationDao().getAllDestinations().observe(viewLifecycleOwner) { destinations ->
            adapter.updateList(destinations)
            tvResultsCount.text = "${destinations.size} destinations found"
        }

        // Search
        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.isEmpty()) {
                    database.destinationDao().getAllDestinations()
                        .observe(viewLifecycleOwner) { destinations ->
                            adapter.updateList(destinations)
                            tvResultsCount.text = "${destinations.size} destinations found"
                        }
                } else {
                    database.destinationDao().searchDestinations(query)
                        .observe(viewLifecycleOwner) { destinations ->
                            adapter.updateList(destinations)
                            tvResultsCount.text = "${destinations.size} destinations found"
                        }
                }
            }
        })

        // Category filters
        setupFilters(view, tvResultsCount)
    }

    private fun setupFilters(view: View, tvResultsCount: TextView) {
        val chipAll = view.findViewById<TextView>(R.id.chipAll)
        val chipBeach = view.findViewById<TextView>(R.id.chipBeach)
        val chipCity = view.findViewById<TextView>(R.id.chipCity)
        val chipAdventure = view.findViewById<TextView>(R.id.chipAdventure)

        chipAll.setOnClickListener {
            database.destinationDao().getAllDestinations()
                .observe(viewLifecycleOwner) {
                    adapter.updateList(it)
                    tvResultsCount.text = "${it.size} destinations found"
                }
            setActiveChip(view, chipAll)
        }

        chipBeach.setOnClickListener {
            database.destinationDao().getDestinationsByCategory("Beach")
                .observe(viewLifecycleOwner) {
                    adapter.updateList(it)
                    tvResultsCount.text = "${it.size} destinations found"
                }
            setActiveChip(view, chipBeach)
        }

        chipCity.setOnClickListener {
            database.destinationDao().getDestinationsByCategory("City")
                .observe(viewLifecycleOwner) {
                    adapter.updateList(it)
                    tvResultsCount.text = "${it.size} destinations found"
                }
            setActiveChip(view, chipCity)
        }

        chipAdventure.setOnClickListener {
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
package com.embed.pashudhan.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.embed.pashudhan.R
import kotlinx.android.synthetic.main.fragment_pashu_samwardhan.*

class PashuSamwardhanFragment : Fragment() {

    companion object{
        val TAG = "Samachar==>"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pashu_samwardhan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        samacharButton.setOnClickListener {
            fragmentManager?.beginTransaction()?.
            replace(R.id.nav_frameLayout, PashuSamachar())?.
            setReorderingAllowed(true)?.
            addToBackStack(PashuSamachar.TAG)?.
            commit()
        }

        charchaButton.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.upcoming), Toast.LENGTH_SHORT).show()
        }

        swasthyaButton.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.upcoming), Toast.LENGTH_SHORT).show()
        }
    }
}
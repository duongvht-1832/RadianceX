package com.example.android.radiancex

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.android.radiancex.databinding.FragmentAccountBinding
import com.google.firebase.auth.FirebaseAuth
import java.lang.Exception

class AccountFragment : Fragment() {

    lateinit var fAuth: FirebaseAuth
    lateinit var binding: FragmentAccountBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentAccountBinding.inflate(inflater, container, false)
        val view = binding.root

        fAuth = FirebaseAuth.getInstance()

        binding.apply {
            btnSettings.setOnClickListener {
                Toast.makeText(context, "Coming soon!", Toast.LENGTH_SHORT).show()
            }

            btnLogout.setOnClickListener {
                try {
                    fAuth.signOut()
                    val intent = Intent(context, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to log out", Toast.LENGTH_SHORT).show()
                }
            }
        }

        return view
    }

    companion object {
        fun newInstance() = AccountFragment()
    }
}
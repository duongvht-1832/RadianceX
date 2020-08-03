package com.example.android.radiancex

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.Navigation
import androidx.navigation.ui.setupWithNavController
import com.example.android.radiancex.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true);
        supportActionBar?.setHomeButtonEnabled(true);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.let {
            binding.bottomNavigation.setupWithNavController(Navigation.findNavController(this, R.id.nav_host_fragment))
            it.actionAddEntry.setOnClickListener { v: View? ->
                val intent = Intent(this, AddNewEntryActivity::class.java)
                startActivityForResult(intent, NEW_WORD_ACTIVITY_REQUEST_CODE)
            }
            it.actionNewQuestion.setOnClickListener { v: View? -> Toast.makeText(this, "Coming soon!", Toast.LENGTH_SHORT).show() }
            it.tvScreenName.setText(R.string.learn)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.let { if (it.addContentMenu.isExpanded) it.addContentMenu.collapse() }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == NEW_WORD_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Entry added", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val NEW_WORD_ACTIVITY_REQUEST_CODE = 1
    }
}
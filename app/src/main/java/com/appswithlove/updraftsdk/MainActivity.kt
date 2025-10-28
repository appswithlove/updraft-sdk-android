package com.appswithlove.updraftsdk

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.appswithlove.updraftsdk.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowInsetsControllerCompat(window, binding.root).isAppearanceLightStatusBars = true
        WindowInsetsControllerCompat(window, binding.root).isAppearanceLightNavigationBars = true
    }

    override fun onStart() {
        super.onStart()

        binding.button.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}

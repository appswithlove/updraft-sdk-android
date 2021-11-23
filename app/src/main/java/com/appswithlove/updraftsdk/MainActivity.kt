package com.appswithlove.updraftsdk

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.appswithlove.updraftsdk.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding //error


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()

        binding.button.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}
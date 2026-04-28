package com.appswithlove.updraftsdk

import android.content.DialogInterface
import android.graphics.Typeface
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.appswithlove.updraft.R as UpdraftR
import com.appswithlove.updraftsdk.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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
            val title = getString(UpdraftR.string.updraft_updateAvailable_titleWithVersion, "2.3.0")
            val released = resources.getQuantityString(UpdraftR.plurals.updraft_relative_weeksAgo, 2, 2)
            val message = getString(UpdraftR.string.updraft_updateAvailable_descriptionFull, released, "2.2.5")
            val dialog = MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(UpdraftR.string.updraft_updateAvailable_openButton, null)
                .setNegativeButton(UpdraftR.string.updraft_updateAvailable_laterButton, null)
                .setCancelable(false)
                .show()
            dialog.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)?.apply {
                typeface = Typeface.create(typeface, Typeface.BOLD)
            }
        }
    }
}

package com.tanasi.streamflix.activities.main

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import com.bumptech.glide.Glide
import com.tanasi.navigation.widget.setupWithNavController
import com.tanasi.streamflix.NavMainGraphDirections
import com.tanasi.streamflix.R
import com.tanasi.streamflix.database.AppDatabase
import com.tanasi.streamflix.databinding.ActivityMainTvBinding
import com.tanasi.streamflix.databinding.ContentHeaderMenuMainTvBinding
import com.tanasi.streamflix.fragments.player.PlayerTvFragment
import com.tanasi.streamflix.ui.UpdateAppTvDialog
import com.tanasi.streamflix.utils.UserPreferences
import com.tanasi.streamflix.utils.getCurrentFragment

class MainTvActivity : FragmentActivity() {

    private var _binding: ActivityMainTvBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<MainViewModel>()

    private lateinit var updateAppDialog: UpdateAppTvDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_Tv)
        super.onCreate(savedInstanceState)
        _binding = ActivityMainTvBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) {
            finish()
            startActivity(Intent(this, MainMobileActivity::class.java))
        }

        val navHostFragment = this.supportFragmentManager
            .findFragmentById(binding.navMainFragment.id) as NavHostFragment
        val navController = navHostFragment.navController

        UserPreferences.setup(this)
        AppDatabase.setup(this)
        UserPreferences.currentProvider?.let {
            navController.navigate(R.id.home)
        }

        binding.navMain.setupWithNavController(navController)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.navMainFragment.isFocusedByDefault = true
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.navMain.headerView?.apply {
                val header = ContentHeaderMenuMainTvBinding.bind(this)

                Glide.with(context)
                    .load(UserPreferences.currentProvider?.logo?.takeIf { it.isNotEmpty() }
                        ?: R.drawable.ic_provider_default_logo)
                    .into(header.ivNavigationHeaderIcon)
                header.tvNavigationHeaderTitle.text = UserPreferences.currentProvider?.name
                header.tvNavigationHeaderSubtitle.text = getString(
                    R.string.main_menu_change_provider
                )

                setOnOpenListener {
                    header.tvNavigationHeaderTitle.visibility = View.VISIBLE
                    header.tvNavigationHeaderSubtitle.visibility = View.VISIBLE
                }
                setOnCloseListener {
                    header.tvNavigationHeaderTitle.visibility = View.GONE
                    header.tvNavigationHeaderSubtitle.visibility = View.GONE
                }

                setOnClickListener {
                    navController.navigate(NavMainGraphDirections.actionGlobalProviders())
                }
            }

            when (destination.id) {
                R.id.search,
                R.id.home,
                R.id.movies,
                R.id.tv_shows -> binding.navMain.visibility = View.VISIBLE
                else -> binding.navMain.visibility = View.GONE
            }
        }

        viewModel.state.observe(this) { state ->
            when (state) {
                MainViewModel.State.CheckingUpdate -> {}
                is MainViewModel.State.SuccessCheckingUpdate -> {
                    val asset = state.release?.assets
                        ?.find { it.contentType == "application/vnd.android.package-archive" }
                    if (asset != null) {
                        updateAppDialog = UpdateAppTvDialog(this, state.release).also {
                            it.setOnUpdateClickListener { _ ->
                                if (!it.isLoading) viewModel.downloadUpdate(this, asset)
                            }
                            it.show()
                        }
                    }
                }

                MainViewModel.State.DownloadingUpdate -> updateAppDialog.isLoading = true
                is MainViewModel.State.SuccessDownloadingUpdate -> {
                    viewModel.installUpdate(this, state.apk)
                    updateAppDialog.hide()
                }

                MainViewModel.State.InstallingUpdate -> updateAppDialog.isLoading = true

                is MainViewModel.State.FailedUpdate -> {
                    Toast.makeText(
                        this,
                        state.error.message ?: "",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (navController.currentDestination?.id) {
                    R.id.home -> when {
                        binding.navMain.hasFocus() -> finish()
                        else -> binding.navMain.requestFocus()
                    }
                    R.id.search,
                    R.id.movies,
                    R.id.tv_shows -> when {
                        binding.navMain.hasFocus() -> binding.navMain.findViewById<View>(R.id.home)
                            .let {
                                it.requestFocus()
                                it.performClick()
                            }
                        else -> binding.navMain.requestFocus()
                    }
                    else -> {
                        when (val currentFragment = getCurrentFragment()) {
                            is PlayerTvFragment -> currentFragment.onBackPressed()
                            else -> false
                        }.takeIf { !it }?.let {
                            navController.navigateUp()
                        }
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkUpdate()
    }
}
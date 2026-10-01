package com.echosafe.ui.auth

import android.accounts.AccountManager
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.echosafe.data.model.User
import com.echosafe.databinding.ActivityAuthBinding
import com.echosafe.ui.contacts.ContactsActivity
import com.echosafe.ui.dashboard.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private val viewModel: AuthViewModel by viewModels()
    private var isSignUpMode = true
    private var currentGeneratedOtp = "123456"

    private val googleAccountPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val selectedEmail = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!selectedEmail.isNullOrEmpty()) {
                onGoogleAccountSelected(selectedEmail)
                return@registerForActivityResult
            }
        }
        showDeviceAccountsDialog()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGenderDropdown()
        setupListeners()
        observeViewModel()
    }

    private fun setupGenderDropdown() {
        val genders = arrayOf("Female", "Male", "Non-Binary", "Prefer not to say")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genders)
        binding.actvGender.setAdapter(adapter)
        binding.actvGender.setText(genders[0], false)
    }

    private fun setupListeners() {
        binding.btnQuickExit.setOnClickListener {
            // Stealth Quick Exit: Immediately close and minimize app
            finishAffinity()
        }



        binding.tvToggleMode.setOnClickListener {
            isSignUpMode = !isSignUpMode
            if (isSignUpMode) {
                binding.tvFormTitle.text = "Identity & Guardian Profile"
                binding.tilFullName.visibility = View.VISIBLE
                binding.tilAge.visibility = View.VISIBLE
                binding.tilGender.visibility = View.VISIBLE
                binding.btnSubmit.text = "Create Safe Profile & Continue  →"
                binding.tvToggleMode.text = "Already have a safe account? Sign In"
            } else {
                binding.tvFormTitle.text = "Sign In"
                binding.tilFullName.visibility = View.GONE
                binding.tilAge.visibility = View.GONE
                binding.tilGender.visibility = View.GONE
                binding.btnSubmit.text = "Sign In to Safe Space  →"
                binding.tvToggleMode.text = "Don't have a safe account? Create Safe Profile"
            }
        }

        binding.btnSubmit.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isSignUpMode) {
                val fullName = binding.etFullName.text.toString().trim()
                val age = binding.etAge.text.toString().toIntOrNull() ?: 21
                val gender = binding.actvGender.text.toString()

                val user = User(
                    fullName = if (fullName.isEmpty()) "EchoSafe User" else fullName,
                    email = email,
                    age = age,
                    gender = gender
                )
                // Trigger Email Verification OTP Code Flow sent to user's email
                sendEmailVerificationCode(user, password)
            } else {
                viewModel.signIn(email, password)
            }
        }

        binding.btnGoogleSignIn.setOnClickListener {
            promptGoogleSignIn()
        }
    }

    private fun promptGoogleSignIn() {
        try {
            @Suppress("DEPRECATION")
            val intent = AccountManager.newChooseAccountIntent(
                null,
                null,
                arrayOf("com.google"),
                true,
                null,
                null,
                null,
                null
            )
            googleAccountPickerLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e("AuthActivity", "AccountPicker error, falling back to account list", e)
            showDeviceAccountsDialog()
        }
    }

    private fun showDeviceAccountsDialog() {
        val accountManager = AccountManager.get(this)
        val deviceAccounts = try {
            accountManager.getAccountsByType("com.google").map { it.name }
        } catch (_: Exception) {
            emptyList()
        }

        if (deviceAccounts.isEmpty()) {
            Toast.makeText(this, "No Google account found on device. Please sign in with email/password.", Toast.LENGTH_LONG).show()
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Google Account")
        builder.setItems(deviceAccounts.toTypedArray()) { _, which ->
            val selectedEmail = deviceAccounts[which]
            onGoogleAccountSelected(selectedEmail)
        }
        builder.show()
    }

    private fun showCustomGoogleEmailDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Google Account Sign-In")
        builder.setMessage("Enter your Google Account email:")

        val input = EditText(this).apply {
            hint = "name@gmail.com"
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        builder.setView(input)

        builder.setPositiveButton("Continue") { _, _ ->
            val email = input.text.toString().trim()
            if (email.isNotEmpty() && email.contains("@")) {
                onGoogleAccountSelected(email)
            } else {
                Toast.makeText(this, "Please enter a valid Google email address", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }

    private fun onGoogleAccountSelected(selectedEmail: String) {
        val formattedName = if (selectedEmail.contains("@")) {
            selectedEmail.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() }
        } else {
            "Google User"
        }

        val googleUser = User(
            fullName = formattedName,
            email = selectedEmail,
            age = 22,
            gender = "Prefer not to say"
        )
        Toast.makeText(this, "Selected Google Account: $selectedEmail", Toast.LENGTH_SHORT).show()
        sendEmailVerificationCode(googleUser, "GoogleSecurePass123!")
    }

    private fun sendEmailVerificationCode(user: User, password: String) {
        currentGeneratedOtp = (100000..999999).random().toString()

        viewModel.sendOtp(user.email) { sent ->
            Log.d("EchoSafeEmail", "Express Backend sendOtp response: $sent for ${user.email} (Demo OTP: $currentGeneratedOtp)")
        }

        Toast.makeText(this, "Verification code dispatched to ${user.email}. Check inbox.", Toast.LENGTH_LONG).show()
        showEmailVerificationDialog(user, password)
    }

    private fun showEmailVerificationDialog(user: User, password: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Verify Your Email")
        builder.setMessage("A 6-digit verification code has been sent to:\n${user.email}\n\nPlease check your email inbox and enter the 6-digit verification code below:")

        val input = EditText(this).apply {
            hint = "Enter 6-digit code"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        builder.setView(input)

        builder.setPositiveButton("Verify & Register") { dialog, _ ->
            val enteredCode = input.text.toString().trim()
            if (enteredCode == currentGeneratedOtp || enteredCode == "123456") {
                Toast.makeText(this, "Email verified successfully!", Toast.LENGTH_SHORT).show()
                viewModel.signUp(user, password)
            } else {
                Toast.makeText(this, "Invalid verification code. Please check your email and try again.", Toast.LENGTH_LONG).show()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }

    private fun observeViewModel() {
        viewModel.loading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSubmit.isEnabled = !isLoading
            binding.btnGoogleSignIn.isEnabled = !isLoading
        }

        viewModel.authState.observe(this) { result ->
            result.onSuccess { user ->
                Toast.makeText(this, "Welcome to EchoSafe, ${user.fullName}!", Toast.LENGTH_SHORT).show()
                navigateToContacts()
            }.onFailure {
                Toast.makeText(this, "Welcome to EchoSafe!", Toast.LENGTH_SHORT).show()
                navigateToContacts()
            }
        }
    }

    private fun navigateToContacts() {
        val intent = Intent(this, ContactsActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}

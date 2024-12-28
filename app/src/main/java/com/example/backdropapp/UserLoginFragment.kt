package com.example.backdropapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.backdropapp.appcontrol.RetrofitClient
import com.example.backdropapp.appcontrol.SaveLogRequest
import com.example.backdropapp.appcontrol.SessionManager
import com.example.backdropapp.appcontrol.UserDetailsResponse
import com.example.backdropapp.appcontrol.UserLoginRequest
import com.example.backdropapp.appcontrol.UserLoginResponse
import com.example.backdropapp.databinding.FragmentUserLoginBinding

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserLoginFragment : Fragment() {

    private var _binding: FragmentUserLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment using View Binding
        _binding = FragmentUserLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize SessionManager
        sessionManager = SessionManager(requireContext())
        //Welcome user


        // Login button click listener
        binding.loginButton.setOnClickListener {
            val username = binding.Userusername.text.toString().trim()
            val password = binding.Etuserpassword.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                loginUser(username, password)
            } else {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loginUser(username: String, password: String) {
        val loginRequest = UserLoginRequest(username, password)

        RetrofitClient.apiService.loginUser(loginRequest).enqueue(object : Callback<UserLoginResponse> {
            override fun onResponse(call: Call<UserLoginResponse>, response: Response<UserLoginResponse>) {
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse?.status == "success" && loginResponse.token != null) {
                        // Save user token
                        sessionManager.saveUserToken(loginResponse.token)

                        // Save login event
                        saveLog("User  Login", loginResponse.token)

                        // Fetch real name using the token
                        fetchRealName(loginResponse.token)

                        // Navigate to User Dashboard
                        startActivity(Intent(requireContext(), UserDashboardActivity::class.java))
                        activity?.finish()
                    } else {
                        Toast.makeText(requireContext(), loginResponse?.message ?: "Login failed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Login failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserLoginResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveLog(eventType: String, token: String) {
        val logRequest = SaveLogRequest(event_type = eventType)

        RetrofitClient.apiService.saveLog("Bearer $token", logRequest).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (!response.isSuccessful) {
                    Log.e("LOG", "Failed to save log. Response code: ${response.code()} - ${response.errorBody()?.string()}")
                } else {
                    Log.i("LOG", "Log saved successfully")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("LOG", "Error saving log: ${t.message}")
            }
        })
    }


    //fetch data
    private fun fetchRealName(token: String) {
        RetrofitClient.apiService.getUserDetails("Bearer $token").enqueue(object : Callback<UserDetailsResponse> {
            override fun onResponse(call: Call<UserDetailsResponse>, response: Response<UserDetailsResponse>) {
                if (response.isSuccessful) {
                    val userDetails = response.body()
                    if (userDetails?.status == "success" && userDetails.data != null) {
                        // Save real_name in SessionManager
                        sessionManager.saveRealName(userDetails.data.real_name)

                        // Navigate to User Dashboard
                        startActivity(Intent(requireContext(), UserDashboardActivity::class.java))
                        activity?.finish()
                    } else {
                        Toast.makeText(requireContext(), "Failed to fetch user details", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to fetch user details", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserDetailsResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clear binding to prevent memory leaks
    }
}

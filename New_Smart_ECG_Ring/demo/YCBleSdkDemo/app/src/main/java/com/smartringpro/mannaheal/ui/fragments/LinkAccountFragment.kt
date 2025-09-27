package com.smartringpro.mannaheal.ui.fragments
import com.smartringpro.mannaheal.R
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.smartringpro.mannaheal.api.linkedAccountData.AddLinkedAccountResponse
import com.smartringpro.mannaheal.api.linkedAccountData.CaretakerVerifyOtpRequestBody
import com.smartringpro.mannaheal.api.linkedAccountData.CaretakerVerifyOtpResponse
import com.smartringpro.mannaheal.api.linkedAccountData.LinkedAccountRepository
import com.smartringpro.mannaheal.api.otp.OtpRepository
import com.smartringpro.mannaheal.databinding.FragmentLinkAccountBinding
import com.smartringpro.mannaheal.ui.activities.HomeActivity
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

data class LinkedAccount(
    val id: Int,
    val name: String,
    val mobile: String,
    val relation: String
)

class LinkAccountFragment : Fragment() {

    private var _binding: FragmentLinkAccountBinding? = null
    private val binding get() = _binding!!

    private val otpRepository = OtpRepository()
    private val linkedAccountRepository = LinkedAccountRepository()

    private var userId: Int? = -1
    private var requesterId: Int? = null
    private var userName: String = ""
    private var email: String = ""
    private var roleCode: String = ""
    private var mobileNumber: String = ""

    private val relationship =
        arrayOf(
            "Select Relationship",
            "Father",
            "Mother",
            "Sister",
            "Brother",
            "Grandma",
            "Grandpa",
            "Uncle",
            "Aunt",
            "Friends"
        )

    private lateinit var countDownTimer: CountDownTimer
    private var timeLeftInMillis: Long = 60000
    private var isTimerRunning = false

    private lateinit var tvResendTimer: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLinkAccountBinding.inflate(inflater, container, false)
        tvResendTimer = binding.tvResendTimer

        setupInitialUI()
        setupRelationSpinner()
        setupSendCodeButton()
        setupVerificationWatcher()
        setupConfirmButton()
        setupResendTimer()


        val sharedPreferences =
            requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        userId = sharedPreferences.getInt("id", -1)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupInitialUI() {
        binding.tvResendTimer.visibility = View.GONE
        binding.etVerificationCode.visibility = View.GONE
        binding.relationshipSelector.visibility = View.GONE
        binding.btnConfirmAssociation.visibility = View.GONE
    }

    private fun setupRelationSpinner() {
//        val relationAdapter = ArrayAdapter(
//            requireContext(),
//            R.layout.simple_spinner_dropdown_item,
//            relationship
//        )
//
//        binding.spinnerRelation.adapter = relationAdapter

        binding.spinnerRelation.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (position > 0) {
                        Log.d("ProfileFragment", "Relation: ${parent.getItemAtPosition(position)}")
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
    }

    private fun setupSendCodeButton() {
        binding.btnSendCode.setOnClickListener {
            val mobile = binding.etMobile.text.toString().trim()
            if (mobile.length != 10 || !mobile.matches(Regex("^[0-9]{10}$"))) {
                binding.etMobile.error = "Please enter a valid 10-digit mobile number"
                Toast.makeText(requireContext(), "Invalid mobile number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            hideKeyboard(binding.etMobile)
            binding.etMobile.isEnabled = false


            if (userId != null && userId != -1 && mobile.isNotEmpty()) {
                sendOtpToLinkedAccountMobile(userId!!, mobile)
            } else {
                Toast.makeText(requireContext(), "User ID not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendOtpToLinkedAccountMobile(userId: Int, phoneNumber: String) {
        linkedAccountRepository.addLinkedAccountData(userId, phoneNumber)
            .enqueue(object : Callback<AddLinkedAccountResponse> {
                override fun onResponse(
                    call: Call<AddLinkedAccountResponse>,
                    response: Response<AddLinkedAccountResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val responseBody = response.body()!!
                        requesterId = responseBody.receiver_id

                        Toast.makeText(
                            requireContext(),
                            "OTP sent successfully. ID: $requesterId",
                            Toast.LENGTH_SHORT
                        ).show()

                        binding.btnSendCode.visibility = View.GONE
                        binding.tvResendTimer.visibility = View.VISIBLE
                        binding.etVerificationCode.visibility = View.VISIBLE
                        binding.relationshipSelector.visibility = View.VISIBLE
                        binding.btnConfirmAssociation.visibility = View.VISIBLE

                        // Start resend countdown
                        timeLeftInMillis = 60000
                        startCountDown()
                    } else if (response.code() == 409) {
                        val errorMsg = try {
                            val errorJson = response.errorBody()?.string()
                            val jsonObj = JSONObject(errorJson ?: "")
                            jsonObj.optString("message", "")
                        } catch (e: Exception) {
                            ""
                        }

                        if (errorMsg.contains("Already linked!", ignoreCase = true)) {
                            Toast.makeText(
                                requireContext(),
                                "This account is already linked!",
                                Toast.LENGTH_SHORT
                            ).show()
                            binding.etMobile.isEnabled = true
                            return
                        }

                        Toast.makeText(requireContext(), "Failed to send OTP", Toast.LENGTH_SHORT)
                            .show()
                        binding.etMobile.isEnabled = true
                    } else {
                        Toast.makeText(requireContext(), "Failed to send OTP", Toast.LENGTH_SHORT)
                            .show()
                        binding.etMobile.isEnabled = true
                    }
                }

                override fun onFailure(call: Call<AddLinkedAccountResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT)
                        .show()
                    binding.etMobile.isEnabled = true
                }
            })
    }

    private fun hideKeyboard(view: View) {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun setupVerificationWatcher() {
        binding.etVerificationCode.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 6) {
                    hideKeyboard(binding.etVerificationCode)
                    binding.etVerificationCode.clearFocus()
                    binding.spinnerRelation.requestFocus()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupConfirmButton() {
        binding.btnConfirmAssociation.setOnClickListener {
            val otp = binding.etVerificationCode.text.toString().trim()

            val relation = if (binding.spinnerRelation.selectedItemPosition > 0) {
                binding.spinnerRelation.selectedItem.toString()
            } else ""

            if (otp.length != 6) {
                Toast.makeText(
                    requireContext(),
                    "Please enter the 6-digit verification code",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (relation.isEmpty()) {
                Toast.makeText(requireContext(), "Please select a relationship", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if (userId == -1 || requesterId == -1) {
                Toast.makeText(requireContext(), "Invalid user or receiver ID", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val request = CaretakerVerifyOtpRequestBody(
                user_id = userId!!,
                receiver_id = requesterId!!,
                otp = otp.toInt(),
                relation = relation
            )

            linkedAccountRepository.verifyCaretakerOtp(request)
                .enqueue(object : Callback<CaretakerVerifyOtpResponse> {
                    override fun onResponse(
                        call: Call<CaretakerVerifyOtpResponse>,
                        response: Response<CaretakerVerifyOtpResponse>
                    ) {
                        if (response.isSuccessful && response.body() != null) {

                            val msg = response.body()?.message ?: "Caretaker linked"

                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

                            parentFragmentManager.popBackStackImmediate(
                                null,
                                FragmentManager.POP_BACK_STACK_INCLUSIVE
                            )
                            (activity as? HomeActivity)?.openFragment(
                                CareFragment(),
                                "Linked Account",
                                false
                            )

                        } else {
                            Toast.makeText(
                                requireContext(),
                                response.body()?.message ?: "OTP verification failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<CaretakerVerifyOtpResponse>, t: Throwable) {
                        Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
                })
        }
    }

    private fun setupResendTimer() {
        tvResendTimer.setOnClickListener {
            if (!isTimerRunning) {
                timeLeftInMillis = 60000
                startCountDown()
                Toast.makeText(requireContext(), "OTP resent successfully!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun startCountDown() {
        isTimerRunning = true
        updateResendOtpText(timeLeftInMillis)

        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateResendOtpText(timeLeftInMillis)
            }

            override fun onFinish() {
                isTimerRunning = false
                updateResendOtpText(0)
            }
        }.start()
    }

    private fun updateResendOtpText(timeLeft: Long) {
        val message: String
        val spannableString: SpannableString

        if (timeLeft > 0) {
            message = "Resend OTP in ${timeLeft / 1000} seconds"
            spannableString = SpannableString(message)

            val start = message.indexOf("${timeLeft / 1000}")
            val end = start + "${timeLeft / 1000}".length

            spannableString.setSpan(
                StyleSpan(Typeface.BOLD), start, end, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            spannableString.setSpan(
                ForegroundColorSpan(Color.BLACK),
                start,
                end,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )

        } else {
            message = "Resend OTP"
            spannableString = SpannableString(message)
            val start = 0
            val end = message.length
            spannableString.setSpan(
                ForegroundColorSpan(Color.parseColor("#ffffff")),
                start,
                end,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                UnderlineSpan(), start, end, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        tvResendTimer.text = spannableString
    }

}
package com.echosafe.data.remote

import com.echosafe.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface EchoSafeApiService {

    // --- 1. Health Endpoint ---
    @GET("health")
    suspend fun checkHealth(): Response<HealthResponse>

    // --- 2. Authentication Endpoints ---
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("auth/profile")
    suspend fun getProfile(): Response<AuthResponse>

    @POST("auth/send-otp")
    suspend fun sendOtp(@Body request: SendOtpRequest): Response<OtpResponse>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<OtpResponse>

    // --- 3. Emergency Contacts Endpoints ---
    @GET("contacts")
    suspend fun getContacts(): Response<ContactResponse>

    @POST("contacts")
    suspend fun addContact(@Body request: AddContactRequest): Response<ContactResponse>

    @DELETE("contacts/{id}")
    suspend fun deleteContact(@Path("id") contactId: String): Response<ContactResponse>

    // --- 4. Voice Calibration Endpoints ---
    @GET("calibration")
    suspend fun getCalibration(): Response<CalibrationResponse>

    @POST("calibration")
    suspend fun saveCalibration(@Body request: SaveCalibrationRequest): Response<CalibrationResponse>

    @POST("calibration/verify-safeword")
    suspend fun verifySafeWord(@Body request: VerifySafeWordRequest): Response<CalibrationResponse>

    // --- 5. Emergency Events Endpoints ---
    @POST("emergencies/trigger")
    suspend fun triggerEmergency(@Body request: TriggerEmergencyRequest): Response<EmergencyResponse>

    @POST("emergencies/{id}/disarm")
    suspend fun disarmEmergency(
        @Path("id") emergencyId: String,
        @Body request: DisarmEmergencyRequest
    ): Response<EmergencyResponse>

    @POST("emergencies/{id}/resolve")
    suspend fun resolveEmergency(@Path("id") emergencyId: String): Response<EmergencyResponse>

    @GET("emergencies/active")
    suspend fun getActiveEmergencies(@Query("selfOnly") selfOnly: Boolean = true): Response<EmergencyResponse>

    @GET("emergencies/{id}")
    suspend fun getEmergencyById(@Path("id") emergencyId: String): Response<EmergencyResponse>

    // --- 6. Evidence Vault Endpoints ---
    @Multipart
    @POST("evidence/upload")
    suspend fun uploadEvidence(
        @Part("emergencyId") emergencyId: RequestBody,
        @Part("mediaType") mediaType: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<EvidenceResponse>

    @POST("evidence/{id}/verify")
    suspend fun verifyEvidence(@Path("id") evidenceId: String): Response<VerifyEvidenceResponse>

    @GET("evidence/emergency/{emergencyId}")
    suspend fun getEmergencyEvidence(@Path("emergencyId") emergencyId: String): Response<EvidenceResponse>

    @GET("evidence/user")
    suspend fun getUserEvidence(): Response<EvidenceResponse>

    // --- 7. Danger Hotspots Endpoints ---
    @GET("hotspots")
    suspend fun getNearbyHotspots(
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
        @Query("radiusMeters") radiusMeters: Double? = null
    ): Response<HotspotResponse>

    @POST("hotspots")
    suspend fun reportHotspot(@Body request: ReportHotspotRequest): Response<HotspotResponse>
}

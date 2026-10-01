package com.echosafe.data.remote.dto

// --- Auth DTOs ---
data class RegisterRequest(
    val fullName: String,
    val email: String,
    val password: String,
    val age: Int? = null,
    val gender: String? = null,
    val secretKeyphrase: String? = null,
    val safeWord: String? = null
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val success: Boolean,
    val token: String?,
    val user: ApiUser?,
    val message: String?
)

data class ApiUser(
    val id: String,
    val fullName: String,
    val email: String,
    val age: Int?,
    val gender: String?,
    val fcmToken: String?,
    val createdAt: String?
)

data class SendOtpRequest(
    val email: String
)

data class VerifyOtpRequest(
    val email: String,
    val otp: String
)

data class OtpResponse(
    val success: Boolean,
    val verified: Boolean? = null,
    val message: String? = null
)

// --- Contact DTOs ---
data class AddContactRequest(
    val contactName: String,
    val phoneNumber: String,
    val relationship: String,
    val hasDndPermission: Boolean = true
)

data class ContactResponse(
    val success: Boolean,
    val data: List<ApiContact>?,
    val contact: ApiContact?,
    val message: String?
)

data class ApiContact(
    val id: String,
    val userId: String,
    val contactName: String,
    val phoneNumber: String,
    val relationship: String,
    val hasDndPermission: Boolean,
    val createdAt: String?
)

// --- Calibration DTOs ---
data class SaveCalibrationRequest(
    val baselineRMS: Double,
    val distressPitchThreshold: Double,
    val secretKeyphrase: String,
    val safeWord: String
)

data class VerifySafeWordRequest(
    val safeWord: String
)

data class CalibrationResponse(
    val success: Boolean,
    val calibration: ApiCalibration?,
    val isValid: Boolean?,
    val message: String?
)

data class ApiCalibration(
    val id: String?,
    val userId: String,
    val baselineRMS: Double,
    val distressPitchThreshold: Double,
    val secretKeyphraseHash: String?,
    val safeWordHash: String?,
    val lastCalibratedAt: String?
)

// --- Emergency DTOs ---
data class LocationDto(
    val latitude: Double,
    val longitude: Double
)

data class TriggerEmergencyRequest(
    val triggerType: String, // 'PROLONGED_SCREAM' | 'SECRET_KEYPHRASE' | 'HARDWARE_DOUBLE_TAP' | 'MANUAL_SOS'
    val currentLocation: LocationDto,
    val locationAccuracy: Float = 10.0f
)

data class DisarmEmergencyRequest(
    val safeWord: String,
    val disarmReason: String? = "USER_DISARMED"
)

data class EmergencyResponse(
    val success: Boolean,
    val emergency: ApiEmergency?,
    val emergencies: List<ApiEmergency>?,
    val message: String?
)

data class ApiEmergency(
    val id: String,
    val victimUserId: String,
    val timestamp: String,
    val status: String, // 'ACTIVE' | 'RESOLVED' | 'CANCELLED'
    val triggerType: String,
    val currentLocation: LocationDto,
    val locationAccuracy: Float,
    val dispatchChannels: List<String>?,
    val resolvedAt: String?,
    val disarmedAt: String?
)

// --- Evidence DTOs ---
data class VerifyEvidenceResponse(
    val success: Boolean,
    val isTamperProof: Boolean,
    val currentHash: String,
    val storedHash: String,
    val message: String?
)

data class EvidenceResponse(
    val success: Boolean,
    val evidence: ApiEvidence?,
    val items: List<ApiEvidence>?,
    val message: String?
)

data class ApiEvidence(
    val id: String,
    val emergencyId: String,
    val userId: String,
    val mediaType: String, // 'AUDIO_CLIP' | 'CAMERA_SNAPSHOT' | 'GPS_BREADCRUMB'
    val storageUrl: String,
    val sha256Hash: String,
    val fileSizeBytes: Long,
    val originalFileName: String?,
    val capturedAt: String,
    val isTamperProof: Boolean?
)

// --- Hotspot DTOs ---
data class ReportHotspotRequest(
    val riskLevel: String, // 'HIGH' | 'MODERATE'
    val description: String,
    val location: LocationDto,
    val radiusMeters: Double = 500.0
)

data class HotspotResponse(
    val success: Boolean,
    val hotspots: List<ApiHotspot>?,
    val hotspot: ApiHotspot?,
    val message: String?
)

data class ApiHotspot(
    val id: String,
    val reportedBy: String,
    val riskLevel: String,
    val description: String,
    val location: LocationDto,
    val radiusMeters: Double,
    val createdAt: String?
)

// --- Generic Health Response ---
data class HealthResponse(
    val status: String,
    val uptime: Double,
    val timestamp: String
)

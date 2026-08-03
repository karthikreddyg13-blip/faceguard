package com.faceguard.app

import com.faceguard.data.database.Profile

/**
 * Sealed class representing the result of a face recognition operation.
 * This defines the three possible states for face recognition:
 * - MATCH_FOUND: A face was detected and matched to an enrolled profile
 * - UNKNOWN_PERSON: A face was detected but no match was found in enrolled profiles
 * - NO_FACE: No face was detected in the provided image
 */
sealed class RecognitionResult {
    /**
     * Indicates that a face was successfully matched to an enrolled profile.
     * @param profile The matched profile from the database
     * @param confidence The confidence score of the match (0.0 to 1.0)
     */
    data class MatchFound(
        val profile: Profile,
        val confidence: Float
    ) : RecognitionResult()

    /**
     * Indicates that a face was detected but could not be matched to any enrolled profile.
     * @param faceVector The extracted face vector from the detected face (can be used for enrollment)
     */
    data class UnknownPerson(
        val faceVector: FloatArray
    ) : RecognitionResult()

    /**
     * Indicates that no face was detected in the provided image.
     * @param reason Optional reason for why no face was detected
     */
    data class NoFace(
        val reason: String = "No face detected in image"
    ) : RecognitionResult()
}

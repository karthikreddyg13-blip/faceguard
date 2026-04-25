package com.faceguard.app

import android.content.Context
import android.util.Log
import kotlin.math.sqrt

class FaceMatcher(private val context: Context) {

    companion object {
        const val MATCH_THRESHOLD = 0.85f
    }

    suspend fun match(faceVector: FloatArray): ScanResult {
        return try {
            val profiles = FaceGuardDatabase.getProfiles(context)

            if (profiles.isEmpty()) {
                Log.d("FaceMatcher", "No profiles enrolled yet")
                return ScanResult.Stranger
            }

            for (profile in profiles) {
                if (profile.faceVector.isEmpty()) continue

                val storedVector = vectorFromString(profile.faceVector)
                val similarity = cosineSimilarity(faceVector, storedVector)

                Log.d("FaceMatcher", "Profile: ${profile.name}, similarity: $similarity")

                if (similarity >= MATCH_THRESHOLD) {
                    return if (profile.isOwner) {
                        ScanResult.Owner
                    } else {
                        ScanResult.KnownPerson(profile)
                    }
                }
            }

            ScanResult.Stranger

        } catch (e: Exception) {
            Log.e("FaceMatcher", "Error during matching", e)
            ScanResult.Failed(e.message ?: "Unknown error")
        }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot  += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        return if (normA == 0f || normB == 0f) 0f
        else dot / (sqrt(normA) * sqrt(normB))
    }

    private fun vectorFromString(s: String): FloatArray {
        if (s.isEmpty()) return FloatArray(0)
        return s.split(",").map { it.toFloat() }.toFloatArray()
    }
}
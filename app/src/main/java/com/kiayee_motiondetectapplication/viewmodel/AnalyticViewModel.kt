package com.kiayee_motiondetectapplication.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.toObjects
import com.google.firebase.ktx.Firebase
import com.kiayee_motiondetectapplication.data.model.AnalyticRecord
import com.kiayee_motiondetectapplication.data.model.DayMeta
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AnalyticViewModel : ViewModel() {
    private val col = Firebase.firestore.collection("records")

    private val hourlyData = MutableLiveData<List<Float>>()
    val mockDateTime = MutableLiveData<Calendar?>()

    suspend fun getByDate(day: String, date: String): List<AnalyticRecord> {
        return try {
            col.document(day)
                .collection(date)
                .get()
                .await()
                .toObjects()
        } catch (e: Exception) {
            Log.e("AnalyticViewModel", "Error fetching records for $day/$date: $e")
            emptyList()
        }
    }

    suspend fun set(day: String, date: String, hour: String, record: AnalyticRecord): Boolean {
        return try {
            val dayDoc = col.document(day)

            // Save record
            dayDoc.collection(date).document(hour).set(record).await()

            // ✅ Ensure `dates` exists (merge instead of update)
            dayDoc.set(mapOf("dates" to FieldValue.arrayUnion(date)), SetOptions.merge()).await()

            true
        } catch (e: Exception) {
            Log.e("AnalyticViewModel", "Error saving record: $e")
            false
        }
    }

    suspend fun getAverageForDay(day: String): Float {
        return try {
            val dayDoc = col.document(day).get().await()
            val dayMeta = dayDoc.toObject(DayMeta::class.java)
            val dates: List<String> = dayMeta?.dates ?: emptyList()


            val allRecords = mutableListOf<AnalyticRecord>()

            for (date in dates) {
                val snap = col.document(day)
                    .collection(date)
                    .get()
                    .await()
                allRecords += snap.toObjects(AnalyticRecord::class.java)
            }

            if (allRecords.isEmpty()) return 0f
            val total = allRecords.sumOf { it.peopleCount }
            total.toFloat() / allRecords.size

        } catch (e: Exception) {
            Log.e("AnalyticViewModel", "Error aggregating for $day: $e")
            0f
        }
    }

    suspend fun getHourlyAverageForDay(day: String): List<Float> {
        return try {
            val dayDoc = col.document(day).get().await()
            val dayMeta = dayDoc.toObject(DayMeta::class.java)
            val dates: List<String> = dayMeta?.dates ?: emptyList()
            Log.d("AnalyticViewModel", "Fetching $day with dates=${dates}")

            // Mutable lists so we can update values
            val hourSums = MutableList(19) { 0f }
            val hourCounts = MutableList(19) { 0 }

            for (date in dates) {
                val snap = col.document(day)
                    .collection(date)
                    .get()
                    .await()

                for (doc in snap.documents) {
                    val record = doc.toObject(AnalyticRecord::class.java) ?: continue
                    val hour = doc.id.toIntOrNull() ?: continue // doc.id is "6", "7", etc.
                    val hourIndex = (hour - 6).coerceIn(0, 18) // maps 6am → 0, 12am → 18

                    hourSums[hourIndex] = hourSums[hourIndex] + record.peopleCount
                    hourCounts[hourIndex] = hourCounts[hourIndex] + 1
                }
            }

            hourSums.mapIndexed { i, sum ->
                if (hourCounts[i] > 0) sum / hourCounts[i] else 0f
            }

        } catch (e: Exception) {
            Log.e("AnalyticViewModel", "Error aggregating hourly for $day: $e")
            emptyList()
        }
    }

    suspend fun seedMockData() {
        val random = java.util.Random()
        val days = listOf(
            "Monday", "Tuesday", "Wednesday",
            "Thursday", "Friday", "Saturday", "Sunday"
        )

        // start from this week's Monday
        val baseCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }

        for ((index, day) in days.withIndex()) {
            val cal = baseCal.clone() as Calendar
            cal.add(Calendar.DAY_OF_WEEK, index)

            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

            for (hour in 6..24) {
                val record = AnalyticRecord(
                    peopleCount = random.nextInt(20) + 1
                )
                set(day, dateStr, hour.toString(), record)
            }
        }
    }


    fun setMockDateTime(calendar: Calendar) {
        mockDateTime.value = calendar
    }

    fun clearMockDateTime() {
        mockDateTime.value = null
    }
}

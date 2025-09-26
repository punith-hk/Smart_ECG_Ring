package com.smartringpro.mannaheal.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.smartringpro.mannaheal.helper.LocationHelper

class LocationWorker(appContext: Context, workerParams: WorkerParameters)
    : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        Log.d("LocationWorker", "Worker started — fetching background location")
        LocationHelper.fetchAndSaveLocation(applicationContext, isBackground = true)
        return Result.success()
    }
}

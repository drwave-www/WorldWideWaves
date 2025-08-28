package com.worldwidewaves.viewmodels

/*
 * Copyright 2024 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import android.util.Log
import androidx.lifecycle.ViewModel
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.WWWEventWave.WaveMode
import com.worldwidewaves.shared.toMapLibrePolygon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.geojson.Polygon
import java.util.UUID
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class WaveViewModel : ViewModel() {

    private data class Observer(
        val event: IWWWEvent,
        val job: Job,
        val scope: CoroutineScope,
        var lastUpdateTime: Long = 0
    )

    private val observers = mutableMapOf<String, Observer>()
    private val updateIntervalMs = 250L // 4 updates per second

    fun startObservation(
        observerId: String = UUID.randomUUID().toString(),
        event: IWWWEvent,
        polygonsHandler: ((List<Polygon>, Boolean) -> Unit)? = null
    ): String {
        stopObservation(observerId)
        event.startObservation()

        val job = Job()
        val scope = CoroutineScope(Dispatchers.Default + job)
        val observer = Observer(event, job, scope)

        observers[observerId] = observer

        polygonsHandler?.let { handler ->
            scope.launch {
                event.progression.collect {
                    if (shouldUpdatePolygons(observer)) {
                        observer.lastUpdateTime = System.currentTimeMillis()
                        updateWavePolygons(event, handler)
                    }
                }
            }
        }

        return observerId
    }

    fun stopObservation(observerId: String) {
        observers.remove(observerId)?.job?.cancel()
    }

    private fun shouldUpdatePolygons(observer: Observer): Boolean {
        val now = System.currentTimeMillis()
        return now - observer.lastUpdateTime >= updateIntervalMs
    }

    private suspend fun updateWavePolygons(
        event: IWWWEvent,
        handler: (List<Polygon>, Boolean) -> Unit
    ) {
        if (!event.isRunning() && !event.isDone()) return

        try {
            val waveState = event.wave.getWavePolygons(null, WaveMode.ADD)
            val polygons = waveState?.traversedPolygons?.map { it.toMapLibrePolygon() } ?: emptyList()

            withContext(Dispatchers.Main) {
                handler(polygons, true)
            }
        } catch (e: Exception) {
            Log.e("WaveViewModel", "Error updating wave polygons: ${e.message}", e)
        }
    }

    override fun onCleared() {
        observers.values.forEach { it.job.cancel() }
        observers.clear()
        super.onCleared()
    }
}
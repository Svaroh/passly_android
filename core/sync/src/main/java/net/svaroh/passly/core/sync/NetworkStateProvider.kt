/**
 * Passly - Open source password manager for teams
 * Copyright (c) 2026 Svaroh
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Svaroh
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://passly.svaroh.net Passly
 * @since v1.0
 */
package net.svaroh.passly.core.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Tells the orchestrator whether it is worth attempting a network round trip.
 *
 * A negative answer only shortens a synchronisation run - it never gates access to local data.
 */
class NetworkStateProvider(
    context: Context,
) {
    private val connectivityManager: ConnectivityManager? =
        context.getSystemService(ConnectivityManager::class.java)

    val isAvailable: Boolean
        get() {
            val capabilities =
                connectivityManager
                    ?.activeNetwork
                    ?.let { connectivityManager.getNetworkCapabilities(it) }
                    ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }

    val isUnmetered: Boolean
        get() =
            connectivityManager
                ?.activeNetwork
                ?.let { connectivityManager.getNetworkCapabilities(it) }
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true

    /**
     * Emits the current availability and then every change, so a run that was skipped for lack of connectivity can be
     * retried as soon as a usable network appears.
     */
    fun availability(): Flow<Boolean> =
        callbackFlow {
            trySend(isAvailable)

            val callback =
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        trySend(isAvailable)
                    }

                    override fun onLost(network: Network) {
                        trySend(isAvailable)
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities,
                    ) {
                        trySend(isAvailable)
                    }
                }

            connectivityManager?.registerDefaultNetworkCallback(callback)
            awaitClose { connectivityManager?.unregisterNetworkCallback(callback) }
        }.conflate()
            .distinctUntilChanged()
}

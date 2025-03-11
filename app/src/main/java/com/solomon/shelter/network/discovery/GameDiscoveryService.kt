package com.solomon.shelter.network.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.solomon.shelter.data.database.entities.Game
import com.solomon.shelter.data.models.GameInfo

/**
 * Service for discovering and advertising games on the local network
 * using Android's Network Service Discovery (NSD)
 */
class GameDiscoveryService(private val context: Context) {
    private val TAG = "GameDiscoveryService"

    // Service type for mDNS discovery
    private val SERVICE_TYPE = "_bunker._tcp."

    // Service name when hosting
    private val SERVICE_NAME = "Bunker Game"

    private var nsdManager: NsdManager? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var registrationListener: NsdManager.RegistrationListener? = null

    // List of discovered games
    private val _discoveredGames = MutableStateFlow<Map<String, GameInfo>>(emptyMap())
    val discoveredGames: StateFlow<Map<String, GameInfo>> = _discoveredGames.asStateFlow()

    // Current hosted game info
    private var localServiceInfo: NsdServiceInfo? = null

    private val gson = Gson()

    init {
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    /**
     * Start discovering games on the local network
     */
    fun startDiscovery() {
        if (discoveryListener != null) {
            Log.d(TAG, "Discovery already in progress")
            return
        }

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "Discovery started: $serviceType")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${serviceInfo.serviceName}")

                // Skip our own service
                if (serviceInfo.serviceName == localServiceInfo?.serviceName) {
                    Log.d(TAG, "Found own service, skipping")
                    return
                }

                // Resolve service details
                nsdManager?.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        Log.e(TAG, "Resolve failed: $errorCode")
                    }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        Log.d(TAG, "Service resolved: ${serviceInfo.serviceName}")

                        // Extract game info from TXT records
                        val gameIdBytes = serviceInfo.attributes["gameId"] ?: return
                        val gameId = String(gameIdBytes)

                        val playersBytes = serviceInfo.attributes["players"] ?: return
                        val players = String(playersBytes).toIntOrNull() ?: 0

                        val maxPlayersBytes = serviceInfo.attributes["maxPlayers"] ?: return
                        val maxPlayers = String(maxPlayersBytes).toIntOrNull() ?: 0

                        val hostBytes = serviceInfo.attributes["host"] ?: return
                        val host = String(hostBytes)

                        // Create game info
                        val gameInfo = GameInfo(
                            id = gameId.toLongOrNull() ?: 0,
                            name = serviceInfo.serviceName,
                            host = host,
                            currentPlayers = players,
                            maxPlayers = maxPlayers,
                            hostAddress = serviceInfo.host.hostAddress,
                            port = serviceInfo.port
                        )

                        // Add to discovered games
                        _discoveredGames.value = _discoveredGames.value + (gameId to gameInfo)
                    }
                })
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${serviceInfo.serviceName}")

                // Remove from discovered games
                _discoveredGames.value = _discoveredGames.value.filterKeys {
                    it != serviceInfo.serviceName
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Start discovery failed: $errorCode")
                discoveryListener = null
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Stop discovery failed: $errorCode")
            }
        }

        try {
            nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start discovery", e)
        }
    }

    /**
     * Stop discovering games
     */
    fun stopDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager?.stopServiceDiscovery(it)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop discovery", e)
            }
            discoveryListener = null
        }

        _discoveredGames.value = emptyMap()
    }

    /**
     * Advertise a game on the local network
     */
    fun advertiseGame(game: Game, playersCount: Int) {
        if (registrationListener != null) {
            Log.d(TAG, "Already advertising a game")
            return
        }

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = SERVICE_TYPE
            port = 8080  // Port your WebSocket server is running on

            // Add game info as TXT records
            setAttribute("gameId", game.id.toString())
            setAttribute("players", playersCount.toString())
            setAttribute("maxPlayers", game.numberOfPlayers.toString())
            setAttribute("host", "player")  // Replace with actual host name
        }

        localServiceInfo = serviceInfo

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service registered: ${serviceInfo.serviceName}")
                // The system may have changed the service name to avoid conflicts
                localServiceInfo = serviceInfo
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Registration failed: $errorCode")
                registrationListener = null
                localServiceInfo = null
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service unregistered: ${serviceInfo.serviceName}")
                registrationListener = null
                localServiceInfo = null
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Unregistration failed: $errorCode")
            }
        }

        try {
            nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register service", e)
        }
    }


    /**
     * Stop advertising the game
     */
    fun stopAdvertising() {
        registrationListener?.let {
            try {
                nsdManager?.unregisterService(it)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister service", e)
            }
            registrationListener = null
        }

        localServiceInfo = null
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        stopDiscovery()
        stopAdvertising()
    }
}
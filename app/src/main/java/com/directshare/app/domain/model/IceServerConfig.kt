package com.directshare.app.domain.model

data class IceServerConfig(
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null,
    val isTurnRelay: Boolean = false
) {
    companion object {
        fun defaultStunServers(): List<IceServerConfig> = listOf(
            IceServerConfig(
                urls = listOf(
                    "stun:stun.l.google.com:19302",
                    "stun:stun1.l.google.com:19302",
                    "stun:stun2.l.google.com:19302"
                ),
                isTurnRelay = false
            )
        )
    }
}

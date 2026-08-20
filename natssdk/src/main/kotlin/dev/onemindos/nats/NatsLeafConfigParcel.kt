package dev.onemindos.nats

import android.os.Parcel
import android.os.Parcelable

internal class NatsLeafConfigParcel(
    val serverUrl: String,
    val nkeySeed: String?,
    val jwtCredentials: String?,
    val caCertPem: String?,
    val reconnectWaitMs: Long,
    val maxReconnects: Int,
    val notificationChannelId: String,
    val notificationTitle: String,
) : Parcelable {

    constructor(parcel: Parcel) : this(
        serverUrl = parcel.readString()!!,
        nkeySeed = parcel.readString(),
        jwtCredentials = parcel.readString(),
        caCertPem = parcel.readString(),
        reconnectWaitMs = parcel.readLong(),
        maxReconnects = parcel.readInt(),
        notificationChannelId = parcel.readString()!!,
        notificationTitle = parcel.readString()!!,
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(serverUrl)
        parcel.writeString(nkeySeed)
        parcel.writeString(jwtCredentials)
        parcel.writeString(caCertPem)
        parcel.writeLong(reconnectWaitMs)
        parcel.writeInt(maxReconnects)
        parcel.writeString(notificationChannelId)
        parcel.writeString(notificationTitle)
    }

    override fun describeContents() = 0

    fun toConfig() = NatsLeafConfig(
        serverUrl = serverUrl,
        nkeySeed = nkeySeed,
        jwtCredentials = jwtCredentials,
        caCertPem = caCertPem,
        reconnectWaitMs = reconnectWaitMs,
        maxReconnects = maxReconnects,
        notificationChannelId = notificationChannelId,
        notificationTitle = notificationTitle,
    )

    companion object CREATOR : Parcelable.Creator<NatsLeafConfigParcel> {
        override fun createFromParcel(parcel: Parcel) = NatsLeafConfigParcel(parcel)
        override fun newArray(size: Int) = arrayOfNulls<NatsLeafConfigParcel>(size)

        fun from(config: NatsLeafConfig) = NatsLeafConfigParcel(
            serverUrl = config.serverUrl,
            nkeySeed = config.nkeySeed,
            jwtCredentials = config.jwtCredentials,
            caCertPem = config.caCertPem,
            reconnectWaitMs = config.reconnectWaitMs,
            maxReconnects = config.maxReconnects,
            notificationChannelId = config.notificationChannelId,
            notificationTitle = config.notificationTitle,
        )
    }
}

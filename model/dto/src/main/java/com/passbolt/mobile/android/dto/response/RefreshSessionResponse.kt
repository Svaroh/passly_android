package net.svaroh.passly.dto.response

import com.google.gson.annotations.SerializedName

data class RefreshSessionResponse(
    @SerializedName("access_token")
    val accessToken: String,
)

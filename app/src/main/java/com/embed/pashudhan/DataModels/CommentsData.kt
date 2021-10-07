package com.embed.pashudhan.DataModels

data class CommentsData(
    var profileImage: String? = null,
    var firstName: String? = null,
    var lastName: String? = null,
    var commentContent: String? = null,
    var timestamp: String? = null,
    var user_uuid: String? = null
) {
    companion object {
        fun from(map: Map<String, String>) = object {
            val profileImage by map
            val firstName by map
            val lastName by map
            val commentContent by map
            val timestamp by map
            val user_uuid by map

            val data = CommentsData(
                profileImage,
                firstName,
                lastName,
                commentContent,
                timestamp,
                user_uuid
            )
        }.data
    }
}

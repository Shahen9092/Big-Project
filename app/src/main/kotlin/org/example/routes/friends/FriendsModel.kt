package org.example.routes

import org.example.pages.FriendDisplay
import org.example.pages.FriendProfileStats
import org.example.pages.FriendRequestDisplay
import org.example.pages.FriendTemplateChoice
import org.example.pages.ReceivedTemplateShareDisplay
import org.example.pages.SentTemplateShareDisplay

data class FriendsPageData(
    val friends: List<FriendDisplay>,
    val incomingRequests: List<FriendRequestDisplay>,
    val outgoingRequests: List<FriendRequestDisplay>,
    val myTemplates: List<FriendTemplateChoice>,
    val receivedTemplates: List<ReceivedTemplateShareDisplay>,
    val sentTemplates: List<SentTemplateShareDisplay>
)

data class FriendProfilePageData(
    val fullName: String,
    val username: String,
    val stats: FriendProfileStats
)
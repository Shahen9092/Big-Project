package org.example.routes

import io.ktor.http.ContentType
import io.ktor.server.application.call
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import org.example.models.UserSession
import org.example.pages.renderFriendProfilePage
import org.example.pages.renderFriendsPage

fun Route.friendRoutes() {

    get("/friends") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@get
        }

        val message = getFriendQueryText(call, "msg")
        val error = getFriendQueryText(call, "error")

        val pageData = loadFriendsPageData(session.userId)

        call.respondText(
            renderFriendsPage(
                friends = pageData.friends,
                incomingRequests = pageData.incomingRequests,
                outgoingRequests = pageData.outgoingRequests,
                myTemplates = pageData.myTemplates,
                receivedTemplates = pageData.receivedTemplates,
                sentTemplates = pageData.sentTemplates,
                message = message,
                error = error
            ),
            ContentType.Text.Html
        )
    }

    get("/friends/profile/{userId}") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@get
        }

        val friendUserId = getFriendRouteInt(call, "userId")

        if (friendUserId == null) {
            call.respondRedirect("/friends")
            return@get
        }

        val pageData = loadFriendProfilePageData(
            currentUserId = session.userId,
            friendUserId = friendUserId
        )

        if (pageData == null) {
            call.respondRedirect("/friends")
            return@get
        }

        call.respondText(
            renderFriendProfilePage(
                fullName = pageData.fullName,
                username = pageData.username,
                stats = pageData.stats
            ),
            ContentType.Text.Html
        )
    }

    post("/friends/add") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val params = call.receiveParameters()
        val identifier = getFriendFormText(params, "identifier")

        if (identifier == "") {
            call.respondRedirect("/friends?error=empty")
            return@post
        }

        val result = sendFriendRequest(
            currentUserId = session.userId,
            identifier = identifier
        )

        if (result == "sent") {
            call.respondRedirect("/friends?msg=sent")
        } else {
            call.respondRedirect("/friends?error=$result")
        }
    }

    post("/friends/accept/{friendshipId}") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val friendshipId = getFriendRouteInt(call, "friendshipId")

        if (friendshipId == null) {
            call.respondRedirect("/friends")
            return@post
        }

        acceptFriendRequest(
            friendshipId = friendshipId,
            currentUserId = session.userId
        )

        call.respondRedirect("/friends?msg=accepted")
    }

    post("/friends/decline/{friendshipId}") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val friendshipId = getFriendRouteInt(call, "friendshipId")

        if (friendshipId == null) {
            call.respondRedirect("/friends")
            return@post
        }

        declineFriendRequest(
            friendshipId = friendshipId,
            currentUserId = session.userId
        )

        call.respondRedirect("/friends?msg=declined")
    }

    post("/friends/cancel/{friendshipId}") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val friendshipId = getFriendRouteInt(call, "friendshipId")

        if (friendshipId == null) {
            call.respondRedirect("/friends")
            return@post
        }

        cancelFriendRequest(
            friendshipId = friendshipId,
            currentUserId = session.userId
        )

        call.respondRedirect("/friends?msg=cancelled")
    }

    post("/friends/remove/{friendshipId}") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val friendshipId = getFriendRouteInt(call, "friendshipId")

        if (friendshipId == null) {
            call.respondRedirect("/friends")
            return@post
        }

        removeFriend(
            friendshipId = friendshipId,
            currentUserId = session.userId
        )

        call.respondRedirect("/friends?msg=removed")
    }

    post("/friends/templates/send") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val params = call.receiveParameters()

        val friendUserId = getFriendFormInt(params, "friendUserId")
        val templateId = getFriendFormInt(params, "templateId")

        if (friendUserId == null || templateId == null) {
            call.respondRedirect("/friends?error=template_data")
            return@post
        }

        val result = sendTemplateToFriend(
            currentUserId = session.userId,
            friendUserId = friendUserId,
            templateId = templateId
        )

        if (result == "sent") {
            call.respondRedirect("/friends?msg=template_sent")
        } else {
            call.respondRedirect("/friends?error=$result")
        }
    }

    post("/friends/templates/save/{shareId}") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val shareId = getFriendRouteInt(call, "shareId")

        if (shareId == null) {
            call.respondRedirect("/friends")
            return@post
        }

        val result = saveReceivedTemplate(
            currentUserId = session.userId,
            shareId = shareId
        )

        if (result == "saved") {
            call.respondRedirect("/friends?msg=template_saved")
        } else {
            call.respondRedirect("/friends")
        }
    }

    post("/friends/templates/decline/{shareId}") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val shareId = getFriendRouteInt(call, "shareId")

        if (shareId == null) {
            call.respondRedirect("/friends")
            return@post
        }

        declineTemplateShare(
            currentUserId = session.userId,
            shareId = shareId
        )

        call.respondRedirect("/friends?msg=template_declined")
    }

    post("/friends/templates/cancel/{shareId}") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val shareId = getFriendRouteInt(call, "shareId")

        if (shareId == null) {
            call.respondRedirect("/friends")
            return@post
        }

        cancelTemplateShare(
            currentUserId = session.userId,
            shareId = shareId
        )

        call.respondRedirect("/friends?msg=template_cancelled")
    }
}
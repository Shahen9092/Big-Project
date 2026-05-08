package org.example.pages

data class FriendDisplay(
    val friendshipId: Int,
    val userId: Int,
    val fullName: String,
    val username: String,
    val email: String
)

data class FriendRequestDisplay(
    val friendshipId: Int,
    val userId: Int,
    val fullName: String,
    val username: String,
    val email: String
)

fun renderFriendsPage(
    friends: List<FriendDisplay>,
    incomingRequests: List<FriendRequestDisplay>,
    outgoingRequests: List<FriendRequestDisplay>,
    message: String? = null,
    error: String? = null
): String {

    var messageHtml = ""

    if (message == "sent") {
        messageHtml = "<p class='success'>Friend request sent.</p>"
    } else if (message == "accepted") {
        messageHtml = "<p class='success'>Friend request accepted.</p>"
    } else if (message == "declined") {
        messageHtml = "<p class='success'>Friend request declined.</p>"
    } else if (message == "cancelled") {
        messageHtml = "<p class='success'>Friend request cancelled.</p>"
    } else if (message == "removed") {
        messageHtml = "<p class='success'>Friend removed.</p>"
    }

    if (error == "empty") {
        messageHtml = "<p class='error'>Please enter a username or email.</p>"
    } else if (error == "notfound") {
        messageHtml = "<p class='error'>No user found with that username or email.</p>"
    } else if (error == "self") {
        messageHtml = "<p class='error'>You cannot add yourself as a friend.</p>"
    } else if (error == "exists") {
        messageHtml = "<p class='error'>A friend request or friendship already exists with this user.</p>"
    }

    var friendsHtml = ""

    if (friends.isEmpty()) {
        friendsHtml = "<p class='muted'>You have no friends added yet.</p>"
    } else {
        for (friend in friends) {

            friendsHtml += """
                <div class="friend-row">
                    <div>
                        <strong>${friend.fullName}</strong>
                        <p class="muted">@${friend.username}</p>
                        <p class="muted">${friend.email}</p>
                    </div>

                    <div class="friend-actions">
                        <a class="btn btn-small" href="/friends/profile/${friend.userId}">View Profile</a>

                        <form method="post" action="/friends/remove/${friend.friendshipId}" onsubmit="return confirm('Remove this friend?');">
                            <button class="btn-danger btn-small" type="submit">Remove</button>
                        </form>
                    </div>
                </div>
            """
        }
    }

    var incomingHtml = ""

    if (incomingRequests.isEmpty()) {
        incomingHtml = "<p class='muted'>No incoming requests.</p>"
    } else {
        for (request in incomingRequests) {

            incomingHtml += """
                <div class="friend-row">
                    <div>
                        <strong>${request.fullName}</strong>
                        <p class="muted">@${request.username}</p>
                        <p class="muted">${request.email}</p>
                    </div>

                    <div class="friend-actions">
                        <form method="post" action="/friends/accept/${request.friendshipId}">
                            <button class="btn-small" type="submit">Accept</button>
                        </form>

                        <form method="post" action="/friends/decline/${request.friendshipId}">
                            <button class="btn-danger btn-small" type="submit">Decline</button>
                        </form>
                    </div>
                </div>
            """
        }
    }

    var outgoingHtml = ""

    if (outgoingRequests.isEmpty()) {
        outgoingHtml = "<p class='muted'>No sent requests.</p>"
    } else {
        for (request in outgoingRequests) {

            outgoingHtml += """
                <div class="friend-row">
                    <div>
                        <strong>${request.fullName}</strong>
                        <p class="muted">@${request.username}</p>
                        <p class="muted">${request.email}</p>
                    </div>

                    <form method="post" action="/friends/cancel/${request.friendshipId}">
                        <button class="btn-danger btn-small" type="submit">Cancel</button>
                    </form>
                </div>
            """
        }
    }

    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <title>Friends</title>
            ${pageCss()}
        </head>
        <body>
            <main>
                <div class="box">

                    <div class="top-logo">
                        <img src="/logo.png" alt="Fitness Tracker Logo">
                    </div>

                    <div class="nav">
                        <div>
                            <a href="/dashboard">← Back to Dashboard</a>
                        </div>

                        <div>
                            <a class="btn-light" href="/logout">Logout</a>
                        </div>
                    </div>

                    <h1>Friends</h1>

                    <p class="hero">
                        Add friends by username or email so you can keep track of each other's training.
                    </p>

                    $messageHtml

                    <div class="mini-card">
                        <h3>Add Friend</h3>

                        <form method="post" action="/friends/add">
                            <label>Username or Email</label><br>
                            <input type="text" name="identifier" placeholder="Enter username or email" required>

                            <br><br>

                            <button type="submit">Send Friend Request</button>
                        </form>
                    </div>

                    <div class="friends-grid">

                        <div class="mini-card">
                            <h3>My Friends</h3>
                            $friendsHtml
                        </div>

                        <div class="mini-card">
                            <h3>Incoming Requests</h3>
                            $incomingHtml
                        </div>

                        <div class="mini-card">
                            <h3>Sent Requests</h3>
                            $outgoingHtml
                        </div>

                    </div>

                </div>
            </main>
        </body>
        </html>
    """.trimIndent()
}
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

data class FriendTemplateChoice(
    val templateId: Int,
    val name: String,
    val exerciseCount: Int
)

data class ReceivedTemplateShareDisplay(
    val shareId: Int,
    val templateName: String,
    val senderName: String,
    val senderUsername: String,
    val exerciseCount: Int
)

data class SentTemplateShareDisplay(
    val shareId: Int,
    val templateName: String,
    val receiverName: String,
    val receiverUsername: String,
    val status: String
)

fun renderFriendsPage(
    friends: List<FriendDisplay>,
    incomingRequests: List<FriendRequestDisplay>,
    outgoingRequests: List<FriendRequestDisplay>,
    myTemplates: List<FriendTemplateChoice>,
    receivedTemplates: List<ReceivedTemplateShareDisplay>,
    sentTemplates: List<SentTemplateShareDisplay>,
    message: String? = null,
    error: String? = null
): String {

    var messageHtml = ""

    if (message == "sent") {
        messageHtml = "<p class='success'>Friend request sent.</p>"
    }

    if (message == "accepted") {
        messageHtml = "<p class='success'>Friend request accepted.</p>"
    }

    if (message == "declined") {
        messageHtml = "<p class='success'>Friend request declined.</p>"
    }

    if (message == "cancelled") {
        messageHtml = "<p class='success'>Friend request cancelled.</p>"
    }

    if (message == "removed") {
        messageHtml = "<p class='success'>Friend removed.</p>"
    }

    if (message == "template_sent") {
        messageHtml = "<p class='success'>Workout template sent.</p>"
    }

    if (message == "template_saved") {
        messageHtml = "<p class='success'>Workout template saved to your templates.</p>"
    }

    if (message == "template_declined") {
        messageHtml = "<p class='success'>Workout template declined.</p>"
    }

    if (message == "template_cancelled") {
        messageHtml = "<p class='success'>Sent workout template cancelled.</p>"
    }

    if (error == "empty") {
        messageHtml = "<p class='error'>Please enter a username or email.</p>"
    }

    if (error == "notfound") {
        messageHtml = "<p class='error'>No user found with that username or email.</p>"
    }

    if (error == "self") {
        messageHtml = "<p class='error'>You cannot add yourself as a friend.</p>"
    }

    if (error == "exists") {
        messageHtml = "<p class='error'>A friend request or friendship already exists with this user.</p>"
    }

    if (error == "template_data") {
        messageHtml = "<p class='error'>Please choose a friend and a workout template.</p>"
    }

    if (error == "notfriend") {
        messageHtml = "<p class='error'>You can only send templates to accepted friends.</p>"
    }

    if (error == "template_notfound") {
        messageHtml = "<p class='error'>Workout template could not be found.</p>"
    }

    if (error == "templateexists") {
        messageHtml = "<p class='error'>This template has already been sent to that friend.</p>"
    }

    var friendsHtml = ""

    if (friends.isEmpty()) {
        friendsHtml = "<p class='muted'>You have no friends added yet.</p>"
    } else {
        for (friend in friends) {
            val fullName = escapeFriendHtml(friend.fullName)
            val username = escapeFriendHtml(friend.username)
            val email = escapeFriendHtml(friend.email)

            friendsHtml += """
                <div class="friend-row">
                    <div>
                        <strong>$fullName</strong>
                        <p class="muted">@$username</p>
                        <p class="muted">$email</p>
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
            val fullName = escapeFriendHtml(request.fullName)
            val username = escapeFriendHtml(request.username)
            val email = escapeFriendHtml(request.email)

            incomingHtml += """
                <div class="friend-row">
                    <div>
                        <strong>$fullName</strong>
                        <p class="muted">@$username</p>
                        <p class="muted">$email</p>
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
            val fullName = escapeFriendHtml(request.fullName)
            val username = escapeFriendHtml(request.username)
            val email = escapeFriendHtml(request.email)

            outgoingHtml += """
                <div class="friend-row">
                    <div>
                        <strong>$fullName</strong>
                        <p class="muted">@$username</p>
                        <p class="muted">$email</p>
                    </div>

                    <form method="post" action="/friends/cancel/${request.friendshipId}">
                        <button class="btn-danger btn-small" type="submit">Cancel</button>
                    </form>
                </div>
            """
        }
    }

    var friendOptions = ""

    if (friends.isEmpty()) {
        friendOptions = "<option value=''>No friends available</option>"
    } else {
        for (friend in friends) {
            val fullName = escapeFriendHtml(friend.fullName)
            val username = escapeFriendHtml(friend.username)

            friendOptions += """
                <option value="${friend.userId}">$fullName (@$username)</option>
            """
        }
    }

    var templateOptions = ""

    if (myTemplates.isEmpty()) {
        templateOptions = "<option value=''>No templates available</option>"
    } else {
        for (template in myTemplates) {
            val name = escapeFriendHtml(template.name)

            templateOptions += """
                <option value="${template.templateId}">$name (${template.exerciseCount} exercises)</option>
            """
        }
    }

    val sendButton = if (friends.isEmpty() || myTemplates.isEmpty()) {
        "<button type='submit' disabled>Send Template</button>"
    } else {
        "<button type='submit'>Send Template</button>"
    }

    var receivedHtml = ""

    if (receivedTemplates.isEmpty()) {
        receivedHtml = "<p class='muted'>No workout templates received.</p>"
    } else {
        for (template in receivedTemplates) {
            val templateName = escapeFriendHtml(template.templateName)
            val senderName = escapeFriendHtml(template.senderName)
            val senderUsername = escapeFriendHtml(template.senderUsername)

            receivedHtml += """
                <div class="friend-row">
                    <div>
                        <strong>$templateName</strong>
                        <p class="muted">From $senderName (@$senderUsername)</p>
                        <p class="muted">${template.exerciseCount} exercises</p>
                    </div>

                    <div class="friend-actions">
                        <form method="post" action="/friends/templates/save/${template.shareId}">
                            <button class="btn-small" type="submit">Save</button>
                        </form>

                        <form method="post" action="/friends/templates/decline/${template.shareId}">
                            <button class="btn-danger btn-small" type="submit">Decline</button>
                        </form>
                    </div>
                </div>
            """
        }
    }

    var sentTemplatesHtml = ""

    if (sentTemplates.isEmpty()) {
        sentTemplatesHtml = "<p class='muted'>No templates sent yet.</p>"
    } else {
        for (template in sentTemplates) {
            val templateName = escapeFriendHtml(template.templateName)
            val receiverName = escapeFriendHtml(template.receiverName)
            val receiverUsername = escapeFriendHtml(template.receiverUsername)
            val status = escapeFriendHtml(template.status)

            var actionHtml = ""

            if (template.status == "Pending") {
                actionHtml = """
                    <form method="post" action="/friends/templates/cancel/${template.shareId}">
                        <button class="btn-danger btn-small" type="submit">Cancel</button>
                    </form>
                """
            } else {
                actionHtml = "<p class='success' style='margin:0;'>Saved</p>"
            }

            sentTemplatesHtml += """
                <div class="friend-row">
                    <div>
                        <strong>$templateName</strong>
                        <p class="muted">To $receiverName (@$receiverUsername)</p>
                        <p class="muted">$status</p>
                    </div>

                    <div class="friend-actions">
                        $actionHtml
                    </div>
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
                        Add friends, manage requests, and share workout templates with each other.
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

                    <br>

                    <div class="mini-card">
                        <h3>Send Workout Template</h3>
                        <p class="muted">
                            Choose one of your templates and send a copy to a friend. They can save it into their own templates.
                        </p>

                        <form method="post" action="/friends/templates/send">
                            <label>Friend</label><br>
                            <select name="friendUserId">
                                $friendOptions
                            </select>

                            <br><br>

                            <label>Workout Template</label><br>
                            <select name="templateId">
                                $templateOptions
                            </select>

                            <br><br>

                            $sendButton
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

                    <h2>Workout Template Sharing</h2>

                    <div class="friends-grid">

                        <div class="mini-card">
                            <h3>Received Templates</h3>
                            $receivedHtml
                        </div>

                        <div class="mini-card">
                            <h3>Sent Templates</h3>
                            $sentTemplatesHtml
                        </div>

                        <div class="mini-card">
                            <h3>Tip</h3>
                            <p class="muted">
                                When you save a friend's template, it becomes your own copy. You can then open it from the Templates page and log it normally.
                            </p>
                            <a class="btn" href="/templates">Go to Templates</a>
                        </div>

                    </div>

                </div>
            </main>
        </body>
        </html>
    """.trimIndent()
}

fun escapeFriendHtml(text: String): String {
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#x27;")
}
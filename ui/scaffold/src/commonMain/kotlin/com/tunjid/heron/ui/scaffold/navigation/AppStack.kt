package com.tunjid.heron.ui.scaffold.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.routeOf
import heron.ui.scaffold.generated.resources.Res
import heron.ui.scaffold.generated.resources.auth
import heron.ui.scaffold.generated.resources.home
import heron.ui.scaffold.generated.resources.messages
import heron.ui.scaffold.generated.resources.notifications
import heron.ui.scaffold.generated.resources.search
import heron.ui.scaffold.generated.resources.splash
import org.jetbrains.compose.resources.StringResource

enum class AppStack(
    val stackName: String,
    val titleRes: StringResource,
    val icon: ImageVector,
    val rootRoute: Route,
) {
    Home(
        stackName = "home-stack",
        titleRes = Res.string.home,
        icon = Icons.Rounded.Home,
        rootRoute = routeOf("/home"),
    ),
    Search(
        stackName = "search-stack",
        titleRes = Res.string.search,
        icon = Icons.Rounded.Search,
        rootRoute = routeOf("/search"),
    ),
    Messages(
        stackName = "messages-stack",
        titleRes = Res.string.messages,
        icon = Icons.Rounded.Mail,
        rootRoute = routeOf("/messages"),
    ),
    Notifications(
        stackName = "notifications-stack",
        titleRes = Res.string.notifications,
        icon = Icons.Rounded.Notifications,
        rootRoute = routeOf("/notifications"),
    ),
    Auth(
        stackName = "auth-stack",
        titleRes = Res.string.auth,
        icon = Icons.Rounded.Lock,
        rootRoute = routeOf("/auth"),
    ),
    Splash(
        stackName = "splash-stack",
        titleRes = Res.string.splash,
        icon = Icons.Rounded.Circle,
        rootRoute = routeOf("/splash"),
    ),
}

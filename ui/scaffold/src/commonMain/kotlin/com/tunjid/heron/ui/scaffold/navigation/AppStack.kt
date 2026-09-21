package com.tunjid.heron.ui.scaffold.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.tunjid.heron.ui.icons.HeronIcons
import com.tunjid.heron.ui.icons.regular.Circle
import com.tunjid.heron.ui.icons.regular.Home
import com.tunjid.heron.ui.icons.regular.Lock
import com.tunjid.heron.ui.icons.regular.Mail
import com.tunjid.heron.ui.icons.regular.Notifications
import com.tunjid.heron.ui.icons.regular.Search
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
        icon = HeronIcons.Regular.Home,
        rootRoute = routeOf("/home"),
    ),
    Search(
        stackName = "search-stack",
        titleRes = Res.string.search,
        icon = HeronIcons.Regular.Search,
        rootRoute = routeOf("/search"),
    ),
    Messages(
        stackName = "messages-stack",
        titleRes = Res.string.messages,
        icon = HeronIcons.Regular.Mail,
        rootRoute = routeOf("/messages"),
    ),
    Notifications(
        stackName = "notifications-stack",
        titleRes = Res.string.notifications,
        icon = HeronIcons.Regular.Notifications,
        rootRoute = routeOf("/notifications"),
    ),
    Auth(
        stackName = "auth-stack",
        titleRes = Res.string.auth,
        icon = HeronIcons.Regular.Lock,
        rootRoute = routeOf("/auth"),
    ),
    Splash(
        stackName = "splash-stack",
        titleRes = Res.string.splash,
        icon = HeronIcons.Regular.Circle,
        rootRoute = routeOf("/splash"),
    ),
}

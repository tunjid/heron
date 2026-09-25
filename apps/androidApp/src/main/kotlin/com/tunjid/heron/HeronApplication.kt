/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.heron

import android.app.Application
import com.tunjid.heron.data.tasks.AndroidBackgroundTaskHost
import com.tunjid.heron.data.tasks.BackgroundTaskDescriptor
import com.tunjid.heron.data.tasks.BackgroundTaskRunner
import com.tunjid.heron.data.tasks.BackgroundTaskScheduler
import com.tunjid.heron.ui.scaffold.R as ScaffoldR
import com.tunjid.heron.ui.scaffold.scaffold.AppState
import kotlinx.coroutines.CoroutineScope

class HeronApplication :
    Application(),
    AndroidBackgroundTaskHost {

    // This needs to be lateinit instead of lazy to ensure it is
    // instantiated on the main thread
    lateinit var appState: AppState

    override val processScope: CoroutineScope
        get() = appState.processScope

    override val backgroundTaskScheduler: BackgroundTaskScheduler
        get() = appState.backgroundTaskScheduler

    override val backgroundTaskRunner: BackgroundTaskRunner
        get() = appState.backgroundTaskRunner

    override val backgroundTaskDescriptor: BackgroundTaskDescriptor
        get() = appState.backgroundTaskDescriptor

    override val notificationIcon: Int
        get() = ScaffoldR.drawable.ic_heron_notification

    override fun onCreate() {
        super.onCreate()
        appState = createAppState(this)
    }
}

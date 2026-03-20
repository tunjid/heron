package com.tunjid.heron.media.video.linux

import org.freedesktop.gstreamer.Gst
import org.freedesktop.gstreamer.Version

internal object GStreamer {
    val initialized by lazy {
        Gst.init(Version.BASELINE, "KMPVideoPlayer")
        true
    }
}

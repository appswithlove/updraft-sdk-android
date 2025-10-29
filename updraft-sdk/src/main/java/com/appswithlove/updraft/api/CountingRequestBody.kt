package com.appswithlove.updraft.api

import android.util.Log
import com.appswithlove.updraft.BuildConfig
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.Sink
import okio.buffer
import java.io.IOException

class CountingRequestBody(
    private val delegate: RequestBody,
    private val listener: Listener
) : RequestBody() {

    override fun contentType(): MediaType? = delegate.contentType()

    override fun contentLength(): Long {
        return try {
            delegate.contentLength()
        } catch (e: IOException) {
            e.printStackTrace()
            -1
        }
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        if (BuildConfig.DEBUG) {
            Log.d("CountingRequestBody", "writeTo called")
        }

        val countingSink = CountingSink(sink)
        val bufferedSink = countingSink.buffer()

        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }

    private inner class CountingSink(delegate: Sink) : ForwardingSink(delegate) {
        private var bytesWritten = 0L

        @Throws(IOException::class)
        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            bytesWritten += byteCount
            listener.onRequestProgress(bytesWritten, contentLength())
            if (BuildConfig.DEBUG) {
                Log.d(
                    "CountingRequestBody",
                    "bytes written: $bytesWritten content length: ${contentLength()}"
                )
            }
        }
    }

    fun interface Listener {
        fun onRequestProgress(bytesWritten: Long, contentLength: Long)
    }
}

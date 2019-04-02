package com.appswithlove.updraft.api;

import android.support.annotation.NonNull;
import android.util.Log;
import com.appswithlove.updraft.BuildConfig;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

import java.io.IOException;

/**
 * @author Leo Nikkilä
 * with modifications made by Paulina Sadowska
 */
public class CountingRequestBody extends RequestBody {

    private final RequestBody delegate;
    private final Listener listener;

    public CountingRequestBody(RequestBody delegate, Listener listener) {
        this.delegate = delegate;
        this.listener = listener;
    }

    @Override
    public MediaType contentType() {
        return delegate.contentType();
    }

    @Override
    public long contentLength() {
        try {
            return delegate.contentLength();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public void writeTo(@NonNull BufferedSink sink) throws IOException {
        if (BuildConfig.DEBUG) {
            Log.d("CountringRequestBody", "writeTo called");
        }
        CountingSink countingSink = new CountingSink(sink);
        BufferedSink bufferedSink = Okio.buffer(countingSink);

        delegate.writeTo(bufferedSink);

        bufferedSink.flush();
    }

    final class CountingSink extends ForwardingSink {

        private long bytesWritten = 0;

        CountingSink(Sink delegate) {
            super(delegate);
        }

        @Override
        public void write(@NonNull Buffer source, long byteCount) throws IOException {
            super.write(source, byteCount);
            bytesWritten += byteCount;
            listener.onRequestProgress(bytesWritten, contentLength());
            if (BuildConfig.DEBUG) {
                Log.d("CountingRequestBody", "bytes written: " + bytesWritten + " content length: " +  contentLength());
            }
        }

    }

    public interface Listener {
        void onRequestProgress(long bytesWritten, long contentLength);
    }

}
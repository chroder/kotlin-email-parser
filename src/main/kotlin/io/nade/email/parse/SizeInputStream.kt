package io.nade.email.parse

import java.io.InputStream

/**
 * Wraps an input stream that counts the bytes read.
 * When the stream is fully read, the bytesRead property will
 * essentially be the "file size" of the stream.
 */
class SizeInputStream(private val istream: InputStream) : InputStream() {
    /**
     * The number of bytes read so far.
     */
    var bytesRead: Int = 0
        private set

    override fun read(): Int {
        val b = istream.read()
        if (b == -1) {
            return -1
        }

        bytesRead += 1
        return b
    }

    override fun read(b: ByteArray?): Int {
        val c = istream.read(b)
        bytesRead += c
        return c
    }

    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        val c = istream.read(b, off, len)
        bytesRead += c
        return c
    }

    override fun skip(n: Long): Long {
        return istream.skip(n)
    }

    override fun available(): Int {
        return istream.available()
    }

    override fun reset() {
        istream.reset()
    }

    override fun close() {
        istream.close()
    }

    override fun mark(readlimit: Int) {
        istream.mark(readlimit)
    }

    override fun markSupported(): Boolean {
        return istream.markSupported()
    }
}
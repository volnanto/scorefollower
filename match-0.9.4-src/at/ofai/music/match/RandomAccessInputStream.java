/*
 * Decompiled with CFR 0_114.
 */
package at.ofai.music.match;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class RandomAccessInputStream
extends InputStream {
    protected RandomAccessFile r;
    protected long markPosition = 0;

    public RandomAccessInputStream(String name) throws FileNotFoundException {
        this.r = new RandomAccessFile(name, "r");
    }

    public RandomAccessInputStream(File f) throws FileNotFoundException {
        this.r = new RandomAccessFile(f, "r");
    }

    public int available() throws IOException {
        long availableBytes = this.r.length() - this.r.getFilePointer();
        if (availableBytes > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int)availableBytes;
    }

    public void close() throws IOException {
        this.r.close();
    }

    public void mark(int readlimit) {
        try {
            this.mark();
        }
        catch (IOException e) {
            e.printStackTrace();
            this.markPosition = -1;
        }
    }

    public void mark() throws IOException {
        this.markPosition = this.r.getFilePointer();
    }

    public boolean markSupported() {
        return true;
    }

    public int read() throws IOException {
        return this.r.read();
    }

    public int read(byte[] b) throws IOException {
        return this.r.read(b);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return this.r.read(b, off, len);
    }

    public void reset() throws IOException {
        if (this.markPosition < 0) {
            throw new IOException("reset(): invalid mark position");
        }
        this.r.seek(this.markPosition);
    }

    public long skip(long n) throws IOException {
        long pos = this.r.getFilePointer();
        this.r.seek(n + pos);
        return this.r.getFilePointer() - pos;
    }

    public long seekFromMark(long n) throws IOException {
        this.r.seek(this.markPosition + n);
        return this.r.getFilePointer() - this.markPosition;
    }
}


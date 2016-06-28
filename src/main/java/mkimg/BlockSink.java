package mkimg;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class BlockSink extends OutputStream {

    ByteBuffer buf = ByteBuffer.allocate(1024 * 1024);
//    ByteBuffer buf = ByteBuffer.allocateDirect(1024 * 1024);
    public long nStage = 0;
    public long nExtent = 0;
    long nLeft = 0;
    long nWasted = 0;
    public int blockSize = 2048;
    private OutputStream out = null;

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }

    public BlockSink() {

    }

    public ByteBuffer getBuffer() {
        buf.clear();
        return buf;
    }

    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (out != null) {
            out.write(b, off, len);
        }
        final long x = this.nLeft + len;
        this.nExtent += (x / this.blockSize);
        this.nLeft = (x % this.blockSize);
    }

    @Override
    public void write(byte[] b) throws IOException {
        this.write(b, 0, b.length);
    }

    @Override
    public void write(int b) throws IOException {
        if (out != null) {
            out.write(b);
        }
        final long x = this.nLeft + 1;
        this.nExtent += (x / this.blockSize);
        this.nLeft = (x % this.blockSize);
    }

    public void writep(byte[] b, int off, int len) throws IOException {
        if (len > 0) {
            assert (this.nLeft == 0);
            if (out != null) {
                out.write(b, off, len);
            }
            this.nExtent += (len / this.blockSize);
            this.nLeft = (len % this.blockSize);
        }
        writep();
    }

    public void writep() throws IOException {
        if (this.nLeft > 0) {
            byte[] bf = new byte[this.blockSize - (int) this.nLeft];
            Arrays.fill(bf, (byte) 0);
            if (out != null) {
                out.write(bf);
            }
            this.nWasted += this.nLeft;
            this.nExtent++;
            this.nLeft = 0;
        }
    }

    public void padBlocks(long n) throws IOException {
        assert (this.nLeft == 0);
        assert (n > 0);
        byte[] b = new byte[this.blockSize];
        Arrays.fill(b, (byte) 0);
        while (n-- > 0) {
            this.write(b, 0, b.length);
            this.nWasted += this.blockSize;
        }
    }

    public void padUpTo(long lba) throws IOException {
        assert (this.nLeft == 0);
        assert (this.nExtent <= lba);
        byte[] b = new byte[this.blockSize];
        Arrays.fill(b, (byte) 0);
        while (this.nExtent < lba) {
            this.write(b, 0, b.length);
            this.nWasted += this.blockSize;
        }
    }

    public long calcBlocks(long size) {
        assert (size >= 0);
        return (((size) / this.blockSize) + ((size % this.blockSize) == 0 ? 0 : 1));
    }

    public void reset() {
        this.nExtent = 0;
        this.nLeft = 0;
        this.nWasted = 0;
    }
}

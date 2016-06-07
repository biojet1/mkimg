package udf;

import java.nio.ByteBuffer;
import java.time.OffsetDateTime;

public class UDFWrite {

    public static void descriptorTag(ByteBuffer b, short tid, long lba, int len) {
        // Uint16 TagIdentifier;
        b.putShort(0, tid);
        b.putShort(2, (short) 2); //  Uint16 DescriptorVersion;
        b.put(4, (byte) 0); //   Uint8 TagChecksum;
        b.put(5, (byte) 0); //  byte Reserved;
        b.putShort(6, (short) 0); //  Uint16 TagSerialNumber;
        b.putShort(8, (short) CrcCCITT.calculate(b.array(), 16, b.position() - 16)); // Uint16 DescriptorCRC;
        b.putShort(10, (short) (b.position() - 16)); //  Uint16 DescriptorCRCLength;
        b.putInt(12, (int) lba); //  Uint32 TagLocation;
        int chksum = 0;
        byte[] buf = b.array();
        for (int i = 0; i < 4; i++) {
            chksum += buf[i] & 255;
        }
        for (int i = 5; i < 16; i++) {
            chksum += buf[i] & 255;
        }
        buf[4] = (byte) chksum;
    }
    final static byte[] OCU = new byte[]{'O', 'S', 'T', 'A', ' ', 'C', 'o', 'm', 'p', 'r', 'e', 's', 's', 'e', 'd', ' ', 'U', 'n', 'i', 'c', 'o', 'd', 'e'};

    public static void putCharSpecOSTACompressedUnicode(ByteBuffer b) {
        int n = 64 - (OCU.length + 1);
        b.put((byte) 0);
        b.put(OCU);
        while (n-- > 0) {
            b.put((byte) 0);
        }
    }

    public static int putDString(ByteBuffer b, String s, final int len) {
        int i;
        int j;
        final int l = s.length();
        final int n = len - 1;
        i = 0;
        j = 0;
        b.mark();
        b.put((byte) 8);
        while ((i < n) && (j < l)) {
            char c = s.charAt(j++);
            if (c > 255) {
                i = 0;
                j = 0;
                b.reset();
                b.put((byte) 16);
                while ((i < n) && (j < l)) {
                    b.put((byte) ((s.charAt(j++) & 65280) >> 8));
                    if (i < n) {
                        b.put((byte) ((s.charAt(j++) & 255)));
                    }
                }
                break;
            }
            b.put((byte) c);
        }
        j = i;
        while (i++ < n) {
            b.put((byte) 0);
        }
        b.put((byte) j);
        return i;
    }

    public static void extentAd(ByteBuffer b, long pos, long len) {
        b.putInt((int) len);
        b.putInt((int) pos);
    }

    public static void putTimestamp(ByteBuffer b, OffsetDateTime time) {
        b.putShort((short) ((1 << 12) | (time.getOffset().getTotalSeconds() / 60))); // Uint16 TypeAndTimezone
        b.putShort((short) time.getYear()); // Uint16 TypeAndTimezone
        b.put((byte) time.getMonthValue()); // Uint8 Month
        b.put((byte) time.getDayOfMonth()); // Uint8 Day;
        b.put((byte) time.getHour()); // Uint8 Hour;
        b.put((byte) time.getMinute()); // Uint8 Minute;
        b.put((byte) time.getSecond()); // Uint8 Second;
        b.put((byte) 0); // Uint8 Centiseconds;
        b.put((byte) 0); // Uint8 HundredsofMicroseconds;
        b.put((byte) 0); // Uint8 Microseconds;
    }

    public static void entityId(ByteBuffer b, String id) {
        putEntityId(b, id.getBytes(), new byte[]{});
    }

    public static void putEntityId(ByteBuffer b, byte[] id, byte[] suffix) {
        b.put((byte) 0);
        int n = id.length;
        if (n < 23) {
            if (n > 0) {
                b.put(id, 0, n);
            }
            while (n++ < 23) {
                b.put((byte) 0);
            }
        } else {
            b.put(id, 0, 23);
        }
        n = suffix.length;
        if (n < 8) {
            if (n > 0) {
                b.put(suffix, 0, n);
            }
            while (n++ < 8) {
                b.put((byte) 0);
            }
        } else {
            b.put(suffix, 0, 8);
        }
    }

}
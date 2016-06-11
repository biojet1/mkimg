package udf;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.OffsetDateTime;
import mkimg.BlockSink;
import mkimg.INodeEntry;
import static udf.UDFWrite.UDF_FILEENTRY_PERMISSION_GR;
import static udf.UDFWrite.UDF_FILEENTRY_PERMISSION_GX;
import static udf.UDFWrite.UDF_FILEENTRY_PERMISSION_OR;
import static udf.UDFWrite.UDF_FILEENTRY_PERMISSION_OX;
import static udf.UDFWrite.UDF_FILEENTRY_PERMISSION_UR;
import static udf.UDFWrite.UDF_FILEENTRY_PERMISSION_UX;
import static udf.UDFWrite.UDF_ICBTAG_FILETYPE_BYTESEQ;
import static udf.UDFWrite.UDF_ICBTAG_FILETYPE_DIRECTORY;
import static udf.UDFWrite.UDF_ICBTAG_FLAG_ARCHIVE;

public class UDFWriteFile {

    public static void fileEntry(final BlockSink out, final INodeEntry ino, final long uniqId, final byte[] imId, long rbaData, long rbaEntry) throws IOException {
        long length = ino.size;
        final ByteBuffer b = out.getBuffer();
        // struct FileEntry { // ECMA 167 4/14.9 
        // struct tag DescriptorTag;
//        b.position(b.position() + 16);
        b.clear().limit(out.blockSize).position(16);
        //struct icbtag ICBTag;
        {
            // Prior Recorded Number of Direct Entries Uint32
            b.putInt(0);
            // Strategy Type Uint16
            b.putShort((short) 4);
            // Strategy Parameter bytes
            b.putShort((short) 0);
            // Maximum Number of Entries Uint16
            b.putShort((short) 1);
            // Reserved #00 byte
            b.put((byte) 0);
            // File Type Uint8
            b.put(ino.isDirectory() ? UDF_ICBTAG_FILETYPE_DIRECTORY : UDF_ICBTAG_FILETYPE_BYTESEQ);
            // Parent ICB Location lb_addr
            b.putInt(0).putShort((short) 0);
            // Flags Uint16
            b.putShort(UDF_ICBTAG_FLAG_ARCHIVE);
        }
        //Uint32 Uid;
        b.putInt(-1);
        //Uint32 Gid;
        b.putInt(-1);
        //Uint32 Permissions;
        b.putInt(ino.isDirectory() ? (UDF_FILEENTRY_PERMISSION_OR | UDF_FILEENTRY_PERMISSION_GR | UDF_FILEENTRY_PERMISSION_UR
                | UDF_FILEENTRY_PERMISSION_OX | UDF_FILEENTRY_PERMISSION_GX | UDF_FILEENTRY_PERMISSION_UX)
                : (UDF_FILEENTRY_PERMISSION_OR | UDF_FILEENTRY_PERMISSION_GR | UDF_FILEENTRY_PERMISSION_UR));
        //Uint16 FileLinkCount;
        b.putShort((short) ino.nlink);
        {
            // UDF 1.02 2.3.6.1 Shall be set to ZERO.
            //Uint8 RecordFormat;
            b.put((byte) 0);
            //Uint8 RecordDisplayAttributes;
            b.put((byte) 0);
            //Uint32 RecordLength;
            b.putInt(0);
        }
        //Uint64 InformationLength;
        b.putLong(length);
        //Uint64 LogicalBlocksRecorded;
        b.putLong(out.calcBlocks(length));
//struct timestamp AccessTime;
        UDFWrite.putTimestamp(b, OffsetDateTime.MIN);
//struct timestamp ModificationTime;
//struct timestamp AttributeTime;
        //Uint32 Checkpoint; Monotonic increasing numeric tag.
        b.putInt(1);
        //struct long_ad ExtendedAttributeICB;
        b.putLong(0).putLong(0).putLong(0).putLong(0);
        //struct EntityID ImplementationIdentifier;
        assert (imId.length == 32);
        b.put(imId);
        //Uint64 UniqueID,
        b.putLong(uniqId);
        //Uint32 LengthofExtendedAttributes;
        b.putInt(0);
        //Uint32 LengthofAllocationDescriptors; length, in bytes, of the Allocation Descriptors field
        b.putInt(0); // later
        //byte ExtendedAttributes[];
        //byte AllocationDescriptors[];
        int chunk;
        int ad = b.position();
        for (; length > 0; length -= chunk) {
            chunk = (length > 0x3ffff800) ? 0x3ffff800 : (int) (length);
            b.putInt(chunk).putInt((int) rbaData);
            rbaData += chunk >> 11;
        }
        b.putInt(212, b.position() - ad);
        // NOTE: UDF DVD Video Compat: files to be less than or equal to 2**30 - 1 Sector (0x40000000-0x800 = 0x3ffff800) bytes in length.
        // TODO: 0x3ffff800 is for 2048 bytes block
        // Write
        out.writePadded(UDFWrite.descriptorTag(b, (short) 261, rbaEntry, b.position()).array(), 0, b.position());
    }
}

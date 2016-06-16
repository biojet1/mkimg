package udf;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Arrays;
import mkimg.BlockSink;
import mkimg.Inode;
import mkimg.Node;
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

    public static void fileEntry(final BlockSink out, final Inode ino, final long uniqId, final byte[] imId, long rbaEntry, long rbaData) throws IOException {
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
            // Strategy Parameter bytes byte[2];
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
        UDFWrite.putTimestamp(b, OffsetDateTime.now().minusHours(1));
//struct timestamp ModificationTime;
        UDFWrite.putTimestamp(b, OffsetDateTime.now().minusHours(1));
//struct timestamp AttributeTime;
        UDFWrite.putTimestamp(b, OffsetDateTime.now().minusHours(1));
        //Uint32 Checkpoint; Monotonic increasing numeric tag.
        b.putInt(1);
        //struct long_ad ExtendedAttributeICB;
        b.putInt(0).putInt(0).putInt(0).putInt(0);
        //struct EntityID ImplementationIdentifier;
        assert (imId.length == 32);
        b.put(imId);
        //Uint64 UniqueID,
        b.putLong(uniqId);
        //Uint32 LengthofExtendedAttributes;
        b.putInt(0);
        //Uint32 LengthofAllocationDescriptors; length, in bytes, of the Allocation Descriptors field
        System.err.println("LengthofAllocationDescriptors: " + b.position());
        b.putInt(123); // later
        //byte ExtendedAttributes[];
        //byte AllocationDescriptors[];
        int chunk;
        int ad = b.position();
        for (; length > 0; length -= chunk) {
            chunk = (length > 0x3ffff800) ? 0x3ffff800 : (int) (length);
            b.putInt(chunk).putInt((int) rbaData);
            rbaData += chunk >> 11;
        }
        assert (b.getInt(172) == 123);
//        assert (b.getInt(212) == 123);
        b.putInt(172, b.position() - ad);
        // NOTE: UDF DVD Video Compat: files to be less than or equal to 2**30 - 1 Sector (0x40000000-0x800 = 0x3ffff800) bytes in length.
        // TODO: 0x3ffff800 is for 2048 bytes block
        // Write
        out.writep(UDFWrite.descriptorTag(b, (short) 261, rbaEntry, b.position()).array(), 0, b.position());
    }

    public static int fileItem(final BlockSink out, final  Node<Inode> cur, String name, long rbaEntry, long rbaData) throws IOException {
        Inode ino = cur.getData();
        short len_impl_use = 0;
        int len_file_id = 0;
        //struct FileIdentifierDescriptor { // ISO 13346 4/14.4 
        final ByteBuffer b = out.getBuffer();
        //struct tag DescriptorTag;
        b.clear().limit(out.blockSize).position(16);
        //Uint16 FileVersionNumber; // UDF-102: 2.3.4.1 Shall be set to 1
        b.putShort((short) 1);
        //Uint8 FileCharacteristics; // ECMA-167: 14.4.3  File Characteristics
        b.put((byte) ((ino.isHidden() ? UDFWrite.UDF_FILE_CHARACTERISTIC_HIDDEN : 0)
                | (ino.isDirectory() ? (UDFWrite.UDF_FILE_CHARACTERISTIC_DIRECTORY
                        | (name == null ? UDFWrite.UDF_FILE_CHARACTERISTIC_PARENT : 0)) : 0))
        );
        //Uint8 LengthofFileIdentifier;
        b.put((byte) 0);// later
        //struct long_ad ICB; // ECMA-167: 14.4.5 This field shall specify the address of an ICB describing the file
        {
            //Uint32 ExtentLength;
            b.putInt(out.blockSize);// out.blockSize == sizeof(inode)
            //struct Lb_addr ExtentLocation;
            {
                //Uint32 LogicalBlockNumber;
                b.putInt((int) rbaData);
                //Uint16 PartitionReferenceNumber;
                b.putShort((short) 0);
            }
            //struct ADImpUse ImplementationUse;
            {
                //Uint16 flags;
                b.putShort((short) 0);
                //byte impUse[4];
//                b.putInt(0);
                b.putInt((int)ino.auxA);
            }
        }
        //Uint16 LengthofImplementationUse;
        b.putShort(len_impl_use);
        //byte ImplementationUse[??];
        if (len_impl_use > 0) {
            UDFWrite.zfill(b, len_impl_use);
        }
        //char FileIdentifier[??];
        if (name != null && !name.isEmpty()) {
            int i = b.position();
            UDFWrite.putDString(b, name, 255, true);
            b.put(19, (byte) (b.position() - i));
        }
        //byte Padding[??];
//        System.err.printf("pad %s %d %d\n", name, b.position(), (b.position() & (4 - 1)));
        while ((b.position() & (4 - 1)) != 0) {
            b.put((byte) 0);
        }
        // Write
        int length = b.position();
        out.write(UDFWrite.descriptorTag(b, (short) 257, rbaEntry, length).array(), 0, length);
        assert (length > 38);
        assert (length <= out.blockSize);
        return length;
    }

    public static void fileData(final BlockSink out, final Inode ino, byte[] buf, final long size, MessageDigest md, boolean interActive) throws IOException {
        InputStream in;
        if (interActive) {
            RETRY:
            for (;;) {
                try {
                    in = ino.getInputStream();
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                    for (;;) {
                        System.err.println("[A]bort [R]etry:");
                        switch (System.in.read()) {
                            case 'A':
                            case 'a':
                            case -1:
                                ex.printStackTrace(System.err);
                                System.exit(1);
                                return;
                            case 'R':
                            case 'r':
                                continue RETRY;
                        }
                    }
                }
                break;
            }
        } else {
            in = ino.getInputStream();
        }
        // Read/Write
        int nSize;
        long nRemain = size;
        while (nRemain > 0) {
            nSize = in.read(buf);
            // Cut ?
            if (nRemain < nSize) {
                nSize = (int) nRemain;
            }
            // Write
            out.write(buf, 0, nSize);
            nRemain -= nSize;
            // Calc MD
            if (md != null) {
                md.update(buf, 0, nSize);
            }
        }
        // Pad?
        if (nRemain != 0) {
            assert (nRemain > 0);
        }
        // Pad to block
        out.writep();
        // Check MD:
        MD:
        if (md != null) {
            byte[] d = md.digest();
            if (ino.hash == null) {
                ino.hash = d;
            } else if (!Arrays.equals(d, ino.hash)) {
                System.err.println("Digest mismatched!");
                while (interActive) {
                    System.err.println("[A]bort [C]ontinue:");
                    switch (System.in.read()) {
                        case 'A':
                        case 'a':
                        case -1:
                            System.exit(1);
                            return;
                        case 'C':
                        case 'c':
                            ino.hash = d;
                            break MD;
                    }
                }
            }
        }
    }
}

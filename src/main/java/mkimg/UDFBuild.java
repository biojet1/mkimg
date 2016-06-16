package mkimg;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import udf.DataDesc;
import udf.UDFWrite;
import udf.UDFWriteFile;
import static udf.UDFWriteFile.fileData;
import static udf.UDFWriteFile.fileItem;

public class UDFBuild {

    private OffsetDateTime logicalVolumeIntegrityDescTime;
    private OffsetDateTime recordingDateandTime;
    private long lbaIntegritySequence;
    private long nLastUniqueID;
    private long partionSize;
    private int nFiles;
    private int nDirectories;
    private String logicalVolumeIdentifier;
    private byte[] applicationIdentifier;
    private String volumeSetIdentifier;
    private String fileSetIdentifier;
    private long lbaMainVolumeDesc;
    private long lbaUDFPartitionStart;
    byte[] ENTITYID_OSTA_COMPLIANT = new byte[]{'\00', '*', 'O', 'S', 'T', 'A', ' ', 'U', 'D', 'F', ' ', 'C', 'o', 'm', 'p', 'l', 'i', 'a', 'n', 't', '\00', '\00', '\00', '\00', '\02', '\01', '\03', '\00', '\00', '\00', '\00', '\00'};
    private long lbaReserveVolumeDesc;
    private long totalSectors;
    private long lbaRootDirectoryStart;

    public UDFBuild() {
    }

    static void volumeRecognitionArea(BlockSink out) throws IOException {
        assert ((out.blockSize % 512) == 0);
        assert ((32768 / out.blockSize) == out.nExtent);
        ByteBuffer b = out.getBuffer();
        byte[][] vrid = new byte[][]{{'B', 'E', 'A', '0', '1'}, {'N', 'S', 'R', '0', '2'}, {'T', 'E', 'A', '0', '1'}};
        Arrays.fill(b.array(), 0, 2048, (byte) 0);
        for (int i = 0; i < 3; ++i) {
            b.clear();
            b.put((byte) 0);// StructureType
            b.put(vrid[i]);//StandardIdentifier
            b.put((byte) 1);// StructureVersion
            out.write(b.array(), 0, 2048);
        }
        assert (((32768 / out.blockSize) + ((3 * 2048) / out.blockSize)) == out.nExtent);
    }

    static void terminatingDescriptor(BlockSink out) throws IOException {
        terminatingDescriptor(out, out.nExtent);
    }

    static void terminatingDescriptor(BlockSink out, long lba) throws IOException {
        ByteBuffer b = out.getBuffer();
        UDFWrite.zfill(b, 512);
        out.writep(UDFWrite.descriptorTag(b, (short) 8, lba, 512).array(), 0, b.position());
    }

    static void systemArea(BlockSink out) throws IOException {
        out.padUpTo(32768 / out.blockSize);
    }

    void logicalVolumeIntegrityDesc(BlockSink out) throws IOException {
        if (out.nStatus != 0) {
            this.lbaIntegritySequence = out.nExtent;
        } else {
            assert (this.lbaIntegritySequence == out.nExtent);
        }
        // struct LogicalVolumeIntegrityDesc { // ISO 13346 3/10.10
        ByteBuffer b = out.getBuffer();
        // struct tag DescriptorTag;
        b.position(16);
        // struct timestamp RecordingDateAndTime;
        UDFWrite.putTimestamp(b, this.logicalVolumeIntegrityDescTime);
        // Uint32 IntegrityType  (Close Integrity Descriptor)
        b.putInt(1);
        // extent_ad NextIntegrityExtent;
        b.putInt(0).putInt(0);
        // struct LogicalVolumeIntegrityDescContentsUse LogicalVolumeContentsUse;
        {
            // Uint64 UniqueID;
            b.putLong(this.nLastUniqueID);
            // 	byte reserved[24];
            UDFWrite.zfill(b, 24);
        }
        // Uint32 NumberOfPartitions;
        b.putInt(1);
        // Uint32 LengthOfImplementationUse;
        b.putInt(46);
        // Uint32 FreeSpaceTable;
        b.putInt(0);
        // Uint32 SizeTable;
        b.putInt((int) this.partionSize);
        // struct LogicalVolumeIntegrityDescImplementationUse ImplementationUse;
        {
            // struct EntityID ImplementationID;
            UDFWrite.entityId(b, "mkimg");
            b.putInt(this.nFiles); // Uint32 NumberofFiles;
            b.putInt(this.nDirectories); // Uint32 NumberofDirectories;
            b.putShort((short) 0x102); // Uint16 MinimumUDFReadRevision;
            b.putShort((short) 0x102); // Uint16 MinimumUDFWriteRevision;
            b.putShort((short) 0x102); // Uint16 MaximumUDFWriteRevision;
        }
        // Write
        out.writep(UDFWrite.descriptorTag(b, (short) 9, out.nExtent, b.position()).array(), 0, b.position());
        assert (134 == b.position());
    }

    void volumeDescriptorSequence(BlockSink out) throws IOException {
        if (out.nExtent < 256) {
            if (out.nStatus != 0) {
                this.lbaMainVolumeDesc = out.nExtent;
            } else {
                assert (this.lbaMainVolumeDesc == out.nExtent);
            }
        } else if (out.nExtent > 256) {
            if (out.nStatus != 0) {
                this.lbaReserveVolumeDesc = out.nExtent;
            } else {
                assert (this.lbaReserveVolumeDesc == out.nExtent);
            }
        } else {
            assert (false);
        }
        ByteBuffer b = out.getBuffer();
        // struct PrimaryVolumeDescriptor
        {
            // tag DescriptorTag
            b.clear().limit(512).position(16);
            // Uint32 VolumeDescriptorSequenceNumber;
            b.putInt(0);
            // Uint32 PrimaryVolumeDescriptorNumber;
            b.putInt(0);
            // dstring VolumeIdentifier[32];
            assert (b.position() == 24);
            UDFWrite.putDString(b, this.logicalVolumeIdentifier, 32, false);
            // Uint16 VolumeSequenceNumber;
            assert (b.position() == 56);
            b.putShort((short) 1);
            // Uint16 MaximumVolumeSequenceNumber;
            b.putShort((short) 1);
            // Uint16 InterchangeLevel;
            b.putShort((short) 2);
            // Uint16 MaximumInterchangeLevel;
            b.putShort((short) 2);
            // Uint32 CharacterSetList;
            b.putInt(1);
            // Uint32 MaximumCharacterSetList;
            b.putInt(1);
            // dstring VolumeSetIdentifier[128];
            UDFWrite.putDString(b, this.volumeSetIdentifier, 128, false);
            // struct charspec DescriptorCharacterSet;
            System.err.printf("%dp %dl %dr\n", b.position(), b.limit(), b.remaining());
            assert (b.position() == 200);
            UDFWrite.putCharSpecOSTACompressedUnicode(b);
            // struct charspec ExplanatoryCharacterSet;
            UDFWrite.putCharSpecOSTACompressedUnicode(b);
            // struct extent_ad VolumeAbstract;
            assert (b.position() == 328);
            UDFWrite.extentAd(b, 0, 0);
            // struct extent_ad VolumeCopyrightNotice;
            UDFWrite.extentAd(b, 0, 0);
            // struct EntityID ApplicationIdentifier;
            UDFWrite.zfill(b, 32);
            // struct timestamp RecordingDateandTime;
            UDFWrite.putTimestamp(b, this.recordingDateandTime);
            // struct EntityID ImplementationIdentifier;
            b.put(this.applicationIdentifier);
            // byte ImplementationUse[64];
            UDFWrite.zfill(b, 64);
            //Uint32 PredecessorVolumeDescriptorSequenceLocation;
            b.putInt(0);
            //Uint16 Flags;
            b.putShort((short) 0);
            //byte Reserved[22];
            UDFWrite.zfill(b, 22);
            // Write
            out.writep(UDFWrite.descriptorTag(b, (short) 1, out.nExtent, b.position()).array(), 0, b.position());
            assert (512 == b.position());
        }
        //struct ImpUseVolumeDescriptor 
        {
            //struct tag DescriptorTag;
            b.clear().position(16);
            //Uint32 VolumeDescriptorSequenceNumber;
            b.putInt(1);
            //struct EntityID ImplementationIdentifier;
            UDFWrite.putEntityId(b, 0, new byte[]{'*', 'U', 'D', 'F', ' ', 'L', 'V', ' ', 'I', 'n', 'f', 'o'}, new byte[]{2, 1});
            //struct LVInformation ImplementationUse;
            {
                // LVICharset
                UDFWrite.putCharSpecOSTACompressedUnicode(b);
                // dstring LogicalVolumeIdentifier[128];
                UDFWrite.putDString(b, this.logicalVolumeIdentifier, 128, false);
                // dstring LVInfo1[36];x3
                UDFWrite.zfill(b, 36 * 3);
                // struct EntityID ImplementionID;
                b.put(this.applicationIdentifier);
                // byte ImplementationUse[128];
                UDFWrite.zfill(b, 128);
            }
            // Write
            out.writep(UDFWrite.descriptorTag(b, (short) 4, out.nExtent, b.position()).array(), 0, b.position());
            assert (512 == b.position());
        }
        //   struct PartitionDescriptor 
        {
            // struct tag DescriptorTag;
            b.clear().position(16);
            // Uint32 VolumeDescriptorSequenceNumber;
            b.putInt(2);
            // Uint16 PartitionFlags;
            b.putShort((short) 1);
            // Uint16 PartitionNumber;
            b.putShort((short) 0);
            // struct EntityID PartitionContents;
            UDFWrite.putEntityId(b, 0, new byte[]{'+', 'N', 'S', 'R', '0', '2'}, new byte[]{2, 1});
            // byte PartitionContentsUse[128];
            UDFWrite.zfill(b, 128);
            // Uint32 AccessType;
            b.putInt(1);
            // Uint32 PartitionStartingLocation;
            b.putInt((int) this.lbaUDFPartitionStart);
            // Uint32 PartitionLength;
            b.putInt((int) this.partionSize);
            // struct EntityID ImplementationIdentifier;
            b.put(this.applicationIdentifier);
            // byte ImplementationUse[128];
            UDFWrite.zfill(b, 128);
            // byte Reserved[156];
            UDFWrite.zfill(b, 156);
            // Write
            out.writep(UDFWrite.descriptorTag(b, (short) 5, out.nExtent, b.position()).array(), 0, b.position());
            assert (512 == b.position());
        }
        // struct LogicalVolumeDescriptor 
        {
            // struct tag DescriptorTag;
            b.clear().limit(446).position(16);
            // Uint32 VolumeDescriptorSequenceNumber;
            b.putInt(3);
            // struct charspec DescriptorCharacterSet;
            UDFWrite.putCharSpecOSTACompressedUnicode(b);
            // dstring LogicalVolumeIdentifier[128];
            UDFWrite.putDString(b, this.logicalVolumeIdentifier, 128, false);
            // Uint32 LogicalBlockSize;
            b.putInt(out.blockSize);
            // struct EntityID DomainIdentifier;
            assert (b.position() == 216);
            b.put(ENTITYID_OSTA_COMPLIANT);
            // struct long_ad LogicalVolumeContentsUse;
            assert (b.position() == 248);
            b.putInt(out.blockSize * 2);
            UDFWrite.zfill(b, 12);
            // Uint32 MapTableLength;
            b.putInt(6);
            // Uint32 NumberofPartitionMaps;
            b.putInt(1);
            // struct EntityID ImplementationIdentifier;
            b.put(this.applicationIdentifier);
            // byte ImplementationUse[128];
            UDFWrite.zfill(b, 128);
            // struct extent_ad IntegritySequenceExtent;
            b.putInt(out.blockSize * 2).putInt((int) this.lbaIntegritySequence);
            // struct Type1PartitionMap PartitionMaps;
            {
                // Uint8 PartitionMapType;
                b.put((byte) 1);
                // Uint8 PartitionMapLength;
                b.put((byte) 6);
                // Uint16 VolumeSequenceNumber;
                b.putShort((short) 1);
                // Uint16 PartitionNumber;
                b.putShort((short) 0);

            }
            // Write
            out.writep(UDFWrite.descriptorTag(b, (short) 6, out.nExtent, b.position()).array(), 0, b.position());
            assert (446 == b.position());
        }
        // struct UnallocatedSpaceDesc  // ISO 13346 3/10.8 
        {
            // struct tag DescriptorTag;
            b.clear().position(16);
            // Uint32 VolumeDescriptorSequenceNumber;
            b.putInt(4);
            // Uint32 NumberofAllocationDescriptors;
            b.putInt(0);
            // Write
            out.writep(UDFWrite.descriptorTag(b, (short) 7, out.nExtent, b.position()).array(), 0, b.position());
            assert (24 == b.position());
        }
        UDFBuild.terminatingDescriptor(out);
        out.padBlocks(10);
    }

    void anchorVolumeDescriptorPointer(BlockSink out) throws IOException {
        // struct AnchorVolumeDescriptorPointer  // ISO 13346 3/10.2
        ByteBuffer b = out.getBuffer();
        // struct tag DescriptorTag;
        b.limit(512).position(16);
        // struct extent_ad MainVolumeDescriptorSequenceExtent;
        b.putInt(out.blockSize * 16).putInt((int) this.lbaMainVolumeDesc);
        // struct extent_ad ReserveVolumeDescriptorSequenceExtent;
        b.putInt(out.blockSize * 16).putInt((int) this.lbaReserveVolumeDesc);
        // byte Reserved[480];
        UDFWrite.zfill(b, 480);
        // Write
        out.writep(UDFWrite.descriptorTag(b, (short) 2, out.nExtent, b.position()).array(), 0, b.position());
        assert (512 == b.position());
    }

    void filesetDescriptor(BlockSink out) throws IOException {
//struct FileSetDescriptor { /* ISO 13346 4/14.1 */
        ByteBuffer b = out.getBuffer();
//        struct tag DescriptorTag;
        b.limit(512).position(16);
//        struct timestamp RecordingDateandTime;
        UDFWrite.putTimestamp(b, recordingDateandTime);
//        Uint16 InterchangeLevel;
        b.putShort((short) 3);
//        Uint16 MaximumInterchangeLevel;
        b.putShort((short) 3);
//        Uint32 CharacterSetList;
        b.putInt(1);
//        Uint32 MaximumCharacterSetList;
        b.putInt(1);
//        Uint32 FileSetNumber;
        b.putInt(0);
//        Uint32 FileSetDescriptorNumber;
        b.putInt(0);
//        struct charspec LogicalVolumeIdentifierCharacterSet;
        UDFWrite.putCharSpecOSTACompressedUnicode(b);
//        dstring LogicalVolumeIdentifier[128];
        UDFWrite.putDString(b, logicalVolumeIdentifier, 128, false);
//        struct charspec FileSetCharacterSet;
        UDFWrite.putCharSpecOSTACompressedUnicode(b);
//        dstring FileSetIdentifer[32];
        UDFWrite.putDString(b, fileSetIdentifier, 32, false);
//        dstring CopyrightFileIdentifier[32];
        UDFWrite.putDString(b, "copyright", 32, false);
//        dstring AbstractFileIdentifier[32];
        UDFWrite.putDString(b, "abstract", 32, false);
//        struct long_ad RootDirectoryICB;
        b.putInt(out.blockSize);
        b.putInt((int) (this.lbaRootDirectoryStart - this.lbaUDFPartitionStart)).putShort((short) 0);
        b.putShort((short) 0).putInt(0);
//        struct EntityID DomainIdentifier;
        b.put(ENTITYID_OSTA_COMPLIANT);
//        struct long_ad NextExtent;
        b.putInt(0).putInt(0).putInt(0).putInt(0);
//        byte Reserved[48];
        UDFWrite.zfill(b, 48);
//};
        // Write
        out.writep(UDFWrite.descriptorTag(b, (short) 256, out.nExtent - this.lbaUDFPartitionStart, b.position()).array(), 0, b.position());
        assert (512 == b.position());
    }

    void tryPush(LinkedList<DataDesc> descs, Node<Inode> node, boolean isEntry) {
        Inode ino = node.getData();
        if (isEntry) {
            if ((ino.flag & 0x1) != 0) {
                return;
            }
            ino.flag |= 0x1;
        } else {
            if ((ino.flag & 0x2) != 0) {
                return;
            }
            ino.flag |= 0x2;
        }
        descs.add(new DataDesc(node, descs.size(), isEntry));
    }

    void build(LinkedList<DataDesc> descs, Node<Inode> parent) {
        for (Node<Inode> child : parent) {
            tryPush(descs, child, true); // put children's inode
        }
        for (Node<Inode> child : parent) {
            tryPush(descs, child, false); // put children's data
        }
        for (Node<Inode> child : parent) {
            if (child.getData().isDirectory()) {
                build(descs, child);
            }
        }
    }

    void supply(Node<Inode> node) {
        Inode payload = node.getData();
        if (payload == null) {

        }
        for (Node<Inode> child : node) {
            supply(child);
        }
    }

    void write(BlockSink out, DataDesc dd) throws IOException {
        Node<Inode> nod = dd.node;
        Inode ino = nod.getData();
        if (dd.isEntry()) {
            long id;
            boolean isRoot = false;
            if (ino.isDirectory()) {
                if (nod.getParent() == null) {
                    isRoot = true;
                    if (out.nStatus != 0) {
                        this.lbaRootDirectoryStart = out.nExtent;
                    } else {
                        assert (this.lbaRootDirectoryStart == out.nExtent);
                    }
                }
            }
            if (out.nStatus != 0) {
                ino.auxA = out.nExtent++; // 1 block 
                assert (ino.nlink > 0);
            } else {
                assert (ino.auxA == out.nExtent);
                assert (ino.auxB < this.totalSectors);
                assert ((ino.auxB == 0) || (ino.auxB > this.lbaUDFPartitionStart));
                UDFWriteFile.fileEntry(out, ino, isRoot ? 0 : 16 + dd.seq, this.applicationIdentifier, ino.auxA - this.lbaUDFPartitionStart, ino.auxB - this.lbaUDFPartitionStart);
            }
        } else {
            fileData(out, nod);
        }
    }

    public void fileData(final BlockSink out, final Node<Inode> cur) throws IOException {
        final Inode ino = cur.getData();
        if (ino.isDirectory()) {
            long lbaBase = out.nExtent;
            long rbaBase = lbaBase - lbaUDFPartitionStart;
            long _size = 0;
            Node<Inode> parent = cur.getParent();
            if (parent == null) {// root
                parent = cur;
            }
            _size += fileItem(out, parent, null, rbaBase, parent.getData().auxA - lbaUDFPartitionStart);
            for (Node<Inode> child : cur) {
                Inode cIno = child.getData();
                _size += fileItem(out, child, child.getName(), rbaBase + (_size / out.blockSize), cIno.auxA - lbaUDFPartitionStart);
                if (out.nStatus != 0) {
                    if (!cIno.isDirectory()) {
                        this.nFiles++;
                    }
                }
            }
            out.writep();
            if (out.nStatus != 0) {
                this.nDirectories++;
                assert (rbaBase > 0);
                assert (_size > 0);
                assert (0 == ino.auxB);
                ino.auxB = lbaBase;
                ino.size = _size;
            } else {
                assert (ino.auxB > this.lbaUDFPartitionStart);
                assert (ino.auxB < this.totalSectors);
                assert (ino.auxB == lbaBase);
                assert (_size == ino.size);
                assert (ino.size >= 0);
                assert (ino.size < (this.partionSize * out.blockSize));
            }
        } else if (out.nStatus != 0) {
            assert (out.nLeft == 0);
            assert (0 == ino.auxB);
            long nSizePadded = out.calcBlocks(ino.size);
            assert (nSizePadded > 0);
            ino.auxB = out.nExtent;
            out.nExtent += nSizePadded;
        } else {
            assert (out.nExtent == ino.auxB);
            UDFWriteFile.fileData(out, ino, out.getBuffer().array(), ino.size, null, false);
            assert ((out.nExtent - out.calcBlocks(ino.size)) == ino.auxB);
        }
    }

    void partitionStart(BlockSink out) throws IOException {
        if (out.nStatus != 0) {
            this.lbaUDFPartitionStart = out.nExtent;
        } else {
            assert (this.lbaUDFPartitionStart == out.nExtent);
        }
        filesetDescriptor(out);
        UDFBuild.terminatingDescriptor(out, out.nExtent - this.lbaUDFPartitionStart);
    }

    void build(BlockSink out, TreeNode root, String outfile) throws IOException {
        out.getBuffer().order(ByteOrder.LITTLE_ENDIAN);
        Iterator<Node<Inode>> w = root.depthFirstIterator();
        while (w.hasNext()) {
            Node<Inode> cur = w.next();
            Inode ino = cur.getData();
            assert (ino.getFileType() != 0);
            if (cur.isLeaf()) {
                assert (!ino.isDirectory());
            } else {
                assert (ino.isDirectory());
            }
            if (ino.hasModifiedTime()) //            System.err.println(cur.getName());
            {

            }

            if (ino.isDirectory()) {
                Node<Inode> parent = cur.getParent();
                if (parent != null) {
                    parent.getData().nlink++;
                }
            }
            ino.nlink++;
        }
// prep vars
        {
            OffsetDateTime now = OffsetDateTime.now();
            if (logicalVolumeIntegrityDescTime == null) {
                logicalVolumeIntegrityDescTime = now;
            }
            if (recordingDateandTime == null) {
                recordingDateandTime = now;
            }
            // Set Prefix
            // now.
            String setPrefix = String.format("%04d%02d%02dT%02d%02d%02d%s", now.getYear(), now.getMonth().getValue(), now.getDayOfMonth(), now.getHour(), now.getMinute(), now.getSecond(), now.getOffset());
            // Set VolumeSetIdentifier
            if (volumeSetIdentifier == null || volumeSetIdentifier.isEmpty()) {

            }
            // 	 - CS0 representation of unique hex number in first 8 character positions, UDF 2.2.2.5
            if (volumeSetIdentifier == null || volumeSetIdentifier.isEmpty()) {
                Random rnd = new Random();
                volumeSetIdentifier = String.format("%04X%04X-VSI", rnd.nextLong(), rnd.nextLong());

            }
            // Set LogicalVolumeIdentifier
            if (logicalVolumeIdentifier == null || logicalVolumeIdentifier.isEmpty()) {
                String name = root.getName();
                if (name != null && !name.isEmpty()) {
                    logicalVolumeIdentifier = name;
                }
            }
            if (logicalVolumeIdentifier == null || logicalVolumeIdentifier.isEmpty()) {

                logicalVolumeIdentifier = String.format("VOL-%S", Long.toString(now.toEpochSecond(), Character.MAX_RADIX));
            }
            // Set FileSetIdentifier
            if (fileSetIdentifier == null || fileSetIdentifier.isEmpty()) {
                fileSetIdentifier = String.format("FSI%X", now.toEpochSecond());
            }
            // Set ApplicationIdentifier
            String appId = "mkimg-1.0";
            if (applicationIdentifier == null) {
                ByteBuffer b = ByteBuffer.allocate(32);
                UDFWrite.entityId(b, appId);
                applicationIdentifier = b.array();
            }

            System.err.printf("%24s: %s\n", "LogicalVolumeIdentifier", logicalVolumeIdentifier);
            System.err.printf("%24s: %s\n", "VolumeSetIdentifier", volumeSetIdentifier);
            System.err.printf("%24s: %s\n", "FileSetIdentifier", fileSetIdentifier);
            System.err.printf("%24s: %s\n", "ApplicationIdentifier", appId);
            System.err.printf("%24s: %s\n", "RecordTime", recordingDateandTime);
        }
// prep tree
        LinkedList<DataDesc> descs = new LinkedList<>();
        {

            tryPush(descs, root, true);
            tryPush(descs, root, false);
            build(descs, root);
        }
// build image
        out.nStatus = 1;
        long volumeSize = 0;
        do {
            if (out.nStatus != 0) {
                System.err.println("Calculating ...");
            } else if (outfile == null || outfile.isEmpty()) {
                System.out.println(out.nExtent);
                break;
            } else if (outfile.equals("NUL")) {
                System.err.println("Writing to nowhere ...");
            } else if (outfile.equals("-")) {
                System.out.println("Writing to STDOUT...");
            } else if (outfile.charAt(0) == '|') {
                String cmd = outfile.substring(1);
                System.out.print("Piping to: ");
                System.out.println(cmd);
            } else {
                System.out.print("Writing to: ");
                System.out.println(outfile);
                out.setOutputStream(new FileOutputStream(outfile));
            }

            out.reset();
            if (out.nStatus == 0) {
                out.nExtent = 0;
            }
            UDFBuild.systemArea(out);
            UDFBuild.volumeRecognitionArea(out);
            logicalVolumeIntegrityDesc(out);
            UDFBuild.terminatingDescriptor(out);
            PARTITION:
            {
                /////
                {
                    out.padUpTo(256 - 16);
                    volumeDescriptorSequence(out);
                    out.padUpTo(256);
                    anchorVolumeDescriptorPointer(out);
                }
                partitionStart(out);
                {
                    for (DataDesc desc : descs) {
                        write(out, desc);
                    }
                }
                PARTITION_END:
                {
                    assert (out.nExtent > this.lbaUDFPartitionStart);
                    if (out.nStatus != 0) {
                        this.partionSize = out.nExtent - this.lbaUDFPartitionStart;
                    } else {
                        assert ((out.nExtent - this.lbaUDFPartitionStart) == this.partionSize);
                    }
                }
            }
            volumeDescriptorSequence(out);
            anchorVolumeDescriptorPointer(out);
            this.totalSectors = out.nExtent;

            if (out.nStatus == 0) {
                System.err.println("Writing completed\n");
                assert ((out.nExtent * out.blockSize) == volumeSize);
                assert ((16 + descs.size()) == this.nLastUniqueID);
            } else {
                this.nLastUniqueID = 16 + descs.size();
                volumeSize = out.nExtent * out.blockSize;
                assert (out.nExtent <= 2147483647);
                assert (out.nExtent > (256 + 1));
                System.err.printf("%20s: %s\n", "MainVolumeDescLBA", lbaMainVolumeDesc);
                System.err.printf("%20s: %s\n", "ReserveVolumeDescLBA", lbaReserveVolumeDesc);
                System.err.printf("%20s: %s\n", "IntegritySequenceLBA", lbaIntegritySequence);
                System.err.printf("%20s: %s\n", "PartitionStartLBA", lbaUDFPartitionStart);
                System.err.printf("%20s: %s\n", "RootDirectoryLBA", lbaRootDirectoryStart);
                System.err.printf("%20s: %s\n", "PartionSize", partionSize);
                System.err.printf("%20s: %s\n", "BlockSize", out.blockSize);
//                System.err.printf("%20s: %s\n", "PartionSize", partionSize);
                System.err.printf("%20s: %s\n", "VolumeSize", volumeSize);
            }
            System.err.printf("Entries: Files %d, Directories %d\n", this.nFiles, this.nDirectories);
            System.err.printf("Image Size %d sectors, %d, %d wasted\n", out.nExtent, volumeSize, out.nWasted);
        } while (out.nStatus-- != 0);
    }
}
/*
 K:/wrx/java/mkimg/bin/mkimg.cmd K:\app\nt\BootICE -o C:/temp/udf.iso && K:/pub/001/udf_test.exe -ecclength 1  C:/temp/udf.iso >  C:/temp/udf.log
 */

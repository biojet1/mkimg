package mkimg;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.OffsetDateTime;
import java.util.Arrays;
import udf.CrcCCITT;
import udf.UDFWrite;

public class UDFBuild {

    private final OffsetDateTime logicalVolumeIntegrityDescTime;
    private final OffsetDateTime recordingDateandTime;
    private long lbaIntegritySequence;
    private long nLastUniqueID;
    private int partionSize;
    private int nFiles;
    private int nDirectories;
    private String logicalVolumeIdentifier;
    private String applicationIdentifier;
    private String volumeSetIdentifier;

    public UDFBuild() {
        recordingDateandTime = logicalVolumeIntegrityDescTime = OffsetDateTime.now();
    }

    static void volumeRecognitionArea(BlockSink out) throws IOException {
        assert ((out.blockSize % 512) == 0);
        assert ((32768 / out.blockSize) == out.nExtent);
        ByteBuffer b = out.getBuffer();
        byte[][] vrid = new byte[][]{{'B', 'E', 'A', '0', '1'}, {'N', 'S', 'R', '0', '2'}, {'T', 'E', 'A', '0', '1'}};
        b.clear();
        Arrays.fill(b.array(), 0, 2048, (byte) 0);
        for (int i = 0; i < 3; ++i) {
            b.clear();
            b.put((byte) 0);// StructureType
            b.put(vrid[i]);//StandardIdentifier
            b.put((byte) 1);// StructureVersion
            out.writeBlocks(b.array(), 0, 2048);
        }

        assert (((32768 / out.blockSize) + ((3 * 2048) / out.blockSize)) == out.nExtent);
    }

    static void terminatingDescriptor(BlockSink out, long lba) throws IOException {
        ByteBuffer b = out.getBuffer();
        b.clear();
        b.position(512);
        Arrays.fill(b.array(), 0, b.position(), (byte) 0);
        UDFWrite.descriptorTag(b, (short) 8, lba, 512);
        out.writePadded(b.array(), 0, b.position());
    }

    static void systemArea(BlockSink out) throws IOException {
        out.padUpTo(32768 / out.blockSize);
    }

    void primaryVolumeDescriptor(BlockSink out) throws IOException {
        ByteBuffer b = out.getBuffer();
        // PrimaryVolumeDescriptor
        b.clear();
        // tag DescriptorTag
        b.position(16);
        // Uint32 VolumeDescriptorSequenceNumber;
        b.putInt(0);
        // Uint32 PrimaryVolumeDescriptorNumber;
        b.putInt(0);
        // dstring VolumeIdentifier[32];
        UDFWrite.putDString(b, this.logicalVolumeIdentifier, 32);
        //Uint16 VolumeSequenceNumber;
        b.putShort((short) 1);
        //Uint16 MaximumVolumeSequenceNumber;
        b.putShort((short) 1);
        //Uint16 InterchangeLevel;
        b.putShort((short) 2);
        //Uint16 MaximumInterchangeLevel;
        b.putShort((short) 2);
        //Uint32 CharacterSetList;
        b.putShort((short) 1);
        //Uint32 MaximumCharacterSetList;
        b.putShort((short) 1);
        // dstring VolumeSetIdentifier[128];
        UDFWrite.putDString(b, this.volumeSetIdentifier, 128);
        // struct charspec DescriptorCharacterSet;
        UDFWrite.putCharSpecOSTACompressedUnicode(b);
        // struct charspec ExplanatoryCharacterSet;
        UDFWrite.putCharSpecOSTACompressedUnicode(b);
        // struct extent_ad VolumeAbstract;
        UDFWrite.extentAd(b, 0, 0);
        // struct extent_ad VolumeCopyrightNotice;
        UDFWrite.extentAd(b, 0, 0);
        //struct EntityID ApplicationIdentifier;
        for (int i = 0; i++ < 32;) {
            b.put((byte) 0);
        }
        //struct timestamp RecordingDateandTime;
        UDFWrite.putTimestamp(b, this.recordingDateandTime);
        //struct EntityID ImplementationIdentifier;
        UDFWrite.entityId(b, this.applicationIdentifier);
        //byte ImplementationUse[64];
        for (int i = 0; i++ < 64;) {
            b.put((byte) 0);
        }
        //Uint32 PredecessorVolumeDescriptorSequenceLocation;
        b.putInt(0);
        //Uint16 Flags;
        b.putShort((short) 0);
        //byte Reserved[22];
        for (int i = 0; i++ < 22;) {
            b.put((byte) 0);
        }
        //
        UDFWrite.descriptorTag(b, (short) 1, out.nExtent, 512);
        //
        out.writePadded(b.array(), 0, b.position());
    }

    void UDFWriteLVI(BlockSink out) throws IOException {
        if (out.nStatus != 0) {
            this.lbaIntegritySequence = out.nExtent;
        } else {
            assert (this.lbaIntegritySequence == out.nExtent);
        }
        ByteBuffer b = out.getBuffer();
        b.position(16); //  tag DescriptorTag
        UDFWrite.putTimestamp(b, this.logicalVolumeIntegrityDescTime); // timestamp RecordingDateAndTime
        b.putInt(1); // Uint32 IntegrityType  (Close Integrity Descriptor)
        // extent_ad NextIntegrityExtent;
        b.putInt(0);
        b.putInt(0);
        // LogicalVolumeIntegrityDescContentsUse LogicalVolumeContentsUse;
        b.putLong(this.nLastUniqueID);
        b.putLong(0);
        b.putLong(0);
        b.putLong(0);
        b.putInt(1); //Uint32 NumberOfPartitions;
        b.putInt(46); //Uint32 LengthOfImplementationUse;
        b.putInt(0); //Uint32 FreeSpaceTable;
        b.putInt(this.partionSize); //Uint32 SizeTable;
        // LogicalVolumeIntegrityDescImplementationUse
        UDFWrite.entityId(b, "mkimg"); // EntityID ImplementationID
        b.putInt(this.nFiles); // Uint32 NumberofFiles;
        b.putInt(this.nDirectories); // Uint32 NumberofDirectories;
        b.putShort((short) 0x102); // Uint16 MinimumUDFReadRevision;
        b.putShort((short) 0x102); // Uint16 MinimumUDFWriteRevision;
        b.putShort((short) 0x102); // Uint16 MaximumUDFWriteRevision;
        //
        UDFWrite.descriptorTag(b, (short) 9, out.nExtent, 134);
        //
        out.writePadded(b.array(), 0, b.position());
    }

    void volumeDescriptorSequence(BlockSink out) throws IOException {
        primaryVolumeDescriptor(out);
        //struct ImpUseVolumeDescriptor 
        if (true) {
            ByteBuffer b = out.getBuffer();
            //struct tag DescriptorTag;
            b.position(16);
            //Uint32 VolumeDescriptorSequenceNumber;
            b.putInt(1);
            //struct EntityID ImplementationIdentifier;
            UDFWrite.putEntityId(b, new byte[]{'*', 'U', 'D', 'F', ' ', 'L', 'V', ' ', 'I', 'n', 'f', 'o'}, new byte[]{2, 1});
            //struct LVInformation ImplementationUse;
            UDFWrite.putCharSpecOSTACompressedUnicode(b);
            UDFWrite.putDString(b, this.logicalVolumeIdentifier, 128);
            for (int i = 36 * 3; i-- > 0;) {
                b.put((byte) 0);
            }
            UDFWrite.entityId(b, this.applicationIdentifier);
            //
            UDFWrite.descriptorTag(b, (short) 4, out.nExtent, 512);
            //
            out.writePadded(b.array(), 0, b.position());
        }
    }

    void build(BlockSink out) throws IOException {
        out.nStatus = 1;
        do {
            if (out.nStatus != 0) {
                System.err.println("Calculating ...");

            }
            out.reset();
            if (out.nStatus == 0) {
                out.nExtent = 0;
            }
            UDFBuild.systemArea(out);
            UDFBuild.volumeRecognitionArea(out);
            UDFBuild.terminatingDescriptor(out, out.nExtent);
            out.padUpTo(256 - 16);
            //UDFWriteVDS1(udf, out, db);

            out.padUpTo(256);
        } while (out.nStatus-- != 0);
    }
}

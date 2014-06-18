
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package my.ui;

import java.util.Arrays;

/**
 *
 * @author dimmoro
 */
public class MNFBlockTask extends FileTask {
    public MNFBlockTask(MainUI m, int t, int b) {
        super(m,"");
        this.mnfType = t;
        this.blockNumber = b;
        this.inFilePath = this.outFilePathRoot+this.mnfType+"B"+b;
    }

    @Override
    protected void doIt() throws Exception {
        int remainingSize = progressMonitorInputStream.available();
        int numOfRecords;
        int numOfInserts = 0;

        isLittleEndian = true;
        switch (blockNumber) {
            default:
                break;
            case 1:
                numOfRecords = remainingSize/4;
                displaytoUI("Number of Records: "+numOfRecords);
                for (int recordCursor = 0; recordCursor < numOfRecords; recordCursor++) {
                    displaytoUI("----- Record " + (recordCursor+1) + "-----");
                    progressMonitorInputStream.read(dword);
                    displaytoUI("UniqueID = "+byteArraytoInt(dword));
                }
                break;
            case 2:
                numOfRecords = remainingSize/8;
                displaytoUI("Number of Records: "+numOfRecords);
                for (int recordCursor = 0; recordCursor < numOfRecords; recordCursor++) {
                    progressMonitorInputStream.read(dword);
                    int fileNumber = byteArraytoInt(dword);
                    progressMonitorInputStream.skip(4);

                    if (fileNumber < 0) {
                        displaytoUI("----- Record " + (recordCursor+1) + "-----");
                        displaytoUI("FileNumber = "+fileNumber);
                    }

                    String updateQuery = "UPDATE mnfblocks SET fileNumber="+fileNumber+" WHERE mnfType="+mnfType+" AND fileIndex="+(recordCursor+1);
                    stat.executeUpdate(updateQuery);
                    numOfInserts++;
                }
                break;
            case 3:
                //truncateTable("mnfblocks");
                String removeQuery = "DELETE FROM mnfblocks WHERE mnfType="+mnfType;
                stat.executeUpdate(removeQuery);

                numOfRecords = remainingSize/20;
                displaytoUI("Number of Records: "+numOfRecords);

                for (int recordCursor = 0; recordCursor < numOfRecords; recordCursor++) {
                    progressMonitorInputStream.read(dword);
                    int uncompressedSize = byteArraytoInt(dword);
                    progressMonitorInputStream.read(dword);
                    int compressedSize = byteArraytoInt(dword);

                    progressMonitorInputStream.read(dword);
                    int fileHash = Arrays.hashCode(dword);
                    progressMonitorInputStream.read(dword);
                    int fileOffset = byteArraytoInt(dword);
                    progressMonitorInputStream.read(aByte);
                    int compressType = byteArraytoInt(aByte);
                    progressMonitorInputStream.read(aByte);
                    archiveIndex = byteArraytoInt(aByte);
                    progressMonitorInputStream.skip(2);  //Skip 2 bytes                        

                    /*
                    displaytoUI("----- Record " + (recordCursor+1) + "-----");
                    displaytoUI("UncompressedSize = " + uncompressedSize);
                    displaytoUI("CompressedSize = " + compressedSize);
                    displaytoUI("FileHash = " + fileHash);
                    displaytoUI("FileOffset = " + fileOffset);
                    displaytoUI("FileInfo Compress Type = " + compressType);
                    displaytoUI("FileInfo Archive Index = " + archiveIndex);
                    */

                    String insertQuery = "INSERT INTO mnfblocks "
                                       + "(mnfType, fileIndex, archiveIndex,compressType,fileOffset,compressedSize,uncompressedSize) "
                                       + "VALUES ("+mnfType+","+(recordCursor+1)+","+archiveIndex+","+compressType+","+fileOffset+","+compressedSize+","+uncompressedSize+")";
                    stat.executeUpdate(insertQuery);
                    numOfInserts++;
                }
                break;
        }

        displaytoUI("Inserted: "+numOfInserts);
    }

}

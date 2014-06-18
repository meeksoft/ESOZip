
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package my.ui;

/**
 *
 * @author dimmoro
 */
public class ZOSFTBlockTask extends FileTask {
    public ZOSFTBlockTask(MainUI m, int t, int mb, int b) {
        super(m,"");
        this.mnfType = t;
        this.mnfBlockNumber = mb;
        this.blockNumber = b;
        this.inFilePath = this.outFilePathRoot+this.mnfType+"Z"+mb+"_"+b;
    }

    @Override
    protected void doIt() throws Exception {
        int remainingSize = progressMonitorInputStream.available();
        int numOfRecords;
        int numOfInserts = 0;
        byte[] buffer;

        isLittleEndian = true;
        switch (mnfBlockNumber) {
            default:
                break;
        }
        switch (blockNumber) {
            default:
                break;
            case 1:
                break;
            case 2:
                numOfRecords = remainingSize/4;
                displaytoUI("Number of Records: "+numOfRecords);

                for (int recordCursor = 0; recordCursor < numOfRecords; recordCursor++) {
                    progressMonitorInputStream.read(dword);
                    int fileNumber = byteArraytoInt(dword);

                    /*
                    displaytoUI("----- Record " + (recordCursor+1) + "-----");
                    displaytoUI("FileNumber = " + fileNumber);
                    */

                    String updateQuery = "UPDATE zosftblocks SET fileNumber="+fileNumber+" WHERE mnfType="+mnfType+" AND fileIndex="+(recordCursor+1);
                    stat.executeUpdate(updateQuery);
                    numOfInserts++;
                }
                break;
            case 3:
                numOfRecords = remainingSize/16;
                displaytoUI("Number of Records: "+numOfRecords);

                for (int recordCursor = 0; recordCursor < 20; recordCursor++) {
                    progressMonitorInputStream.read(dword);
                    int fileIndex = byteArraytoInt(dword);
                    progressMonitorInputStream.read(dword);
                    int fileOffset = byteArraytoInt(dword);
                    buffer = new byte[8];
                    progressMonitorInputStream.read(buffer);
                    long complex = byteArraytoLong(buffer);

                    displaytoUI("----- Record " + (recordCursor+1) + "-----");
                    displaytoUI("FileIndex = " + fileIndex);
                    displaytoUI("FileOffset = " + fileOffset);
                    displaytoUI("Complex = " + complex);

                    numOfInserts++;
                }
                break;
        }

        displaytoUI("Inserted: "+numOfInserts);
    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package my.ui;

import java.sql.ResultSet;

/**
 *
 * @author dimmoro
 */
public class ZOSFTTask extends FileTask {
    public ZOSFTTask(MainUI m,String f,int mt) {
        super(m,"");
        this.inFilePath = this.outFilePathRoot+f;
        mnfType = mt;
    }

    @Override
    protected void doIt() throws Exception {
        ResultSet rs;
        byte[] buffer;

        isLittleEndian = true;
        
        boolean foundZOSFT = false;
        buffer = new byte[1];
        int remainingSize = progressMonitorInputStream.available();
        for (int i = 0; i < remainingSize; i++) {
            progressMonitorInputStream.read(buffer);
            String s = new String(buffer);
            if (s.compareTo("Z")  == 0) {
                i++;
                progressMonitorInputStream.read(buffer);
                s = new String(buffer);
                if (s.compareTo("O") == 0) {
                    i++;
                    progressMonitorInputStream.read(buffer);
                    s = new String(buffer);
                    if (s.compareTo("S") == 0) {
                        i++;
                        progressMonitorInputStream.read(buffer);
                        s = new String(buffer);
                        if (s.compareTo("F") == 0) {
                            i++;
                            progressMonitorInputStream.read(buffer);
                            s = new String(buffer);
                            if (s.compareTo("T") == 0) {
                                foundZOSFT = true;
                                break;
                            }
                        }
                    }                    
                }
            }
        }
        
        if (!foundZOSFT) return;
        displaytoUI("ZOSFT");

        progressMonitorInputStream.skip(2);
        progressMonitorInputStream.skip(4);
        progressMonitorInputStream.read(dword);
        displaytoUI("Type: " + byteArraytoInt(dword));
        progressMonitorInputStream.read(dword);
        int recordCount = byteArraytoInt(dword);
        displaytoUI("RecordCount: " + recordCount);
        
        for (int mb = 1; mb <= 3; mb++) {

            /* Read Block Header */
            displaytoUI("-----Block Header "+mb+"-----");
            progressMonitorInputStream.read(word);
            displaytoUI("BlockId = " + byteArraytoInt(word));
            progressMonitorInputStream.read(dword);
            displaytoUI("FieldSize = " + byteArraytoInt(dword));
            progressMonitorInputStream.read(dword);
            int recordCountB1 = byteArraytoInt(dword);
            displaytoUI("RecordCountB1 = " + recordCountB1);
            progressMonitorInputStream.read(dword);
            int recordCountB2 = byteArraytoInt(dword);
            displaytoUI("RecordCountB2 = " + recordCountB2);
            progressMonitorInputStream.read(dword);
            int recordCountB3 = byteArraytoInt(dword);
            displaytoUI("RecordCountB3 = " + recordCountB3);

            int uncompressedSize;
            int compressedSize;
            String outFileName;

            /* Block B1, B2, B3 */
            for (int b = 1; b <= 3; b++) {
                if (mnfType == 6 && mb == 3 && b > 1) continue; //GAME ZOSFT does not have 3.2,3.3
                
                displaytoUI("-----B"+mb+"."+b+"-----");
                progressMonitorInputStream.read(dword);
                uncompressedSize = byteArraytoInt(dword);
                displaytoUI("UncompressedSize = " + uncompressedSize);
                progressMonitorInputStream.read(dword);
                compressedSize = byteArraytoInt(dword);
                displaytoUI("CompressedSize = " + compressedSize);
                
                if (mb == 2 && b > 1) {
                    buffer = new byte[compressedSize];
                    progressMonitorInputStream.read(buffer);
                    outFileName = outFilePathRoot+mnfType+"Z"+mb+"_"+b;
                    byteArrayZLibDecompressWrite(buffer, uncompressedSize, outFileName);                    
                } else {
                    progressMonitorInputStream.skip(compressedSize);                
                }
            }
        }
        
        //Handle filenames
        String removeQuery = "DELETE FROM zosftblocks WHERE mnfType="+mnfType;
        stat.executeUpdate(removeQuery);

        displaytoUI("-----Filename Data-----");
        progressMonitorInputStream.read(dword);
        int recordSize = byteArraytoInt(dword);
        displaytoUI("RecordSize: " + recordSize);
        
        buffer = new byte[1];
        remainingSize = progressMonitorInputStream.available()-5;
        displaytoUI("RemainingSize: " + remainingSize);
        int fileNameCount = 0;
        String aFilename = "";
        for (int i = 1; i <= remainingSize; i++) {
            progressMonitorInputStream.read(buffer);
            if (buffer[0] == 0) {
                //displaytoUI(aFilename);
                fileNameCount++;
                String insertQuery = "INSERT INTO zosftblocks "
                                   + "(mnfType, fileIndex, fileName) "
                                   + "VALUES ("+mnfType+","+fileNameCount+",'"+aFilename+"')";
                stat.executeUpdate(insertQuery);
                aFilename = "";
            } else {
                aFilename += new String(buffer);
            }
        }
        displaytoUI("FileNameCount: " + fileNameCount);
        
        buffer = new byte[5];
        progressMonitorInputStream.read(buffer);
        displaytoUI(new String(buffer));
    }    

}

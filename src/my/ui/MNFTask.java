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
public class MNFTask extends FileTask {
    public MNFTask(MainUI m, String f) {
        super(m,f);
    }

    @Override
    protected void doIt() throws Exception {
        ResultSet rs;

        /* Read MNF file header */
        isLittleEndian = true;
        progressMonitorInputStream.read(dword);
        displaytoUI("MAGIC WORD = " + new String(dword));
        progressMonitorInputStream.read(word);
        displaytoUI("MES_VERSION = " + byteArraytoInt(word));
        progressMonitorInputStream.read(aByte);
        displaytoUI("FileCount = " + byteArraytoInt(aByte));
        progressMonitorInputStream.read(dword);
        mnfType = byteArraytoInt(dword);
        displaytoUI("MNF_TYPE = " + mnfType);
        progressMonitorInputStream.read(dword);
        displaytoUI("DataSize = " + byteArraytoInt(dword));

        int uncompressedSize;
        int compressedSize;
        isLittleEndian = false;
        byte[] buffer;
        String outFileName;

        /* Handle Block Type 0, only found in eso.mnf */
        if (mnfType == 1) {
            displaytoUI("-----B0-----");
            progressMonitorInputStream.read(word);
            displaytoUI("BlockId = " + byteArraytoInt(word));
            progressMonitorInputStream.read(word);
            displaytoUI("Unknown1 = " + byteArraytoInt(word));

            for (int i = 0; i < 2; i++) {
                progressMonitorInputStream.read(dword);
                uncompressedSize = byteArraytoInt(dword);
                displaytoUI("UncompressedSize = " + uncompressedSize);
                //progressMonitorInputStream.read(dword);
                //compressedSize = byteArraytoInt(dword);
                //displaytoUI("CompressedSize = " + compressedSize);
                progressMonitorInputStream.skip(uncompressedSize);
            }
        }

        /* Read Block Header */
        displaytoUI("-----Block Header-----");
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

        /* Block B1, B2, B3 */
        for (int b = 1; b <= 3; b++) {
            displaytoUI("-----B"+b+"-----");
            progressMonitorInputStream.read(dword);
            uncompressedSize = byteArraytoInt(dword);
            displaytoUI("UncompressedSize = " + uncompressedSize);
            progressMonitorInputStream.read(dword);
            compressedSize = byteArraytoInt(dword);
            displaytoUI("CompressedSize = " + compressedSize);
            //progressMonitorInputStream.skip(compressedSize);

            buffer = new byte[compressedSize];
            progressMonitorInputStream.read(buffer);
            outFileName = outFilePathRoot+mnfType+"B"+b;
            byteArrayZLibDecompressWrite(buffer, uncompressedSize, outFileName);
        }
    }    
}

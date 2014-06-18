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
public class DatTask extends FileTask {
    
    public DatTask(MainUI m,String f) {
        super(m,f);
        
        int idx;
        idx = f.lastIndexOf("/");
        String af = idx >= 0 ? f.substring(idx + 1) : f;
        
        if (af.startsWith("game")) {
            this.mnfType = 6;
            this.archiveIndex = 0;
        } else if (af.startsWith("esoaudioen")) {
            
        } else {
            this.mnfType = 1;
            String ai = af.substring(3, af.length()-4);
            this.archiveIndex = Integer.parseInt(ai);
        }
    }

    @Override
    protected void doIt() throws Exception {
        ResultSet rs;

        isLittleEndian = true;
        progressMonitorInputStream.read(dword);
        displaytoUI("MAGIC WORD = " + new String(dword));
        progressMonitorInputStream.read(word);
        displaytoUI("PES_VERSION = " + byteArraytoInt(word));
        progressMonitorInputStream.read(dword);
        displaytoUI("Unknown2...  ");
        progressMonitorInputStream.read(dword);
        displaytoUI("OffsetToFirstFile = " + byteArraytoInt(dword));

        int fileCount = 0;
        String outFileName; 
        byte[] buffer;
        String getBlocksQuery = "SELECT m.fileNumber, m.compressedSize, m.uncompressedSize, m.compressType, z.fileName FROM mnfblocks m "
                              + "LEFT JOIN zosftblocks z ON z.mnfType=m.mnfType AND z.fileIndex=m.fileNumber "
                              + "WHERE m.archiveIndex = "+archiveIndex+" AND m.mnfType = "+mnfType+" ORDER BY fileOffset";
        //System.out.println(getBlocksQuery);
        rs = stat.executeQuery(getBlocksQuery);            
        while(rs.next()) {
            int fileNumber = rs.getInt(1);
            int compressedSize = rs.getInt(2);
            int uncompressedSize = rs.getInt(3);
            int compressType = rs.getInt(4);
            String fileName = rs.getString(5);
        
            //progressMonitorInputStream.skip(compressedSize);
            buffer = new byte[compressedSize];
            progressMonitorInputStream.read(buffer);
            if (fileName==null || fileName.trim().length()==0) {
                outFileName = outFilePathRoot+"/esounzip/esopot/"+fileNumber;
            } else {
                outFileName = outFilePathRoot+"/esounzip/"+fileName;
            }
            switch(compressType) {
                case 1:  //Zlib
                    byteArrayZLibDecompressWrite(buffer, uncompressedSize, outFileName);
                    break;
                case 2:  //Snappy
                    byteArraySnappyDecompressWrite(buffer, uncompressedSize, outFileName);
                    break;
                case 0: //Non compressed
                    byteArrayWrite(buffer, outFileName);
                    break;
                default:
                    break;
            }
            fileCount++;
        }
        displaytoUI("FileCount = "+fileCount);
    }
}

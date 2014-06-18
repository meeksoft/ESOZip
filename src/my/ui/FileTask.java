/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package my.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import javax.swing.ProgressMonitorInputStream;
import javax.swing.SwingWorker;
import org.xerial.snappy.Snappy;

/**
 *
 * @author dimmoro
 */
public class FileTask extends SwingWorker<Void, Void> {
    protected ProgressMonitorInputStream progressMonitorInputStream;
    protected byte[] dword = new byte[4];
    protected byte[] word = new byte[2];
    protected byte[] aByte = new byte[1];
    protected boolean isLittleEndian = false;

    protected MainUI delegate;
    protected String inFilePath;
    protected String outFilePathRoot;
    protected FileInputStream fileInputStream;
    protected Connection conn;
    protected Statement stat;

    protected int mnfType;
    protected int blockNumber;
    protected int mnfBlockNumber;  //For ZOSFT
    protected int archiveIndex;
    
    public FileTask(MainUI m,String f) {
        this.delegate = m;
        this.inFilePath = f;
        this.outFilePathRoot = removeLastChar(new File(".").getAbsolutePath());

        try {
            setConnection(this.outFilePathRoot+"esozip.sqlite");
            stat = conn.createStatement();
        } catch (SQLException ex) {
            Logger.getLogger(FileTask.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    protected Void doInBackground() throws Exception {
        int progress = 0;
        //Initialize progress property.
        setProgress(0);

        File f = new File(inFilePath);
        if (!f.exists()) {
            displaytoUI("File does not exist: " + inFilePath);
            return null;
        }

        try {
            displaytoUI("Loading File..."+inFilePath);
            fileInputStream = new FileInputStream(f);
            progressMonitorInputStream = new ProgressMonitorInputStream(delegate, "Reading", fileInputStream);
            byte[] data = new byte[(int) f.length()];  // Instantiate array
            displaytoUI(data.length + "  file bytes");

            this.doIt();
            
            //Trash any extra bytes.
            int cursor = 0;
            int ch;
            while ((ch = progressMonitorInputStream.read()) != -1) {
                cursor++;
            }
            displaytoUI("Cursor: " + cursor + "...  ");

        } catch (java.lang.ArrayIndexOutOfBoundsException ex) {
            Logger.getLogger(FileTask.class.getName()).log(Level.SEVERE, null, ex);
        } catch (java.io.UnsupportedEncodingException ex) {
            Logger.getLogger(FileTask.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FileTask.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    protected void doIt() throws Exception {
    }

    /*
     * Executed in event dispatching thread
     */
    @Override
    protected void done() {
        displaytoUI("Done.\n\r");
        setProgress(100);
        try {
            progressMonitorInputStream.close();
            conn.close();
        } catch (SQLException | IOException ex) {
            Logger.getLogger(FileTask.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    protected void displaytoUI(String s) {
        delegate.tlog.append(s+"\n");
    }
    
    protected void byteArraySnappyDecompressWrite(byte[] compr, int uncomprLen, String filePath) throws IOException {
        // Decompress the bytes
        displaytoUI("Decompressing...");
        byte[] result = Snappy.uncompress(compr);
        displaytoUI("Done.\n\r");        
        byteArrayWrite(result, filePath);
    }
    
    protected void byteArrayZLibDecompressWrite(byte[] compr, int uncomprLen, String filePath) throws IOException, DataFormatException {
        displaytoUI("Decompressing...");
        int comprLen = compr.length;
        byte[] result = new byte[uncomprLen];

        java.util.zip.Inflater decompresser;
        decompresser = new java.util.zip.Inflater();
        decompresser.setInput(compr, 0, comprLen);
        int resultLength = decompresser.inflate(result);
        decompresser.end();
        displaytoUI("Done.\n\r");        

        byteArrayWrite(result, filePath);
    }

    protected void byteArrayWrite(byte[] arr, String filePath) throws IOException {
        displaytoUI("Writing File " + filePath + "...");
        insureDirs(filePath,false);
        try (FileOutputStream fileOuputStream = new FileOutputStream(filePath)) {
            fileOuputStream.write(arr);
            int bytesWriten = arr.length;
            displaytoUI(bytesWriten + " bytes writen...  ");
            fileOuputStream.flush();
            fileOuputStream.close();
        }
        displaytoUI("Done.\n\r");
    }

    protected int byteArraytoInt(byte[] arr) {
        return byteArraytoInt(arr, isLittleEndian);
    }
    protected int byteArraytoInt(byte[] arr, boolean isLEndian) {
        ByteBuffer bb = ByteBuffer.wrap(arr);
        if (isLEndian) bb.order(ByteOrder.LITTLE_ENDIAN);
        else bb.order(ByteOrder.BIG_ENDIAN);
        switch(arr.length) {
            case 1:
                /*
                int ch = arr[0];
                if (ch < 0) ch += 256;
                return ch;
                 */
                long x;
                byte b = arr[0];
                if (isLEndian) x = (b & 0xffL);
                else x = (b & 0xffL) << 24;
                return (int)x;
            case 2:
                return bb.getShort();
            case 4:
                return bb.getInt();
            default:
                return bb.getInt();
        }
    }
    
    protected long byteArraytoLong(byte[] arr) {
        return byteArraytoLong(arr, isLittleEndian);
    }
    protected long byteArraytoLong(byte[] arr, boolean isLEndian) {
        ByteBuffer bb = ByteBuffer.wrap(arr);
        if (isLEndian) bb.order(ByteOrder.LITTLE_ENDIAN);
        else bb.order(ByteOrder.BIG_ENDIAN);
        return bb.getLong();
    }
    
    protected int bytetoInt(byte b) {
        byte[] arr = new byte[1];
        arr[0] = b;
        ByteBuffer bb = ByteBuffer.wrap(arr);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        String s = bb.toString();
        return Integer.parseInt(s);
    }

    private void setConnection(String s) {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:"+s);
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(FileTask.class.getName()).log(Level.SEVERE, null, ex);
        }
        displaytoUI("Database connection: "+s);
    }
    protected void truncateTable(String tableName) {
        try {
            Statement truncateStat;
            truncateStat = conn.createStatement();
            truncateStat.executeUpdate("DELETE FROM "+tableName);
            //stat.executeUpdate("DELETE FROM SQLITE_SEQUENCE WHERE name='"+tableName+"'");
        } catch(SQLException ex) {
            Logger.getLogger(FileTask.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    protected static int getRowCount(ResultSet rs) throws SQLException {
        int current = rs.getRow();
        rs.last();
        int count = rs.getRow();
        if(count == -1)
           count = 0;
        if(current == 0)
           rs.beforeFirst();
        else
           rs.absolute(current);
        return count;
    }

    protected static String removeLastChar(String str) {
        return str.substring(0,str.length()-1);
    }
    
    protected void insureDirs(String s,boolean isDirectory) {
        File f = new File(s);
        if (!f.exists()) {
            if (isDirectory) {
                f.mkdirs();
            } else {
                this.insureDirs(f.getParent(),true);
            }
        }
    }
}

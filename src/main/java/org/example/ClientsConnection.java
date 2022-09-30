package org.example;


import java.io.*;

public class ClientsConnection {
    private String fileName;
    private long fileSize = 0;

    private final BufferedInputStream in; // поток чтения из сокета
    private final BufferedOutputStream out; // поток записи в сокет


    public ClientsConnection(BufferedInputStream in, BufferedOutputStream out) {
        this.in = in;
        this.out = out;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public BufferedInputStream getIn() {
        return in;
    }

    public BufferedOutputStream getOut() {
        return out;
    }
}

package org.example;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;

public class ClientsConnection {
    private String fileName;
    private long fileSize = 0;
    private double inst_rec_speed;
    private double avg_rec_speed;

    public double getInst_rec_speed() {
        return inst_rec_speed;
    }

    public void setInst_rec_speed(double inst_rec_speed) {
        this.inst_rec_speed = inst_rec_speed;
    }

    public double getAvg_rec_speed() {
        return avg_rec_speed;
    }

    public void setAvg_rec_speed(double avg_rec_speed) {
        this.avg_rec_speed = avg_rec_speed;
    }

    private final BufferedReader in; // поток чтения из сокета
    private final BufferedWriter out; // поток записи в сокет


    public ClientsConnection(BufferedReader in, BufferedWriter out) {
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

    public BufferedReader getIn() {
        return in;
    }

    public BufferedWriter getOut() {
        return out;
    }
}

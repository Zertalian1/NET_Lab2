package org.example;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

public class Session implements Runnable{
    ClientsConnection connection;
    private final int BUFF_SIZE = 1024;
    ReentrantLock locker = new ReentrantLock();
    private final Clock clock = Clock.systemUTC();
    private long fileSize=0;
    private long lastSize = 0;
    private int time = 0;

    public Session(Socket clientSocket) throws IOException {

        connection = new ClientsConnection(
                new BufferedInputStream(clientSocket.getInputStream()),
                new BufferedOutputStream(clientSocket.getOutputStream()));
    }

    @Override
    public void run() {

        Thread printSpeedThread = new Thread(this::printAvgSpeed);
        System.out.println("Вывод скорости запущен");
        Thread clientReaderThread = new Thread(this::clientReader);
        System.out.println("Слушатель клиента запущен");
        Instant begin = clock.instant();
        clientReaderThread.start();
        printSpeedThread.start();
        try {
            clientReaderThread.join();
            System.out.println("Соединение зарывается");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            printSpeedThread.interrupt();
            if(time<1){
                System.out.println((fileSize-lastSize)/ Duration.between(begin, clock.instant()).toMillis() * 1000);
            }
        }
    }

    private void printAvgSpeed(){
        while (true){
            try {
                Thread.sleep(3000);
                time++;
                locker.lock();
                System.out.println((fileSize-lastSize)/3);
                System.out.println(fileSize/(time* 3L));
                locker.unlock();
                if(Thread.interrupted()){
                    return;
                }
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private void clientReader() {
        FileOutputStream writer = null;
        Instant begin = null;
        byte [] input = new byte[BUFF_SIZE];
        try {
            try {

                System.out.println("Ожидание данных");
                String header;
                int elementsRead;
                elementsRead = connection.getIn().read(input);
                if( elementsRead ==-1){
                    throw new IOException();
                }
                int headerSize = 0;
                header = new String(input,0,elementsRead, StandardCharsets.UTF_8);
                for(;headerSize<BUFF_SIZE;headerSize++){
                    if(header.charAt(headerSize) == '#'){
                        break;
                    }
                }
                header = header.substring(0,headerSize);
                if (header.startsWith("ClientFileInfo:")){
                    String [] parsedMsg = header.split(":");  // ClientFileInfo:fileName:msgSize
                    System.out.println("Получен заголовок: " + header);
                    connection.setFileName(parsedMsg[1]);
                    connection.setFileSize(Long.parseLong(parsedMsg[2]));
                    writer = openFile();
                }
                else {
                    throw new IOException("Header Error");
                }
                do{
                    System.out.println(" Считывание данных: " + (float)fileSize/connection.getFileSize()*100);
                    elementsRead = connection.getIn().read(input);
                    if(elementsRead==-1){
                        break;
                    }
                    locker.lock();
                    fileSize += elementsRead;
                    locker.unlock();
                    if (writer != null) {
                        writer.write(input,0, elementsRead);
                        writer.flush();
                    }else{
                        System.out.println("Error in the format of the message sent by the client");
                        return;
                    }
                }while (true);
                writer.close();
                if(fileSize != connection.getFileSize() ){
                    System.out.println("Ошибка при передачи, теоретический и фактический размер файла не совпадают");
                    clientWriter("failure".getBytes(StandardCharsets.UTF_8));
                    return;
                }
                clientWriter("success".getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                clientWriter("failure".getBytes(StandardCharsets.UTF_8));
            }finally {
                connection.getIn().close();
                connection.getOut().close();
            }
        }catch (ClientWriterException e) {
            System.out.println(e.getMessage());
        }catch (IOException ignored){
        }
    }

    private FileOutputStream openFile(){
        FileOutputStream writer;
        int i = 0;
        while (true){
            try{
                File file = new File(".\\input\\"+connection.getFileName()+i+".txt");
                if (!file.createNewFile()){
                    i++;
                    continue;
                }
                writer = new FileOutputStream(file, false);
                break;
            }
            catch(IOException ignored){}
            i++;
        }
        return writer;
    }

    private void clientWriter(byte[] text) throws ClientWriterException {
        try {
            connection.getOut().write(text);
            connection.getOut().flush();
        } catch (IOException e) {
            throw new ClientWriterException(e.getMessage());
        }

    }
}
// пункты 6, 8
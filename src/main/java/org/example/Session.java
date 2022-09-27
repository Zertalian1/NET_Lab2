package org.example;

import java.io.*;
import java.net.Socket;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

public class Session implements Runnable{
    ClientsConnection connection;
    private final int BUFF_SIZE = 1024;
    private final Clock clock = Clock.systemUTC();
    ReentrantLock locker = new ReentrantLock();

    public Session(Socket clientSocket) throws IOException {

        connection = new ClientsConnection(
                new BufferedReader(new InputStreamReader(clientSocket.getInputStream())),
                new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())));
    }

    @Override
    public void run() {
        Thread printSpeedThread = new Thread(this::printAvgSpeed);
        System.out.println("Вывод скорости запущен");
        Thread clientReaderThread = new Thread(this::clientReader);
        System.out.println("Слушатель клиента запущен");
        clientReaderThread.start();
        printSpeedThread.start();
        try {
            clientReaderThread.join();
            System.out.println("Соединение зарывается");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            printSpeedThread.interrupt();   // не интераптит
        }
        printAvgSpeed();
    }

    private void printAvgSpeed(){
        while (true){
            try {
                System.out.println("avg speed: " + connection.getAvg_rec_speed() + " bite?/sec");
                System.out.println("inst speed: "+connection.getInst_rec_speed() + " bite?/sec");
                Thread.sleep(3000);
                if(Thread.interrupted()){
                    return;
                }
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private void clientReader() {
        FileWriter writer = null;
        //while(true) {
            Instant begin = null;
            try {
                try {
                    long fileSize=0;
                    System.out.println("Ожидание данных");
                    String header = connection.getIn().readLine();
                    if (header.startsWith("ClientFileInfo:")){
                        String [] parsedMsg = header.split(":");  // ClientFileInfo:fileName:msgSize
                        System.out.println("Получен заголовок: " + header);
                        connection.setFileName(parsedMsg[1]);
                        connection.setFileSize(Long.parseLong(parsedMsg[2]));
                        writer = openFile();
                        begin = clock.instant();
                    }
                    else {
                        throw new IOException("Header Error");
                    }
                    do{
                        System.out.println(" Считывание данных");
                        char [] input = new char[BUFF_SIZE];
                        int isEnd = connection.getIn().read(input);
                        if(isEnd==-1){
                            break;
                        }
                        int inputSize = getSize(input);
                        calculateSpeed(begin, inputSize);
                        fileSize += inputSize;
                        if (writer != null) {
                            writer.write(input,0, inputSize);
                            writer.flush();
                        }else{
                            System.out.println("Error in the format of the message sent by the client");
                            return;
                        }
                    }while (true);
                    writer.close();
                    if(fileSize != connection.getFileSize() ){
                        System.out.println("Ошибка при передачи, теоретический и фактический размер файла не совпадают");
                        throw new IOException();
                    }
                    clientWriter("success\n");
                } catch (IOException e) {
                    clientWriter("failure\n");
                }finally {
                    connection.getIn().close();
                    connection.getOut().close();
                }
            }catch (ClientWriterException e) {
                System.out.println(e.getMessage());
                //return;
            }catch (IOException ignored){}
       // }
    }

    private void calculateSpeed(Instant begin, int inputSize){
        if(begin!=null) {
            connection.setInst_rec_speed((double)inputSize / Duration.between(begin, clock.instant()).toMillis() * 1000);  // в секунду, добавить блокировку
            if(connection.getAvg_rec_speed() == 0){
                connection.setAvg_rec_speed(connection.getInst_rec_speed());
            }else {
                connection.setAvg_rec_speed((connection.getAvg_rec_speed() + connection.getInst_rec_speed()) / 2);
            }
        }
    }

    public int getSize(char[] data){
        int size=0;
        for(int i=0;i<BUFF_SIZE;i++){
            if(data[i]!='\u0000'){
                size++;
            }
        }
        return size;
    }

    private FileWriter openFile(){
        FileWriter writer;
        int i = 0;
        while (true){
            try{
                File file = new File(".\\input"+connection.getFileName()+i+".txt");
                // поправить, чтобы был не абс путь
                if (!file.createNewFile()){
                    i++;
                    continue;
                }
                writer = new FileWriter(file, false);
                break;
            }
            catch(IOException ignored){}
            i++;
        }
        return writer;
    }

    private void clientWriter(String text) throws ClientWriterException {
        try {
            connection.getOut().write(text);
            connection.getOut().flush();
        } catch (IOException e) {
            throw new ClientWriterException(e.getMessage());
        }

    }
}
// пункты 6, 8
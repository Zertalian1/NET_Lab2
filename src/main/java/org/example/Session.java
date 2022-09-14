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
    private final Clock clock = Clock.systemUTC();
    ReentrantLock locker = new ReentrantLock();

    public Session(Socket clientSocket) throws IOException {

        connection = new ClientsConnection(
                new BufferedReader(new InputStreamReader(clientSocket.getInputStream())),
                new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())));
        //System.out.println(connection.getIn().readLine());
    }

    @Override
    public void run() {
        //Thread printSpeedThread = new Thread(this::printAvgSpeed);
        //System.out.println("Вывод скорости запущен");
        Thread clientReaderThread = new Thread(this::clientReader);
        System.out.println("Слушатель клиента запущен");
        clientReaderThread.start();
        //printSpeedThread.start();
        try {
            clientReaderThread.join();
            System.out.println("Соединение зарывается");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //printSpeedThread.interrupt();
        }
    }

    private void printAvgSpeed(){
        while (true){
            try {
                locker.lock();
                System.out.println("avg speed: " + connection.getAvg_rec_speed() + " bite?/sec");
                System.out.println("inst speed: "+connection.getInst_rec_speed() + " bite?/sec");
                Thread.sleep(3000);
                if(Thread.interrupted()){
                    return;
                }
            } catch (InterruptedException e) {
                return;
            }finally {
                locker.unlock();
            }
        }
    }

    private void clientReader(){
        FileWriter writer = null;
        //while(true) {
            String word = null;
            Instant begin = null;
            try {
                try {
                    long fileSize=0;
                    System.out.println("Ожмдание данных");
                    word = connection.getIn().readLine();
                    while (word != null && !word.isEmpty()) {       ///// бесконечный вывод косячно считается размер сообщения
                        System.out.println(" Считывание данных");
                        if (word.equals("exit")) {
                            return;
                        }
                        if (word.startsWith("ClientFileInfo:")){
                            String [] parsedMsg = word.split(":");  // ClientFileInfo:fileName:msgSize
                            System.out.println("Получен заголовок: " + word);
                            connection.setFileName(parsedMsg[1]);
                            connection.setFileSize(Long.parseLong(parsedMsg[2]));
                            writer = openFile();
                            begin = clock.instant();
                            word = connection.getIn().readLine();
                            continue;
                        }

                        if (writer != null) {
                            writer.write(word+"\n");
                            writer.flush();
                            //fileSize+=word.getBytes(StandardCharsets.UTF_8).length + "%n".getBytes(StandardCharsets.UTF_8).length;
                        }else{
                            System.out.println("Error in the format of the message sent by the client");
                            return;
                        }
                        fileSize+=word.getBytes(StandardCharsets.UTF_8).length + "%n".getBytes(StandardCharsets.UTF_8).length;
                        System.out.println(word);
                        word = connection.getIn().readLine();
                    }
                    if(fileSize != connection.getFileSize() ){
                        System.out.println("Ошибка при передачи, теоретический и фактический размер файла не совпадают");
                        throw new IOException();
                    }
                    clientWriter("success\n");
                    if(begin!=null) {
                        locker.lock();
                        connection.setInst_rec_speed((double)fileSize / Duration.between(begin, clock.instant()).toMillis() * 1000);  // в секунду, добавить блокировку
                        if(connection.getAvg_rec_speed() == 0){
                            connection.setAvg_rec_speed(connection.getInst_rec_speed());
                        }else {
                            connection.setAvg_rec_speed((connection.getAvg_rec_speed() + connection.getInst_rec_speed()) / 2);
                        }
                        locker.unlock();
                    }
                } catch (IOException e) {
                    clientWriter("failure\n");
                }
            }catch (ClientWriterException e) {
                System.out.println(e.getMessage());
                //return;
            }
       // }
    }

    private FileWriter openFile(){
        FileWriter writer;
        int i = 0;
        while (true){
            try{
                File file = new File("C:\\Users\\zerta\\IdeaProjects\\Network_lab2\\src\\main\\resources\\"+connection.getFileName()+i);
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
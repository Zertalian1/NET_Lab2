package org.example;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

//Клиенту передаётся в параметрах относительный или абсолютный путь к файлу, который нужно отправить.
//Клиенту также передаётся в параметрах DNS-имя (или IP-адрес) и номер порта сервера.
public class Clients {
    private static Socket clientSocket; //сокет для общения
    private static BufferedInputStream in; // поток чтения из сокета
    private static BufferedOutputStream out; // поток записи в сокет

    private static final int headerSize = 1024;

    public static void main(String[] args) {    //файл, ip, порт
        Clients client = new Clients();
        client.startClient(new File(args[0]), args[1], Integer.parseInt(args[2]) );
    }

    public void startClient(File file, String address, int port ) {
        try {
            try {
                clientSocket = new Socket(address, port);
                in = new BufferedInputStream(clientSocket.getInputStream());
                out = new BufferedOutputStream(clientSocket.getOutputStream());
                serverComm(file);
            } finally {
                System.out.println("Клиент был закрыт...");
                clientSocket.close();
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private byte[] createHeader(File file){
        ByteBuffer header = ByteBuffer.allocate(headerSize);
        header.put("ClientFileInfo:".getBytes(StandardCharsets.UTF_8));
        header.put(file.getName().getBytes(StandardCharsets.UTF_8));
        header.put(":".getBytes(StandardCharsets.UTF_8));
        header.put(String.valueOf(file.length()).getBytes(StandardCharsets.UTF_8));
        header.put("#".getBytes(StandardCharsets.UTF_8));
        return header.array();
    }

    private void sendFile(File file) throws IOException {
        InputStream fin = new FileInputStream(file);
        out.write(createHeader(file));
        out.flush();
        long totalSize = 0;
        int size;
        while (true){
            byte [] data = new byte[1024];
            if((size=fin.read(data))==-1){
                break;
            }
            totalSize+=size;
            out.write(data,0, size);
            out.flush();
        }
        out.close();
        fin.close();
        System.out.println(totalSize);
    }

    private void serverComm(File file){
        try {
            int sendAttempts = 0;
            String serverAnswer = null;
            do {
                sendAttempts+=1;
                System.out.println("Попытка отправить файл");
                sendFile(file);
                byte[] answer = new byte[10];
                if(in.read(answer)!=-1){
                    serverAnswer = new String(answer, StandardCharsets.UTF_8);
                }else{
                    System.out.println("Answer ERROR");
                    return;
                }
                System.out.println(serverAnswer);
            }while(serverAnswer.startsWith("failure") && sendAttempts<3);
            in.close();
            if(serverAnswer.equals("failure")){
                System.out.println("Can't send file" + file.getName());
                return;
            }
            System.out.println("File was sent");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}

package org.example;

import java.io.*;
import java.net.Socket;

//Клиенту передаётся в параметрах относительный или абсолютный путь к файлу, который нужно отправить.
//Клиенту также передаётся в параметрах DNS-имя (или IP-адрес) и номер порта сервера.
public class Clients {
    private static Socket clientSocket; //сокет для общения
    private static BufferedReader in; // поток чтения из сокета
    private static BufferedWriter out; // поток записи в сокет

    public static void main(String[] args) {    //файл, ip, порт
        Clients client = new Clients();
        client.startClient(new File(args[0]), args[1], Integer.parseInt(args[2]) );
    }

    public void startClient(File file, String address, int port ) {
        try {
            try {
                clientSocket = new Socket("localhost", port);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                // достать из пути имя файла
                // считать файл в память или как - то иначе узнать его размер
                // отправить файл, получить его ответ, если успех, то ?
                //                                     если провал, то 3 отправки
                serverComm(file);
            } finally {
                System.out.println("Клиент был закрыт...");
                clientSocket.close();
                in.close();
                out.close();
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void sendFile(File file) throws IOException {
        BufferedReader readFile = new BufferedReader(new FileReader(file)); // ClientFileInfo:fileName:msgSize
        out.write("ClientFileInfo:" + file.getName() + ":" + file.length() + "\n");
        out.flush();
        char [] data = new char[1024];
        while (readFile.read(data)!=-1){
            out.write(data);
            out.flush();
            for(int i = 0; i< 1024;i++){
                data[i] = '\u0000';
            }
        }
        out.close();
        readFile.close();
    }

    private void serverComm(File file){
        try {
            int sendAttempts = 0;
            String serverAnswer = null;
            boolean isFileSent;
            do {
                sendAttempts+=1;
                System.out.println("Попытка отправить файл");
                sendFile(file);
                serverAnswer = in.readLine();
                System.out.println(serverAnswer);
            }while(serverAnswer.startsWith("failure") && sendAttempts<3);
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

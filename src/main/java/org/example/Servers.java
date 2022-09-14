package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Clock;
import java.util.concurrent.CopyOnWriteArrayList;

//Серверу передаётся в параметрах номер порта, на котором он будет ждать входящих соединений от клиентов.
public class Servers {
    private final int port;
    private final Clock clock = Clock.systemUTC();
    private final CopyOnWriteArrayList<Thread> sessions = new CopyOnWriteArrayList<>();


    public Servers(int port) {
        this.port = port;
    }

    public static void main(String[] args) {    // порт
        Servers server = new Servers(Integer.parseInt(args[0]));
        server.startServer();
    }

    public void startServer(){
        Thread clientListener = new Thread(this::addConnection);
        Thread clientConnectionClose = new Thread(this::closeConnection);
        clientListener.start();
        clientConnectionClose.start();
        try {
            clientListener.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        clientConnectionClose.interrupt();
    }

    private void addConnection(){
        try (ServerSocket socket = new ServerSocket(port)){
            System.out.println("Сервер запущен!");
            while (true) {
                Socket clientSocket = socket.accept();
                System.out.println("Новое подключение!");
                Thread session = new Thread(new Session(clientSocket));
                session.start();
                sessions.add(session);

            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void closeConnection(){
        sessions.removeIf(s -> !s.isAlive());
    }
}

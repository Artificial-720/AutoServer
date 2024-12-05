package me.artificial.autoserver;

import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class CommandSocketClient {

    private final Logger logger;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public CommandSocketClient(Logger logger) {
        this.logger = logger;
    }

    public boolean startConnection(InetAddress ip) {
        return startConnection(ip, 3333);
    }

    public boolean startConnection(InetAddress ip, int port) {
        try {
            clientSocket = new Socket(ip, port);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            return true;
        } catch (IOException e) {
            logger.error("Error starting connection {}", e.getMessage());
            return false;
        }
    }

    public String sendMessage(String msg) {
        out.println(msg);
        String resp = null;
        try {
            resp = in.readLine();
        } catch (IOException e) {
            logger.error("Error reading message {}", e.getMessage());
        }
        return resp;
    }

    public void stopConnection() {
        try {
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            logger.error("Error closing connection {}", e.getMessage());
        }
    }
}

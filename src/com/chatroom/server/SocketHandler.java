package com.chatroom.server;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

import static com.chatroom.server.Server.getClients;

public class SocketHandler extends Thread
{
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private TextArea inputTextArea;
    private TextFlow onlineUserArea, chatTextArea;
    private String username;

    public SocketHandler(Socket socket, TextFlow chatTextArea, TextArea inputTextArea, TextFlow onlineUsersArea, String username)
    {
        this.socket = socket;
        this.chatTextArea = chatTextArea;
        this.inputTextArea = inputTextArea;
        this.onlineUserArea = onlineUsersArea;
        this.username = "";
    }

    public void run()
    {
        try
        {
            setUpStreams();
            whileChatting();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            //closeConnection();
        }

    }

    /**
     * Set up stream to send and retrieve data
     */
    private void setUpStreams() throws IOException
    {

        output = new ObjectOutputStream(socket.getOutputStream());
        output.flush();

        input = new ObjectInputStream(socket.getInputStream());

        getClients().add(this);
    }

    /**
     * Receives messages
     */
    private void whileChatting() throws IOException
    {
        String message = "";
        ObjectOutputStream tempOutput;
        ArrayList<SocketHandler> clients = getClients();
        ableToType(true);

        do
        {
            try
            {
                message = (String) input.readObject();

                if (message.startsWith("Œ"))
                {
                    processCommand(message);
                }
                else
                {
                    for (int i = 0; i < clients.size(); i++)
                    {
                        tempOutput = clients.get(i).getOutput();
                        tempOutput.writeObject(message);
                        tempOutput.flush();
                    }
                    chatTextArea.setStyle("-fx-text-fill: RED");
                    showMessage(message + "\n");
                }
            }
            catch (ClassNotFoundException e)
            {
                showMessage("\nUser didn't send a string");
            }
            catch (SocketException e)
            {

            }
        }
        while (!message.contains("LOLOLOLOASDAD"));

        for (int i = 0; i < clients.size(); i++)
        {
            tempOutput = clients.get(i).getOutput();
            tempOutput.writeObject(username + " has disconnected");
            tempOutput.flush();
        }
        updateUsers();
    }

    private void processCommand(String command) throws IOException
    {
        ArrayList<SocketHandler> clients = getClients();
        ObjectOutputStream tempOutput;

        if (command.contains("START_CONNECTION"))
        {
            username = command.split(";")[1];

            for (int i = 0; i < clients.size(); i++)
            {
                tempOutput = clients.get(i).getOutput();
                tempOutput.writeObject(username + " has connected");
                tempOutput.flush();
            }
            updateUsers();
            showMessage(username + " has connected\n");
        }
        else if (command.contains("END_CONNECTION"))
        {
            for (int i = 0; i < clients.size(); i++)
            {
                tempOutput = clients.get(i).getOutput();
                tempOutput.writeObject(username + " has disconnected");
                tempOutput.flush();
            }
            showMessage(username + " has disconnected\n");
            clients.remove(this);
            closeConnection();
            updateUsers();

            if (clients.size() == 0)
            {
                ableToType(false);
            }
        }
    }

    private void closeConnection()
    {
        try
        {
            output.close();
            input.close();
            socket.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public ObjectOutputStream getOutput()
    {
        return output;
    }

    public String getUsername()
    {
        return username;
    }

    /**
     * Updates the chat window
     */
    private void showMessage(final String text)
    {
        Text t1 = new Text(text);
        Platform.runLater(() -> chatTextArea.getChildren().add(t1));
    }

    /**
     * Updates user list
     */
    private void updateUsers() throws IOException
    {
        Platform.runLater(() -> onlineUserArea.getChildren().clear());
        ArrayList<SocketHandler> clients = getClients();
        ObjectOutputStream tempOutput;
        String users = "";

        for (int i = 0; i < getClients().size(); i++)
        {
            int finalI = i;
            Text t1 = new Text(getClients().get(finalI).getUsername() + "\n");
            Platform.runLater(() -> onlineUserArea.getChildren().add(t1));
            users = users + getClients().get(finalI).getUsername() + "\n";
        }

        for (int i = 0; i < clients.size(); i++)
        {
            tempOutput = clients.get(i).getOutput();
            tempOutput.writeObject("Œ" + "USER_LIST:" + users);
            tempOutput.flush();
        }

    }

    /**
     * Allows and disallows typing
     */
    public void ableToType(final boolean allowed)
    {
        Platform.runLater(() -> inputTextArea.setEditable(allowed));
    }
}

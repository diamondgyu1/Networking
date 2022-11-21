import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server 
{
    public static void main(String[] args) throws Exception
    {
        int port1 = Integer.parseInt(args[0]);
        int port2 = Integer.parseInt(args[1]);
        ServerSocket ss = new ServerSocket(port1);
        ServerSocket ss2 = new ServerSocket(port2);
        ArrayList<Chatroom> chatrooms = new ArrayList<>();

        while(true)
        {
            ClientInstance ci = new ClientInstance(ss);
            String[] start_command = ci.recieve().split(" ");
            if (start_command[0].equals("#CREATE"))
            {
                Chatroom chatroom = new Chatroom(start_command[1]);
                chatroom.clients.add(ci);
                chatrooms.add(chatroom);
                ci.chatroom_code = chatrooms.size()-1;
                ci.clientName = start_command[2];
                System.out.println("Client Joined: chatroom "+start_command[1]+", name "+ci.clientName);
            }
            else if (start_command[1].equals("#JOIN"))
            {
                for(int i = 0; i<chatrooms.size(); i++)
                {
                    if (chatrooms.get(i).chatroomName.equals(start_command[1]))
                    {
                        ci.chatroom_code = i;
                        ci.clientName = start_command[2];
                        chatrooms.get(i).clients.add(ci);
                        System.out.println("Client Joined: chatroom "+start_command[1]+", name "+ci.clientName);
                        break;
                    }
                }
            }
            else
            {
                // do nothing
            }

        }
    }
}

class Chatroom
{
    String chatroomName;
    ArrayList<ClientInstance> clients = new ArrayList<>();

    public Chatroom(String chatroomName)
    {
        this.chatroomName = chatroomName;
    }
}

class ClientInstance
{
    private Socket socket;
    private BufferedReader br;
    private PrintStream ps;
    public String clientName;
    public int chatroom_code;

    public ClientInstance(ServerSocket ss)
    {
        try
        {
            socket = ss.accept();
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            ps = new PrintStream(socket.getOutputStream());
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        
    }

    public String recieve()
    {
        // System.out.println("recieve");
        try
        {
            String msg = br.readLine();
            return msg;
        } catch(Exception e)
        {
            e.printStackTrace();
            return "Fail";
        }
    }

    public boolean send(String msg)
    {
        try
        {
            System.out.println("ClientInstance: "+msg);
            ps.println(msg);
            return true;
        } catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }
}

class ClientThread extends Thread
{
    ClientInstance ci;
    ArrayList<ClientInstance> chatroom;

    public ClientThread(ServerSocket ss, ArrayList<ClientInstance> chatroom)
    {
        this.ci = new ClientInstance(ss);
        this.chatroom = chatroom;
        System.out.println("New Client Joined");
    }

    @Override
    public void run()
    {
        while(true)
        {
            String message = ci.recieve();

            // if (message == null)
            // {
            //     Thread.sleep(100);
            // }
            System.out.println("ClientThread: "+message);

            System.out.println(chatroom.size());

            // ci.send(message);
            
            for(ClientInstance ci : chatroom)
            {
                ci.send(message);
            }
        }
    }
}

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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

            // Loop until gets either CREATE, JOIN or EXIT
            while(true)
            {
                String[] start_command = ci.recieve().split(" ");
                if (start_command[0].equals("#CREATE"))
                {
                    // 챗방객체 생성
                    Chatroom chatroom = new Chatroom(start_command[1]);

                    // 챗방 목록에 추가
                    chatrooms.add(chatroom);

                    // ClientInstance 설정하고 챗방에 추가
                    ci.chatroom_code = chatrooms.size()-1;
                    ci.clientName = start_command[2];
                    chatroom.clients.add(ci);
                    
                    // 생성한 인스턴스를 담당하는 thread 시작
                    ClientThread ct = new ClientThread(ss, chatroom, ci);
                    ct.start();
                    System.out.println("Client Joined: in chatroom "+start_command[1]+", name "+ci.clientName);
                    break;
                }
                else if (start_command[0].equals("#JOIN"))
                {
                    int i = 0;
                    boolean success = false;
                    for(i = 0; i<chatrooms.size(); i++)
                    {
                        // 같은 이름을 가지는 챗방 발견
                        // System.out.println(chatrooms.get(i).chatroomName);
                        // System.out.println(start_command[1]+"\n");
                        // System.out.print(start_command[1]);
                        // System.out.print("  ");
                        // System.out.println(chatrooms.get(i).chatroomName);
                        if (chatrooms.get(i).chatroomName.equals(start_command[1]))
                        {
                            // System.out.println("Found Chatroom "+start_command[1]);
                            // 생성한 ClientInstance를 그 챗방의 client 목록에 추가
                            ci.chatroom_code = i;
                            ci.clientName = start_command[2];
                            chatrooms.get(i).clients.add(ci);
                            
                            // 생성한 인스턴스를 담당하는 thread 시작
                            ClientThread ct = new ClientThread(ss, chatrooms.get(i), ci);
                            ct.start();
                            System.out.println("Client Joined: chatroom "+start_command[1]+", name "+ci.clientName);
                            success = true;

                            break;
                        }
                    }


                    if (!success)
                    {
                        ci.send("MESSAGE");
                        ci.send("No Such Room!!");
                        continue;
                    }
                
                    break;
                }
                else if (start_command[0].equals("#EXIT"))
                {
                    break;
                }
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
    private DataInputStream dis;
    private DataOutputStream dos;
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

    public String recieve() throws Exception
    {
            String msg = br.readLine();
            return msg;
    }

    public String send(String msg)
    {
        try
        {
            ps.println(msg);
            return "Success";
        } catch(Exception e)
        {
            // e.printStackTrace();
            return "Fail";
        }
    }

    public void recieve_file()
    {
        try
        {
            dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            String fileName = dis.readUTF();
            File file = new File("server-"+fileName);
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            byte[] buffer = new byte[64];
            int count;
            long fileSize = dis.readLong();
            long data = 0;
            while((count=dis.read(buffer)) > 0)
            {
                fos.write(buffer, 0, count);
                data += count;
                if(data == fileSize) break;
                System.out.print("#");
            }
            System.out.println();
            fos.close();
            // dis.close();
            System.out.println("File saved in server: "+fileName);
        }
        catch(Exception e) {}
    }

    public void send_file(String fileName)
    {
        try
        {
            dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            File file = new File("server-"+fileName);
            dos.writeUTF(fileName);
            dos.writeLong(file.length());
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[64];
            int count;

            while((count=fis.read(buffer)) > 0)
            {
                dos.write(buffer, 0, count);
                dos.flush();
                System.out.print("#");
            }
            System.out.println("\nFile Sent: "+fileName+" to client "+clientName);
            fis.close();
        }
        catch(Exception e) {}


    }
}

class ClientThread extends Thread
{
    ClientInstance ci;
    Chatroom chatroom;

    public ClientThread(ServerSocket ss, Chatroom chatroom, ClientInstance ci)
    {
        this.ci = ci;
        this.chatroom = chatroom;
        ci.send("MESSAGE");
        ci.send("joined "+chatroom.chatroomName);
        //System.out.println("New Client Joined");
    }

    //Gather data and commands from client, with loop
    @Override
    public void run()
    {
        while(true)
        {
            try
            {
                String message = ci.recieve();
                String[] msg_split = message.split(" ");

                if (msg_split[0].equals("#STATUS"))
                {
                    
                    ci.send("MESSAGE");
                    String members = "";
                    for(ClientInstance c : chatroom.clients)
                    {
                        members += c.clientName;
                        members += ", ";
                    }
                    //System.out.println("Chatroom Name: "+chatroom.chatroomName+", Members: "+members);
                    ci.send("Chatroom Name: "+chatroom.chatroomName+", Members:"+members);
                    continue;
                }
                if (msg_split[0].equals("#EXIT"))
                {
                    System.out.println("Client Quitted: in chatroom "+chatroom.chatroomName+", name "+ci.clientName);
                    break;
                }
                if (msg_split[0].equals("#PUT"))
                {
                    // 파일송수신
                    System.out.println("Receiving File from Client "+ci.clientName+" of chatroom "+chatroom.chatroomName+": "+msg_split[1]);
                    ci.recieve_file();
                    
                    continue;
                }
                if (msg_split[0].equals("#GET"))
                {
                    System.out.println("Sending File from Client "+ci.clientName+" of chatroom "+chatroom.chatroomName+": "+msg_split[1]);
                    ci.send("FILE");
                    ci.send_file(msg_split[1]);
                }

                if (message.split("")[0].equals("#")) continue;

                System.out.println(chatroom.chatroomName+"("+ci.clientName+"): "+message);
                
                for(int i=0; i<chatroom.clients.size(); i++)
                {
                    chatroom.clients.get(i).send("MESSAGE");
                    String result = chatroom.clients.get(i).send("FROM "+ci.clientName+": "+message);
                    System.out.println("Sent Message to "+chatroom.clients.get(i).clientName+": "+result);
                }
            } catch(Exception e)
            {
                e.printStackTrace();
                break;
            }
        }
    }
}
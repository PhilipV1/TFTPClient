import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class TFTPClient {

    private boolean stop = false;
    public static final int TFTPPORT = 4970;
    public static final int TFTPCLIENT = 226;
    public static final int BUFFERSIZE = 516;
    public static final String WRITEDIR = "write/";
    public static final short OP_RRQ = 1;
    public static final short OP_WRQ = 2;
    public static final short OP_DAT = 3;
    public static final short OP_ACK = 4;
    public static final short OP_ERR = 5;
    public static final int HEADER = 4;

    public static final String MODE = "octet";
    public static final String FILENAME = "shortfile.txt";

    public static void main(String[] args) {
        try {
            TFTPClient client = new TFTPClient();
            client.start();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private void start() throws SocketException {

        try {
            DatagramSocket socket = new DatagramSocket(null);
            SocketAddress localAddress = new InetSocketAddress(TFTPCLIENT);
            socket.bind(localAddress);
            sendRequest(socket);
            while (!stop) {
                testRetransmission(socket);
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void testRetransmission(DatagramSocket socket) {
        receiveData(socket);
    }

    private void sendRequest(DatagramSocket socket) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(BUFFERSIZE);
            buffer.putShort(OP_RRQ);
            buffer.put(FILENAME.getBytes(StandardCharsets.UTF_8));
            buffer.put((byte) 0);
            buffer.put(MODE.getBytes(StandardCharsets.UTF_8));
            buffer.put((byte) 0);
            DatagramPacket packet = new DatagramPacket(buffer.array(), buffer.array().length);
            packet.setPort(TFTPPORT);
            InetAddress local = InetAddress.getLocalHost();
            packet.setAddress(local);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private void receiveData(DatagramSocket socket, String fileName) {
        short blockID = -1;
        boolean receiving = true;
        FileOutputStream fileOutput = createFile();
        while (receiving) {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(BUFFERSIZE);
                DatagramPacket packet = new DatagramPacket(buffer.array(), buffer.array().length);
                socket.receive(packet);
                ByteBuffer received_data = ByteBuffer.wrap(packet.getData());
                // Stop receiving if the packet is less than 516 bytes
                receiving = packet.getLength() != BUFFERSIZE;
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
                System.out.println("Received array length: " + packet.getLength());
                blockID = received_data.getShort(2);

                // Start reading data after the initial 4 bytes
                received_data.position(4);
                ByteBuffer wrapper = ByteBuffer.wrap(data);


                // Send the acknowledgement of the received packet
                sendACK(socket, blockID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        fileOutput.close();
    }

    private FileOutputStream createFile() {
        try {
            File file = new File(WRITEDIR + FILENAME);
            file.createNewFile();
            FileOutputStream tempStream = new FileOutputStream(file);
        } catch (IOException e) {

        }
    }
    private void sendACK(DatagramSocket socket, short blockID) throws IOException {
        ByteBuffer ackBuffer = ByteBuffer.allocate(HEADER);
        ackBuffer.putShort(OP_ACK);
        ackBuffer.putShort(blockID);

        DatagramPacket packet = new DatagramPacket(ackBuffer.array(), ackBuffer.array().length);
        socket.send(packet);
    }

    private void writeToFile(StringBuilder data) {
        try {
            File file = new File("write/" + FILENAME);
            FileWriter writer = new FileWriter(file);
            writer.write(data.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
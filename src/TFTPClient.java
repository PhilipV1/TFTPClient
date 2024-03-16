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
    public static final String FILENAME = "rfc1350.txt";

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

            testRetransmission(socket);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void testRetransmission(DatagramSocket socket) {
        boolean result = receiveData(socket);
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
    private boolean receiveData(DatagramSocket socket) {
        short blockID = 1;
        boolean receiving = true;
        boolean firstPacket = true;
        try {
            File file = new File(WRITEDIR + FILENAME);
            file.createNewFile();
            FileOutputStream fileOutput = new FileOutputStream(file);
            while (receiving) {
                ByteBuffer buffer = ByteBuffer.allocate(BUFFERSIZE);
                DatagramPacket packet = new DatagramPacket(buffer.array(), buffer.array().length);
                socket.receive(packet);
                if (firstPacket) {
                    socket.connect(packet.getAddress(), packet.getPort());
                    firstPacket = false;
                }
                ByteBuffer received_data = ByteBuffer.wrap(packet.getData());
                // Stop receiving if the packet is less than 516 bytes
                receiving = packet.getLength() == BUFFERSIZE;
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
                short opcode = received_data.getShort();

                if (opcode == OP_ERR) {
                    System.out.println("ERROR Received");
                    fileOutput.close();
                    return false;
                }
                if (blockID == received_data.getShort()) {
                    System.out.println("Received packet length: " + packet.getLength()
                        + " | Block number: " + received_data.getShort(2));
                    // Start reading data after the initial 4 bytes
                    received_data.position(4);
                    ByteBuffer wrapper = ByteBuffer.wrap(data);

                    // Wrap the data we want to write to the file
                    byte[] fileData = new byte[wrapper.array().length - 4];
                    wrapper.position(4);
                    wrapper.get(fileData, 0, fileData.length);
                    fileOutput.write(fileData);

                    // Send the acknowledgement of the received packet
                    Thread.sleep(1500);
                    sendACK(socket, blockID);
                    blockID++;
                }

            }
            fileOutput.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return true;
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
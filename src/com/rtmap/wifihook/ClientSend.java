package com.rtmap.wifihook;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import java.util.TooManyListenersException;

import com.rtmap.wifihook.commonTools.DataFilter;
import com.rtmap.wifihook.commonTools.UdpPackage;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

/**
 * 构造参数 : Ip: 接受Ip Port: 端口 Mac: 识别 ID (mac地址 不带"_") timeout: 打开端口等待时间（单位 秒 默认
 * 2s selectPort("COM3"); 选择端口 startRead(); 读取数据
 *
 * @author zzc
 *
 */

public class ClientSend implements SerialPortEventListener {

	private String Ip;
	private int Port;
	private String Mac;

	private CommPortIdentifier commPort;
	private SerialPort serialPort;
	private InputStream inputStream;

	public ClientSend(String ip, int port, String Mac) {
		this.Ip = ip;
		this.Port = port;
		this.Mac = Mac;
	}

	/**
	 * @方法名称 :selectPort
	 * @功能描述 :选择一个端口，比如：COM1
	 * @返回值类型 :void
	 * @param portName
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	public void selectPort(String serialPortName) {

		this.commPort = null;
		CommPortIdentifier cpid;
		Enumeration en = CommPortIdentifier.getPortIdentifiers();

		while (en.hasMoreElements()) {
			cpid = (CommPortIdentifier) en.nextElement();
			if (cpid.getPortType() == CommPortIdentifier.PORT_SERIAL && cpid.getName().equals(serialPortName)) {
				this.commPort = cpid;
				break;
			}
		}

		openPort(serialPortName);
	}

	/**
	 * @throws Exception
	 * @方法名称 :openPort
	 * @功能描述 :打开SerialPort
	 * @返回值类型 :void
	 */
	private void openPort(String serialPortName) {
		if (commPort == null) {
			System.out.println("Unable to find serial port whose name is " + serialPortName);
			System.exit(0);
		} else {
			System.out.println(
					"Port selection succeeded. Current port:" + commPort.getName() + ",Now instantiate SerialPort:");
			try {
				serialPort = (SerialPort) commPort.open(Object.class.getSimpleName(), 2000);
				System.out.println("open SerialPort is successful!");
				// 设置串口通讯参数
				// 波特率，数据位，停止位和校验方式
				// 波特率115200,偶校验 波特率不对将会乱码
				serialPort.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
						SerialPort.PARITY_NONE);

			} catch (PortInUseException | UnsupportedCommOperationException e) {
				throw new RuntimeException("The port (" + commPort.getName() + ") is in use!");
			}
		}
	}

	/**
	 * @方法名称 :startRead
	 * @功能描述 :开始监听从端口中接收的数据
	 * @返回值类型 :void
	 * @param time
	 *            监听程序的存活时间，单位为秒，0 则是一直监听
	 */
	public void startRead() {

		try {
			inputStream = new BufferedInputStream(serialPort.getInputStream());
		} catch (IOException e) {
			throw new RuntimeException("Error getting port InputStream:" + e.getMessage());
		}

		try {
			serialPort.addEventListener(this);
		} catch (TooManyListenersException e) {
			throw new RuntimeException(e.getMessage());
		}

		serialPort.notifyOnDataAvailable(true);

		System.out.println("Start monitoring data from " + serialPort.getName());

	}

	/**
	 * 数据接收的监听处理函数
	 */
	@Override
	public void serialEvent(SerialPortEvent arg0) {
		switch (arg0.getEventType()) {
		case SerialPortEvent.BI:/* Break interrupt,通讯中断 */
		case SerialPortEvent.OE:/* Overrun error，溢位错误 */
		case SerialPortEvent.FE:/* Framing error，传帧错误 */
		case SerialPortEvent.PE:/* Parity error，校验错误 */
		case SerialPortEvent.CD:/* Carrier detect，载波检测 */
		case SerialPortEvent.CTS:/* Clear to send，清除发送 */
		case SerialPortEvent.DSR:/* Data set ready，数据设备就绪 */
		case SerialPortEvent.RI:/* Ring indicator，响铃指示 */
		case SerialPortEvent.OUTPUT_BUFFER_EMPTY:/*
													 * Output buffer is
													 * empty，输出缓冲区清空
													 */
			break;
		case SerialPortEvent.DATA_AVAILABLE:/*
											 * Data available at the serial
											 * port，端口有可用数据。读到缓冲数组，输出到终端
											 */
			try {
				readComm();
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * 
	 * @throws SocketException
	 */
	public void readComm() throws SocketException {
		int readLenth = 2048;
		byte[] readBuffer = new byte[readLenth];
		DatagramSocket socket = new DatagramSocket();
		try {
			// 从线路上读取数据流
			int len = 0;

			while ((len = inputStream.read(readBuffer)) != -1) {

				// 将都出来的数据进行过滤 返回linkList集合 集合内存放单条的字符串数据 例如：
				// 48:45:20:69:3D:DB|70:BA:EF:14:8B:B0|02|0c|3|-83 为一条
				List<String> data = DataFilter.dataFilter(readBuffer);
				Short i = 0;
				for (String str : data) {
//					System.out.println("===>" + str); // 输出查看结果
					try {
						// 将每一条数据打包成udp报文 返回字节数组
						UdpPackage udp = new UdpPackage();
						byte[] b = udp.udpPackage(i, Mac, str);
						DatagramPacket request = new DatagramPacket(b, b.length, InetAddress.getByName(Ip), Port);
						socket.send(request);
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (i == Short.MAX_VALUE) {
						i = 0;
					} else {
						i++;
					}
				}
				break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// 检查字符串是否为空或者null
	public static boolean check(String arg) {
		return (arg == null || "".equals(arg));
	}

	public static void main(String[] args) {
		int argLenth = 4;
		if (args.length < argLenth || args.length > argLenth+1) {
			System.out.println(
					"Incorrect number of parameters! Please input in the following format: ip,port,mac,portName");
			return;
		}
		String ip = args[0];
		String port = args[1];
		String mac = args[2];
		String portName = args[3];

		if (check(ip) || check(port) || check(mac) || check(portName)) {
			System.out.println(
					"Incorrect input of parameter format!");
		} else {
			ClientSend dp = new ClientSend(ip, Integer.parseInt(port), mac);

			dp.selectPort(portName);// "COM3"
			// 开始读串口
			dp.startRead();

			/**
			 * 注意的是windows版本不一样可能会出现无法读取数据，导致监听事件触发不了
			 * 所以加入join函数，保证监听函数执行完才会让主程序执行，这样就不会让监听程序被阻断
			 */
			try {
				Thread.currentThread().join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}

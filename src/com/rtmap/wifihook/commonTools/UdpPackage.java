package com.rtmap.wifihook.commonTools;

public class UdpPackage {

	/**
	 * 
	 * @param requestId    请求Id   
	 * @param mac			lbsid
	 * @param result	传入过滤后的探针读取信息 分条的字符 例如：  48:45:20:69:3D:DB|70:BA:EF:14:8B:B0|02|0c|3|-83   为一条result 
	 * @return   返回打包成udp报文的字符数组
	 */

	public byte[] udpPackage(short requestId, String mac, String result) {
		String libsid = mac;
		int libsidLenth =6; 
		byte[] spi = new byte[libsidLenth];
		byte[] tspi = toBytes(libsid);
		System.arraycopy(tspi, 0, spi, 0, 6);
		byte[] b = toInternet(requestId, spi, result.split("\\|"));
		return b;
	}

	/**
	 * 打包成udp报文
	 *
	 * @param short
	 *            requestId,  请求ID
	 *            
	 *            byte[] apMac, mac转换成的字节数组
	 *             
	 *            String[] result 存放的是 “ 48:45:20:69:3D:DB|70:BA:EF:14:8B:B0|02|0c|3|-83 ”  根据 | split的数组 
	 * 
	 * @return byte[]
	 */
	private byte[] toInternet(short requestId, byte[] apMac, String[] result) {
		int udpLenth = 48 ; 
		byte[] finalData = new byte[udpLenth];
		byte[] header = new byte[] { (byte) 0xcc, (byte) 0x83 };
		byte code = (byte) 0xD6;
		byte subCode = (byte) 0x0;
		short dataLen = 40;
		short vid = 10;

		byte radioType = 0x0;
		byte channel = 0x0;

		byte muType = 0x01;
		byte noise = 0;
		short age = 0;
		int muIPv4 = 0;
		long reserved = 0;

		System.arraycopy(header, 0, finalData, 0, 2);
		System.arraycopy(shortToByteArray(requestId), 0, finalData, 2, 2);

		finalData[4] = code;
		finalData[5] = subCode;
		// datalen
		System.arraycopy(shortToByteArray(dataLen), 0, finalData, 6, 2);
		// apmac
		System.arraycopy(apMac, 0, finalData, 8, 6);
		// vid
		System.arraycopy(shortToByteArray(vid), 0, finalData, 14, 2);
		// mumac
		String muMac = result[0].replace(":", "");
		if (muMac.length() < 12)
			return null;
		System.arraycopy(toBytes(muMac), 0, finalData, 16, 6);
		// radioType
		finalData[22] = radioType;
		// channel
		finalData[23] = Byte.parseByte(result[4]);
		// is associated
		if (result[1].replace(":", "").equals("FFFFFFFFFFFF")) {
			finalData[24] = 0x02;
			System.arraycopy(toBytes("000000000000"), 0, finalData, 25, 6);
		} else {
			finalData[24] = 0x01;
			String assMac = result[1].replace(":", "");
			System.arraycopy(toBytes(assMac), 0, finalData, 25, 6);
		}
		// ap mac
		// String assMac = result[1].replace(":", "");
		// System.arraycopy(toBytes(assMac), 0, finalData, 25, 6);
		// mutype
		finalData[31] = muType;
		try {
			Integer.parseInt(result[5]);
			// rssi
			finalData[32] = Byte.parseByte(result[5]);
		} catch (NumberFormatException e) {
			// e.printStackTrace();
		}

		// noise
		finalData[33] = noise;

		// age
		System.arraycopy(shortToByteArray(age), 0, finalData, 34, 2);
		// muipv4
		System.arraycopy(intToByteArray(muIPv4), 0, finalData, 36, 4);
		// reserved
		System.arraycopy(longToByteArray(reserved), 0, finalData, 40, 8);

		return finalData;
	}

	/**
	 * String to byte[]
	 *
	 * @param s
	 *            long
	 * @return byte[]
	 */
	public static byte[] toBytes(String str) {
		if (str == null || str.trim().equals("")) {
			return new byte[0];
		}
		byte[] bytes = new byte[str.length() / 2];
		for (int i = 0; i < str.length() / 2; i++) {
			String subStr = str.substring(i * 2, i * 2 + 2);
			bytes[i] = (byte) Integer.parseInt(subStr, 16);
		}

		return bytes;
	}

	/**
	 * long to byte[]
	 *
	 * @param long
	 *           
	 * @return byte[]
	 */
	public static byte[] longToByteArray(long s) {
		int longLenth = 8 ;  
		byte[] targets = new byte[longLenth];
		for (int i = 0; i < longLenth; i++) {
			int offset = (targets.length - 1 - i) * 8;//向右移8位
			targets[i] = (byte) ((s >>> offset) & 0xff);
		}
		return targets;
	}

	/**
	 * int to byte[]
	 *
	 * @param int
	 *           
	 * @return byte[]
	 */
	public static byte[] intToByteArray(int s) {
		int intLenth = 4 ;	 
		byte[] targets = new byte[intLenth];
		for (int i = 0; i < intLenth; i++) {
			int offset = (targets.length - 1 - i) * 8;//向右移8位
			targets[i] = (byte) ((s >>> offset) & 0xff);
		}
		return targets;
	}

	/**
	 * short to byte[]
	 *
	 * @param short
	 *           
	 * @return byte[]
	 */
	public static byte[] shortToByteArray(short s) {
		int shortLenth = 2; 
		byte[] targets = new byte[shortLenth];
		for (int i = 0; i < shortLenth; i++) {
			int offset = (targets.length - 1 - i) * 8; //向右移8位
			targets[i] = (byte) ((s >>> offset) & 0xff);
		}
		return targets;
	}
	
}

package com.rtmap.wifihook.commonTools;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataFilter {
	
	private static String start = "^[A-Fa-f0-9]|-|:|\\||\r|\n|\r\n";
	private static String end = "-[0-9]*[1-9][0-9]*$";
	private static String REGEX = "[a-fA-F0-9][a-fA-F0-9]:[a-fA-F0-9][a-fA-F0-9]:[a-fA-F0-9][a-fA-F0-9]:[a-fA-F0-9][a-fA-F0-9]:[a-fA-F0-9][a-fA-F0-9]:[a-fA-F0-9][a-fA-F0-9]\\|[a-fA-F0-9][a-fA-F0-9]:[a-fA-F0-9][a-fA-F0-9]:[a-fA-F0-9][a-fA-F0-9]:[a-fA-F0-9][a-fA-F0-9]:[a-fA-F0-9][a-fA-F0-9]:[a-fA-F0-9][a-fA-F0-9]\\|[0-9a-fA-F][0-9a-fA-F]\\|[0-9a-fA-F][0-9a-fA-F]\\|[0-9]*[0-9][0-9]*\\|-[0-9]*[0-9][0-9]*";

	public static List<String> dataFilter(byte[] readBuffer){
		Pattern p = Pattern.compile(REGEX);
		Pattern sPattern = Pattern.compile(start);
		Pattern ePattern = Pattern.compile(end);
		String stringBuffer = "";
		List<String> data = new LinkedList<>();
		String result = new String(readBuffer) + "";
//		System.out.println(result);
		String rs[] = result.replace("\r\n", "*&").replace("\r", "").replace("\n", "").split("&");
		System.out.println(Arrays.toString(rs));
		for (String r : rs) {
			if (!sPattern.matcher(r).lookingAt()) {
				continue;
			}
			stringBuffer += r;
		}

		Matcher m = p.matcher(stringBuffer);

		while (m.find()) {
			data.add(m.group());
			stringBuffer = stringBuffer.replace(m.group(), "");
		}
		// 替换完正则匹配的字符串后前面会有*占位，在这儿去除
		while (stringBuffer.indexOf("*") == 0) {
			stringBuffer = stringBuffer.substring(1, stringBuffer.length());
		}
		// 如有联系2个*出现则说明stringbuffer*之前的数据无法做匹配了，删除之
		while (stringBuffer.lastIndexOf("**") > 0) {
			stringBuffer = stringBuffer.substring(stringBuffer.lastIndexOf("**"), stringBuffer.length());
		}
		return data;
	}
}

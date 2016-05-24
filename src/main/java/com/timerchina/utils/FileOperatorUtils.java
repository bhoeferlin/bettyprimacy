package com.timerchina.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

public class FileOperatorUtils {

	private static Logger	logger	= Logger.getLogger(FileOperatorUtils.class);

	/**
	 * 
	 * @param filePathAndName
	 * @return
	 */
	public static String readFile(String filePathAndName, String charset) {
		StringBuffer sb = new StringBuffer("");

		BufferedReader br = null;
		InputStreamReader isr = null;
		try {
			if (null != charset)
				isr = new InputStreamReader(new FileInputStream(filePathAndName), charset);
			else
				isr = new InputStreamReader(new FileInputStream(filePathAndName));

			br = new BufferedReader(isr);
			String str = null;

			while ((str = br.readLine()) != null) {
				sb.append(str+"\r\n");
			}
		} catch (FileNotFoundException e) {
			logger.error("读取文件[" + filePathAndName + "]捕捉到异常", e);
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			logger.error("读取文件[" + filePathAndName + "]捕捉到异常", e);
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} finally {
			try {
				if (br != null)
					br.close();
				if (isr != null)
					isr.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.error("关闭文件[" + filePathAndName + "]捕捉到异常", e);
				e.printStackTrace();
				return null;
			}
		}
		return sb.toString();

	}

	/**
	 * create folder by the given name
	 * 
	 * @param folderPath
	 * @return
	 */
	@SuppressWarnings("finally")
	public static boolean createFolder(String folderPath) {
		boolean result = false;
		File newFolder = new File(folderPath);
		try {
			if (!newFolder.exists())
				result = newFolder.mkdirs();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("创建目录[" + folderPath + "]失败", e);
		} finally {
			return result;
		}

	}

	/**
	 * delete file
	 * 
	 * @param filePathAndName
	 * @return
	 */
	@SuppressWarnings("finally")
	public static boolean deleteFile(String filePathAndName) {
		boolean result = false;
		File delFile = new File(filePathAndName);
		try {
			if (delFile.exists()) {
				delFile.delete();
				result = true;
			} else {
				result = false;
				logger.warn("删除文件[" + filePathAndName + "]失败，该文件不存在");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error("删除文件[" + filePathAndName + "]失败");
			e.printStackTrace();
		} finally {
			return result;
		}
	}

	/**
	 * move the file from oldpath to newpath
	 * 
	 * @param filename
	 * @param oldpath
	 * @param newpath
	 * @param cover
	 */
	@SuppressWarnings("finally")
	public static boolean moveFile(String filename, String oldpath, String newpath, boolean cover) {
		File oldfile = new File(oldpath + File.separator + filename);
		File newfile = new File(newpath + File.separator + filename);
		if (!oldfile.exists())
			return false;
		if (!newfile.exists())
			createFolder(newpath);
		boolean result = false;
		try {
			if (!oldpath.equals(newpath)) {

				if (newfile.exists()) {// 若在待转移目录下，已经存在待转移文件
					if (cover)// 覆盖
					{
						deleteFile(newfile.getAbsolutePath());
						result = oldfile.renameTo(newfile);
					}
					else
						logger.warn("文件[" + filename + "]已存在");
				}
				else {
					result = oldfile.renameTo(newfile);
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("移动文件失败", e);
		} finally {
			return result;
		}
	}

	public static void write2File(String fileDirectoryAndName, String content) {
		FileWriter fw = null;
		try {
			String fileName = fileDirectoryAndName;
			File myFile = new File(fileName);
			if (!myFile.exists())
				myFile.createNewFile();
			else
				throw new Exception("the new file already exists!");
			fw = new FileWriter(myFile);
			fw.write(content);
		} catch (Exception e) {
			logger.error("创建文件出错！", e);
			e.printStackTrace();
		} finally {
			try {
				if (fw != null)
					fw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				logger.error("关闭文件失败", e);
			}
		}
	}

	public static void main(String[] args) {
		// boolean test=FileOperatorUtils.moveFile("1406442673523.txt",
		// "D:\\logs", "D:\\logs\\temp", true);
		String test = FileOperatorUtils.readFile("C:/Users/windows/Desktop/关键短语提取/in.txt", "utf-8");
		// System.out.println();
		System.out.println(test);
	}
}

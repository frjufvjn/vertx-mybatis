package com.hansol.isportal.restful.controller;

import com.hansol.isportal.crypto.CryptoException;
import com.hansol.isportal.crypto.CryptoUtils;
import com.hansol.isportal.dbservice.service.DbSvcService;
import com.hansol.isportal.dbservice.service.DbSvcServiceImpl;
import com.hansol.isportal.dbservice.service.MainService;
import com.hansol.isportal.hostservice.BigEndianByteHandler;
import com.hansol.isportal.hostservice.ByteBufferUtil;
import com.hansol.isportal.hostservice.LittleEndianByteHandler;
import com.hansol.isportal.hostservice.PBXSocketConnector;
import com.hansol.isportal.transcoding.ffmpeg.AudioFileTransCoding;
import com.hansol.isportal.transcoding.ffmpeg.GenerateWaveform;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.PostConstruct;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping({"/data"})
public class RestFulController
{
	@Autowired
	private MainService mainService;

	private Properties properties;

	protected final Logger logger;

	private RestTemplate restTemplate;

	private int totMaxRow = 50000;

	//	@PostConstruct
	//	public void initialize() {
	//		
	//	}

	public RestFulController() {
		logger = LogManager.getLogger(RestFulController.class);

		restTemplate = new RestTemplate();

		properties = new Properties();
		InputStream inputStream = getClass().getResourceAsStream("/rec.properties");
		try {
			properties.load(inputStream);
			inputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


//	http://localhost:18080/proxy/testuserid/50000/20180701000000/20180730235959/no/no
//	http://localhost:8080/isportal/data/kbcapital/testuserid/50000/20180701000000/20180730235959/no/no
	@RequestMapping(value="/proxytest/{userId}/{fetchRow}/{sdate}/{edate}/{phoneNum}/{custNo}")
	@ResponseBody
	public Map<String,Object> proxytest(
			@PathVariable String userId, @PathVariable String fetchRow, @PathVariable String sdate, @PathVariable String edate,
			@PathVariable String phoneNum, @PathVariable String custNo,
			HttpServletResponse response,
			HttpServletRequest request
			) throws Exception {

		Map<String,Object> result = new HashMap<String,Object>();

		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Methods", "GET,POST");
		response.addHeader("Access-Control-Allow-Headers", "X-Requested-With");

		boolean isLoop = false;
		int reqFetchRow = Integer.parseInt(fetchRow);

		if ( reqFetchRow > totMaxRow ) {
			response.sendError(403);
			// result.put("ResultCode", "Z1");
			return result;
		}

		Map<String, String> vars = new HashMap<String, String>();
		vars.clear();
		vars.put("userid", userId);
		vars.put("fetchrow", fetchRow);
		vars.put("sdate", sdate);
		vars.put("edate", edate);
		vars.put("phone", phoneNum);
		vars.put("custno", custNo);

		String proxyResponse = restTemplate.getForObject(
				"http://localhost:18080/proxy/{userid}/{fetchrow}/{sdate}/{edate}/{phone}/{custno}", 
				String.class, vars);

		String[] resSplit = proxyResponse.split("#");
		if ( resSplit.length > 1 ) isLoop = true;

		if (!isLoop) {

			String fileFullName = resSplit[0].split("!")[3];
			String[] tmpFileArr = fileFullName.split("/");
			String filename = tmpFileArr[tmpFileArr.length-1];

			File file = new File(fileFullName);

			response.setContentType("application/octet-stream");
			response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
			response.setHeader("Content-Type", "application/octet-stream; charset=utf-8");
			// header에 size 생략 --> chunked로???  
			//response.setHeader("Transfer-Encoding", "chunked");

			try (	InputStream in = new FileInputStream(file);
					OutputStream os = response.getOutputStream() ) {
				byte b[] = new byte[(int)file.length()];
				int leng = 0;

				while( (leng = in.read(b)) > 0 ) {
					os.write(b,0,leng);
				}
			} finally {
				if (file != null) {
					file.delete();
				}
			}

			result.put("ResultCode", "00");
			
		} else {
			int occursCnt = resSplit.length;
			List<File> files = new ArrayList<File>();

			for (int i=0; i<occursCnt; i++) {
				String fileFullName = resSplit[i].split("!")[3];
				files.add(new File(fileFullName));
			}

			String zipFileName = "testzip.zip";
			response.setContentType("text/zip");
			response.setHeader("Content-Disposition", "attachment; filename=" + zipFileName);

			int successCnt = 0;
			String failFileNames = "";
			try (	ServletOutputStream sout = response.getOutputStream();
					BufferedOutputStream bos = new BufferedOutputStream(sout);
					ZipOutputStream zos = new ZipOutputStream(bos); ) {

				for (int i=0; i<files.size(); i++) {
					File file = (File) files.get(i);
					zos.putNextEntry(new ZipEntry(file.getName()));

					try (	FileInputStream fis = new FileInputStream(file);
							BufferedInputStream fif = new BufferedInputStream(fis);) {

						int data = 0;
						while ((data = fif.read()) != -1) {
							zos.write(data);
						}

						zos.closeEntry();
						successCnt++;

					} catch (FileNotFoundException fnfe) {
						zos.write(("ERRORld not find file " + file.getName()).getBytes());
						zos.closeEntry();

						failFileNames += "," + file.getName();

						System.out.println("Couldfind file "+ file.getAbsolutePath());
						continue;
					}
				}
			} finally {
				if ( files.size() == successCnt ) result.put("ResultCode", "00");
				else result.put("ResultCode", "Z2");

				// 임시 파일 지우기 
				for (int i=0; i<files.size(); i++) {
					File file = (File) files.get(i);
					if ( !failFileNames.contains(file.getName())
							&& file.exists()) {
						file.delete();
					}
				}

				files.clear();
			}
		}

		return result;
	}


	private void fileProcess(HttpServletResponse response, String rs) throws IOException, FileNotFoundException {
		String filename = "testdd.csv";
		String fileFullName = rs.split("#")[1];
		File file = new File(fileFullName);

		response.setContentType("application/octet-stream");
		response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
		response.setHeader("Content-Type", "application/octet-stream; charset=utf-8");
		// header에 size 생략 --> chunked로???  
		//response.setHeader("Transfer-Encoding", "chunked");

		try (	InputStream in = new FileInputStream(file);
				OutputStream os = response.getOutputStream() ) {
			byte b[] = new byte[(int)file.length()];
			int leng = 0;

			while( (leng = in.read(b)) > 0 ) {
				os.write(b,0,leng);
			}
		} finally {
			if (file != null) {
				// file.delete();
			}
		}
	}

}

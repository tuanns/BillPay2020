/*
 * Copyright 2012 Viettel Telecom. All rights reserved.
 * VIETTEL PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.viettel.data.ws.utils;

import com.google.gson.Gson;
import com.viettel.common.ExchMsg;
import com.viettel.smsfw.manager.AppManager;
import com.viettel.vas.util.obj.ExchangeChannel;
import com.viettel.vas.wsfw.object.AccountInfo;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import com.viettel.vas.wsfw.object.EwalletLog;
import com.viettel.vas.wsfw.object.WalletBean;
import com.viettel.vas.wsfw.object.ResponseWallet;
import org.apache.http.params.HttpParams;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import com.viettel.vas.wsfw.object.Topup;
import org.w3c.dom.*;
import com.viettel.vas.wsfw.database.DbProcessor;
import com.viettel.vas.wsfw.object.EmolaBean;
import com.viettel.vas.wsfw.object.EpayEmolaBean;
import com.viettel.vas.wsfw.object.Offer;
import com.viettel.vas.wsfw.object.ResponseWalletView;
import com.viettel.vas.wsfw.object.RevertTransaction;
import java.io.Reader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import java.util.Calendar;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author kdvt_tungtt8
 * @version x.x
 * @since Dec 28, 2012
 */
public class Exchange {

	private Logger logger;
	private String loggerLabel = Exchange.class.getSimpleName() + ": ";
	private ExchangeChannel channel;
	public static SimpleDateFormat sdfPro = new SimpleDateFormat("yyyyMMddHHmmss");
	public static final long REQUEST_TIME_OUT = 30000;
	private org.apache.commons.httpclient.HttpClient httpTransport;
	private static MultiThreadedHttpConnectionManager conMgr;

	public Exchange(ExchangeChannel channel, Logger logger) throws IOException {
		this.logger = logger;
		this.channel = channel;
		try {
			logger.info(loggerLabel + "Connect Exchange Client-" + channel.getId());
			if (conMgr == null) {
				conMgr = new MultiThreadedHttpConnectionManager();
				conMgr.setMaxConnectionsPerHost(20000);
				conMgr.setMaxTotalConnections(20000);
			}
			if (httpTransport == null) {
				httpTransport = new org.apache.commons.httpclient.HttpClient(conMgr);
				HttpConnectionManager conMgr1 = httpTransport.getHttpConnectionManager();
				HttpConnectionManagerParams conPars = conMgr1.getParams();
				conPars.setMaxTotalConnections(2000);
				conPars.setConnectionTimeout(30000); //timeout ket noi : ms
				conPars.setSoTimeout(60000); //timeout doc ket qua : ms
			}
		} catch (Exception ex) {
			logger.error(loggerLabel + "ERROR connect Exchange Client-" + channel.getId(), ex);
		}
	}

	public void logTime(String strLog, long timeSt) {
		long timeEx = System.currentTimeMillis() - timeSt;
		StringBuffer br = new StringBuffer();
		if (timeEx >= AppManager.minTimeDb && AppManager.loggerDbMap != null) {
			br.setLength(0);
			br.append(loggerLabel).
					append(AppManager.getTimeLevelDb(timeEx)).append(": ").
					append(strLog).
					append(": ").
					append(timeEx).
					append(" ms");
			logger.warn(br);
		} else {
			br.setLength(0);
			br.append(loggerLabel).
					append(strLog).
					append(": ").
					append(timeEx).
					append(" ms");

			logger.info(br);
		}
	}

	public String callEwallet(String requestId, String mobile, String amount, String transId, String userCall, DbProcessor db) throws Exception {

		String request = "";
		String errorCode = "";
		String description = "";
		long timeSt = System.currentTimeMillis();
		EwalletLog eLog = new EwalletLog();
		try {
			logger.info("Start call Ewallet isdn " + mobile + " amount " + amount);
			String content = null;
			String url = ResourceBundle.getBundle("vas").getString("ewallet_url");
			String api = ResourceBundle.getBundle("vas").getString("api");
			String user = ResourceBundle.getBundle("vas").getString("username");
			String pass = ResourceBundle.getBundle("vas").getString("password");
			String funcName = ResourceBundle.getBundle("vas").getString("functionName");
			String transIdIsdn = transId + mobile;
			eLog.setIsdn(mobile);
			eLog.setUrl(url);
			eLog.setUserName(user);
			eLog.setFunctionName(funcName);
			eLog.setAmount(Long.valueOf(amount));
			eLog.setRequestId(requestId);
			eLog.setTransId(transIdIsdn);
			WalletBean eWallet = new WalletBean();
			eWallet.setMobile(mobile);
			eWallet.setAmount(amount);
			eWallet.setUser(userCall);
			eWallet.setTransID(transIdIsdn);
			Gson gson = new Gson();
			request = gson.toJson(eWallet, WalletBean.class);
			eLog.setRequest(request);
			// set the connection timeout value to 60 seconds (60000 milliseconds)
			final HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParams, 60000);
			HttpConnectionParams.setSoTimeout(httpParams, 60000);

			HttpClient client = new DefaultHttpClient(httpParams);
			HttpPost post = new HttpPost(url + api);
			List nameValuePairs = new ArrayList();
			TextSecurity sec = TextSecurity.getInstance();
			String str = pass + "|" + transIdIsdn;
			String passEncrypt = sec.Encrypt(str);
			nameValuePairs.add(new BasicNameValuePair("Username", user));
			nameValuePairs.add(new BasicNameValuePair("Password", passEncrypt));
			nameValuePairs.add(new BasicNameValuePair("FunctionName", funcName));
			nameValuePairs.add(new BasicNameValuePair("RequestId", transIdIsdn));
			nameValuePairs.add(new BasicNameValuePair("FunctionParams", request));
			post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			HttpResponse response = client.execute(post);
			BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			StringBuilder sb = new StringBuilder();
			String output;
			while ((output = rd.readLine()) != null) {
				sb.append(output);
			}
			content = sb.toString();
			eLog.setRespone(content);
			Gson responseGson = new Gson();
			ResponseWallet responseWallet = responseGson.fromJson(content, ResponseWallet.class);
			if (responseWallet != null && responseWallet.getResponseCode() != null) {
				errorCode = responseWallet.getResponseCode();
				description = responseWallet.getResponseMessage();
				if ("01".equals(errorCode)) {
					logger.info("Call eWallet success isdn " + mobile + " amount " + amount + " duration "
							+ (System.currentTimeMillis() - timeSt)
							+ " errorCode " + errorCode
							+ " description " + description);
				} else {
					logger.error("Call eWallet fail isdn " + mobile + " amount " + amount + " desc " + description
							+ " duration " + (System.currentTimeMillis() - timeSt)
							+ " errorCode " + errorCode
							+ " description " + description);
				}
			} else {
				logger.error("Call eWallet error isdn " + mobile + " amount " + amount + " duration "
						+ (System.currentTimeMillis() - timeSt));
			}
			eLog.setErrorCode(errorCode);
			eLog.setDescription(description);
			eLog.setDuration(System.currentTimeMillis() - timeSt);
			db.insertEwalletLog(eLog);
			return errorCode;
		} catch (Exception ex) {
			logger.error("Had exception call eWallet isdn " + mobile + " amount " + amount + " duration "
					+ (System.currentTimeMillis() - timeSt));
			try {
				eLog.setErrorCode(errorCode);
				eLog.setDescription(description);
				eLog.setDuration(System.currentTimeMillis() - timeSt);
				db.insertEwalletLog(eLog);
			} catch (Exception e) {
				logger.error("Try insert log eWallet_Log had exception isdn " + mobile);
				logger.error(AppManager.logException(timeSt, ex));
			}
			return errorCode;
		}
	}

	public ResponseWallet chargeEmola(String requestId, String mobile, String amount, String transId, String userCall, DbProcessor db,
			String custPhone) throws Exception {
		long timeSt = System.currentTimeMillis();
		EwalletLog eLog = new EwalletLog();
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
		Date now = new Date();
		ResponseWallet responseWallet = null;
		String result = "";
		Reader in = null;
		String desc = "";
		try {
			String strDate = sdfDate.format(now).replace("-", "").replace(".", "").replace(" ", "");
			String requestDate = strDate;
			int transactionType = 1;
			String request = "";
			String response = "";
			String partnerCode = ResourceBundle.getBundle("vas").getString("ewallet_partnerCode");
			String urlString = ResourceBundle.getBundle("vas").getString("ewallet_url");
			String key = ResourceBundle.getBundle("vas").getString("keyEmola");
			URL url = new URL(urlString);
			EmolaBean emola = new EmolaBean();
			emola.setRequestId(requestId);
			emola.setRequestDate(requestDate);
			emola.setPartnerCode(partnerCode);
			emola.setEnterpriseAccount(mobile);
			emola.setAmount(amount);
			emola.setContent("Deduct emola when topup value " + amount + " CustPhone " + custPhone);
			emola.setTransactionType(transactionType);
			eLog.setIsdn(mobile);
			eLog.setUrl(urlString);
			eLog.setAmount(Double.valueOf(amount));
			eLog.setRequestId(requestId);
			String tempSignature = key + requestDate + amount + mobile + partnerCode + requestId;
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(tempSignature.getBytes());
			byte[] digest = md.digest();
			StringBuilder sb = new StringBuilder();
			for (byte b : digest) {
				sb.append(String.format("%02x", b & 0xff));
			}
			logger.info(mobile + " original: " + tempSignature);
			logger.info(mobile + " digested(hex): " + sb.toString());
			emola.setSignature(sb.toString());
			Gson gson = new Gson();
			request = gson.toJson(emola, EmolaBean.class);
			eLog.setRequest(request);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Authorization", "Basic TW92aXRlbDpNb3ZpdGVsMTIzIUAj");
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setConnectTimeout(60000);
			conn.setReadTimeout(60000);
			conn.setDoOutput(true);
			conn.getOutputStream().write(request.getBytes());
			logger.info(mobile + " Response code when open Connection: "
					+ conn.getResponseCode() + "\nResponse message:" + conn.getResponseMessage());
			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			StringBuilder sbJSON = new StringBuilder();
			for (int c;
					(c = in.read()) >= 0;) {
				sbJSON.append((char) c);
			}
			response = sbJSON.toString();
			eLog.setRespone(response);
			responseWallet = gson.fromJson(response, ResponseWallet.class);
			if (responseWallet != null && responseWallet.getResponseCode() != null) {
				result = responseWallet.getResponseCode();
				desc = responseWallet.getResponseMessage();
				if ("01".equals(result)) {
					logger.info("Call eWallet success isdn " + mobile + " amount " + amount + " duration "
							+ (System.currentTimeMillis() - timeSt));
				} else {
					logger.error("Call eWallet fail isdn " + mobile + " amount " + amount
							+ " duration " + (System.currentTimeMillis() - timeSt)
							+ " errorCode " + result
							+ " description " + desc);
				}
			} else {
				logger.error("Call eWallet error responseWallet is null isdn " + mobile + " amount " + amount + " duration "
						+ (System.currentTimeMillis() - timeSt));
			}
			eLog.setErrorCode(result);
			eLog.setDescription("chargeEmola " + desc);
			eLog.setDuration(System.currentTimeMillis() - timeSt);
			db.insertEwalletLog(eLog);
		} catch (Exception e) {
			logger.error("chargeEmola had exception isdn " + mobile + " amount " + amount + " duration "
					+ (System.currentTimeMillis() - timeSt) + " detail " + e.toString());
			logger.error(AppManager.logException(timeSt, e));
			try {
				eLog.setErrorCode("99");
				eLog.setDescription("chargeEmola had exception " + e.toString());
				eLog.setDuration(System.currentTimeMillis() - timeSt);
				db.insertEwalletLog(eLog);
			} catch (Exception ex) {
				logger.error("Try insert log eWallet_Log had exception isdn " + mobile);
				logger.error(AppManager.logException(timeSt, ex));
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception ex) {
					logger.error("Failt to close inputstream " + ex.toString());
				}
			}
			return responseWallet;
		}
	}

	public String viewEmola(String mobile) throws Exception {
		String request = "";
		String errorCode = "";
		String description = "";
		String balance = "";
		long timeSt = System.currentTimeMillis();
		try {
			logger.info("Start call viewEmola isdn " + mobile);
			String content = null;
			String urlview = ResourceBundle.getBundle("vas").getString("ewallet_url_view");
			WalletBean eWallet = new WalletBean();
			URL url = new URL(urlview + "api/");
			eWallet.setMobile(mobile);
			eWallet.setUserName("SyncUser");
			eWallet.setFunctionName("CheckInfo");
			String params = "{\"Mobile\": \"XXX\"}";
			eWallet.setFunctionParams(params.replace("XXX", mobile));
			Gson gson = new Gson();
			request = gson.toJson(eWallet, WalletBean.class);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Authorization", "Basic TW92aXRlbDpNb3ZpdGVsMTIzIUAj");
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setConnectTimeout(60000);
			conn.setReadTimeout(60000);
			conn.setDoOutput(true);
			conn.getOutputStream().write(request.getBytes());
			Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			StringBuilder sbJSON = new StringBuilder();
			for (int c;
					(c = in.read()) >= 0;) {
				sbJSON.append((char) c);
			}
			content = sbJSON.toString();
//            balance = content.substring(content.indexOf("\"Balance\":") + "", content.indexOf(",\"CommisionBlance\""));
			Gson responseGson = new Gson();
			ResponseWalletView responseWallet = responseGson.fromJson(content, ResponseWalletView.class);
			if (responseWallet != null && responseWallet.getResponseCode() != null) {
				errorCode = responseWallet.getResponseCode();
				description = responseWallet.getResponseMessage();
				balance = responseWallet.getBalance();
				if ("01".equals(errorCode)) {
					logger.info("Call eWallet success isdn " + mobile + " duration "
							+ (System.currentTimeMillis() - timeSt)
							+ " errorCode " + errorCode
							+ " description " + description
							+ " balance " + balance);
				} else {
					logger.error("Call eWallet fail isdn " + mobile + " desc " + description
							+ " duration " + (System.currentTimeMillis() - timeSt)
							+ " errorCode " + errorCode
							+ " description " + description);
				}
			} else {
				logger.error("Call eWallet error isdn " + mobile + " duration "
						+ (System.currentTimeMillis() - timeSt));
			}
			return balance;
		} catch (Exception ex) {
			logger.error("Had exception call eWallet isdn " + mobile + " duration "
					+ (System.currentTimeMillis() - timeSt));
			logger.error(AppManager.logException(timeSt, ex));
			return balance;
		}
	}

	public String viewEpay(String mobile) throws Exception {
		String request = "";
		String errorCode = "";
		String description = "";
		String balance = "";
		long timeSt = System.currentTimeMillis();
		try {
			logger.info("Start call viewEpay isdn " + mobile);
			String content = null;
			String urlview = ResourceBundle.getBundle("vas").getString("ewallet_url_view");
			URL url = new URL(urlview);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Authorization", "Basic TW92aXRlbDp6b3Awc3QhQCM=");
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setConnectTimeout(60000);
			conn.setReadTimeout(60000);
			conn.setDoOutput(true);
			conn.getOutputStream().write(request.getBytes());
			Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			StringBuilder sbJSON = new StringBuilder();
			for (int c;
					(c = in.read()) >= 0;) {
				sbJSON.append((char) c);
			}
			content = sbJSON.toString();
//            balance = content.substring(content.indexOf("\"Balance\":") + "", content.indexOf(",\"CommisionBlance\""));
			Gson responseGson = new Gson();
			EpayEmolaBean responseWallet = responseGson.fromJson(content, EpayEmolaBean.class);
			if (responseWallet != null && responseWallet.getResponseCode() != null) {
				errorCode = responseWallet.getResponseCode();
				description = responseWallet.getResponseMessage();
				if ("01".equals(errorCode)) {
					balance = description.split("\\|")[1];
					logger.info("Call eWallet success isdn " + mobile + " duration "
							+ (System.currentTimeMillis() - timeSt)
							+ " errorCode " + errorCode
							+ " description " + description
							+ " balance " + balance);
				} else {
					logger.error("Call eWallet fail isdn " + mobile + " desc " + description
							+ " duration " + (System.currentTimeMillis() - timeSt)
							+ " errorCode " + errorCode
							+ " description " + description);
				}
			} else {
				logger.error("Call eWallet error isdn " + mobile + " duration "
						+ (System.currentTimeMillis() - timeSt));
			}
			return balance;
		} catch (Exception ex) {
			logger.error("Had exception call eWallet isdn " + mobile + " duration "
					+ (System.currentTimeMillis() - timeSt));
			logger.error(AppManager.logException(timeSt, ex));
			return balance;
		}
	}

	public String revertTransaction(DbProcessor db, String userName, String orgRequestId, String isdn, long money)
			throws Exception {
		logger.info("Start revertTransaction staff " + userName + " isdn " + isdn + " orgRequestId " + orgRequestId);
		if (orgRequestId == null || orgRequestId.trim().length() <= 0) {
			logger.info("End revertTransaction orgRequestId is invalid staff " + userName + " isdn " + isdn);
			return "";
		}
		long timeSt = System.currentTimeMillis();
		HttpURLConnection conn;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		String request = "";
		String response = "";
		EwalletLog eLog = new EwalletLog();
		String partnerCode = "E-MOLA";
		String strUrl = "http://10.229.16.37:9955/api/revertTransaction";
		String key = "Az1gW2WHRlzus3LqNB63kedzkWk6OjfUOrXUj7nw";
		String requestDate = sdf.format(new Date());
		String requestId = "0" + orgRequestId;
		ResponseWallet responseWallet = null;
		Reader in = null;
		String result = "";
		String desc = "";
		try {
			RevertTransaction paymentVoucher = new RevertTransaction();
			paymentVoucher.setRequestId(requestId);
			paymentVoucher.setRequestDate(requestDate);
			paymentVoucher.setPartnerCode(partnerCode);
			paymentVoucher.setOrgRequestId(orgRequestId);
			eLog.setIsdn(isdn);
			eLog.setUrl(strUrl);
			eLog.setAmount(money);
			eLog.setRequestId(requestId);
			String tempSignature = key + requestDate + partnerCode + requestId + orgRequestId;
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(tempSignature.getBytes());
			byte[] digest = md.digest();
			StringBuilder sb = new StringBuilder();
			for (byte b : digest) {
				sb.append(String.format("%02x", b & 0xff));
			}
			paymentVoucher.setSignature(sb.toString());
			Gson gson = new Gson();
			request = gson.toJson(paymentVoucher, RevertTransaction.class);
			eLog.setRequest(request);
			URL url = new URL(strUrl);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setConnectTimeout(60000);
			conn.setReadTimeout(60000);
			conn.setRequestProperty("Authorization", "Basic TW92aXRlbDpNb3ZpdGVsMTIzIUAj");
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setDoOutput(true);
			conn.getOutputStream().write(request.getBytes());
			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			StringBuilder sbJSON = new StringBuilder();
			for (int c; (c = in.read()) >= 0;) {
				sbJSON.append((char) c);
			}
			response = sbJSON.toString();
			eLog.setRespone(response);
			logger.info("End revertTransaction staff " + userName + " isdn " + isdn + " response " + response);
			responseWallet = gson.fromJson(response, ResponseWallet.class);
			if (responseWallet != null && responseWallet.getResponseCode() != null) {
				result = responseWallet.getResponseCode();
				desc = responseWallet.getResponseMessage();
			}
			eLog.setErrorCode(result);
			eLog.setDescription("revertTransaction desc: " + desc);
			eLog.setDuration(System.currentTimeMillis() - timeSt);
			db.insertEwalletLog(eLog);
		} catch (Exception e) {
			logger.error("revertTransaction had exception staff " + userName + " isdn " + isdn + " duration "
					+ (System.currentTimeMillis() - timeSt) + " detail " + e.toString());
			logger.error(AppManager.logException(timeSt, e));
			try {
				eLog.setErrorCode("99");
				eLog.setDescription("revertTransaction had exception " + e.toString());
				eLog.setDuration(System.currentTimeMillis() - timeSt);
				db.insertEwalletLog(eLog);
			} catch (Exception ex) {
				logger.error("Try insert log eWallet_Log had exception staff " + userName + " isdn " + isdn);
				logger.error(AppManager.logException(timeSt, ex));
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception ex) {
					logger.error("Failt to close inputstream " + ex.toString());
				}
			}
			return result;
		}
	}

	public String addSmsDataVoice(String isdn, String value, String balanceId, String expireTime) {
		long timeSt = System.currentTimeMillis();
		ExchMsg request = new ExchMsg();
		ExchMsg response = null;
		String err = "";
		try {
			logger.info("Start addSmsDataVoice for sub " + isdn + " value " + value + " accountid " + balanceId);
			request.setCommand("OCSHW_ADJUST_SMS_DATA");
			request.set("ISDN", "258" + isdn);
			request.set("ACCOUNT_TYPE", balanceId);
			request.set("VALIDITY_INCREMENT", "0");
			request.set("AMOUNT", value);
			if (expireTime != null && !"".endsWith(expireTime)) {
				request.set("EXPIRE_DATE", expireTime);
			}
			response = (ExchMsg) channel.sendAll(request, REQUEST_TIME_OUT, true);
			err = response.getError();
			logger.info("End addSmsDataVoice isdn " + isdn + " amount " + value + " balanceid " + balanceId + " result " + err);
			return err;
		} catch (Exception ex) {
			logger.error("Had exception addSmsDataVoice isdn " + isdn + " amount " + value);
			logger.error(AppManager.logException(timeSt, ex));
			return err;
		}
	}

	public String adjustMoney(String isdn, String money, String balanceId) {
		long timeSt = System.currentTimeMillis();
		ExchMsg request = new ExchMsg();
		ExchMsg response = null;
		String err = "";
		try {
			logger.info("Start adjustMoney for sub " + isdn + " value " + money + " accountid " + balanceId);
			request.setCommand("OCSHW_ADJUSTACCOUNT");
			if (!isdn.startsWith("258")) {
				request.set("ISDN", "258" + isdn.trim());
			} else {
				request.set("ISDN", isdn.trim());
			}
			request.set("ACCOUNT_TYPE", balanceId);
			request.set("AMOUNT", money);
			request.set("VALIDITY_INCREMENT", "0");
			response = (ExchMsg) channel.sendAll(request, REQUEST_TIME_OUT, true);
			err = response.getError();
			logger.info("End adjustMoney isdn " + isdn + " amount " + money + " balanceid " + balanceId + " result " + err);
			return err;
		} catch (Exception ex) {
			logger.error("Had exception adjustMoney isdn " + isdn + " amount " + money + " accountid " + balanceId);
			logger.error(AppManager.logException(timeSt, ex));
			return err;
		}
	}

	public String modifyMoney(String isdn, String money, String balanceId, String expireTime) {
		long timeSt = System.currentTimeMillis();
		ExchMsg request = new ExchMsg();
		ExchMsg response = null;
		String err = "";
		try {
			String moneyReal = String.valueOf(Integer.parseInt(money) * 100);
			logger.info("Start addMoney for sub " + isdn + " value " + money + " accountid " + balanceId);
			request.setCommand("OCSHW_ADJUST_SMS_DATA");
			request.set("ISDN", "258" + isdn);
			request.set("ACCOUNT_TYPE", balanceId);
			request.set("AMOUNT", moneyReal);
			request.set("VALIDITY_INCREMENT", "0");
			if (expireTime != null && !"".endsWith(expireTime)) {
				request.set("EXPIRE_DATE", expireTime);
			}
			response = (ExchMsg) channel.sendAll(request, REQUEST_TIME_OUT, true);
			err = response.getError();
			logger.info("End addMoney isdn " + isdn + " amount " + money + " balanceid " + balanceId + " result " + err);
			return err;
		} catch (Exception ex) {
			logger.error("Had exception addMoney isdn " + isdn + " amount " + money);
			logger.error(AppManager.logException(timeSt, ex));
			return err;
		}
	}

	public AccountInfo viewAccInfo(String msisdn) {
		long timeSt = System.currentTimeMillis();
		AccountInfo accounts = null;
		ExchMsg response = null;
		try {
			logger.info("start to viewAccInfo for sub " + msisdn);
			ExchMsg request = new ExchMsg();
			request.setCommand("OCSHW_INTEGRATIONENQUIRY");
			request.set("MSISDN", msisdn);
//            logger.info("Before send " + request.toString());
			response = (ExchMsg) channel.sendAll(request, REQUEST_TIME_OUT, true);
//            logger.info("After send " + response.toString());
			accounts = new AccountInfo();
			accounts.setErr("ERROR");
			if (response == null || response.getError() == null) {
				logger.error("ERROR getAccountInfo, response is null, sub " + msisdn);
				accounts.setErr("Response getAccount is null");
				return accounts;
			}
			if (response.getError().equals("0")) {
				accounts.setErr(response.getError());
				String accTypeStr = (String) response.get("ACCOUNT_TYPE_LIST");
				String balanceStr = (String) response.get("BALANCE_LIST");
				String expireStr = (String) response.get("EXPIRE_TIME_LIST");
				logger.info("list acc_id of sub " + msisdn + ": " + accTypeStr);
				logger.info("list balance of sub " + msisdn + ": " + balanceStr);
				if (accTypeStr != null && accTypeStr.trim().length() > 0) {
					String[] accTypeInfo = accTypeStr.split("&");
					String[] balanceInfo = balanceStr.split("&");
					String[] expireInfo = expireStr.split("&");
					for (int idx = 0; idx < accTypeInfo.length; idx++) {
						String balance = balanceInfo[idx];
						String bal = accounts.getAccount(accTypeInfo[idx]);
						Date expire = sdfPro.parse(expireInfo[idx]);
						if (expire.getTime() > System.currentTimeMillis()) {
							if (bal != null) {
								accounts.putAccount(accTypeInfo[idx], bal);
							} else {
								accounts.putAccount(accTypeInfo[idx], balance);
								accounts.putAccountExpire(accTypeInfo[idx], expireInfo[idx]);
							}
						}
					}
				} else {
					logger.error("ERROR getAccountInfo, account type null, sub " + msisdn);
				}
			} else {
				logger.error("ERROR getAccountInfo, sub " + msisdn + " detail response: " + response);
				accounts.setErr(response.getError());
				accounts.setDesc(response.getDescription());
			}
		} catch (Exception ex) {
			StringBuilder br = new StringBuilder();
			br.setLength(0);
			br.append("ERROR viewAccInfo, msisdn ");
			br.append(msisdn);
			if (response != null) {
				br.append(" RESPONSE:\n").append(response);
			} else {
				br.append(" Response is null");
			}
			logger.error(br.toString(), ex);
			logger.error(AppManager.logException(timeSt, ex));
		}
		return accounts;
	}

	public Topup topupPrePaid(String msisdn, String money, String command) {
		long timeSt = System.currentTimeMillis();
		Topup accounts = null;
		ExchMsg response = null;
		try {
			logger.info("start topupPrePaid for sub " + msisdn + " money " + money);
			ExchMsg request = new ExchMsg();
			request.setSynchronous(true);
			Double amount = 0d;
			amount = Double.parseDouble(money);
			// Chuyen price ve so duong neu la so am.
			amount = Math.abs(amount);
			if (command != null && command.trim().length() > 0) {
				request.setCommand(command);
			} else {
				request.setCommand("OCSHW_PAYMENT");
			}
			request.set("ISDN", msisdn);
			request.set("AMOUNT", formatAmount(String.valueOf(amount)));
			response = (ExchMsg) channel.sendAll(request, REQUEST_TIME_OUT, true);
			accounts = new Topup();
			accounts.setErr("ERROR");
			if (response == null) {
				logger.error("ERROR topupPrePaid, response is null sub " + msisdn + " money " + money);
				accounts.setErr("Response topupPrePaid is null");
				return accounts;
			}
			if (response.getError().equals("0")) {
				logger.info("topupPrePaid success for sub " + msisdn + " money " + money);
				String balance = money;
				accounts.setBalance(balance);
				accounts.setErr(response.getError());
			} else {
				logger.error("ERROR topupPrePaid msisdn " + msisdn + " money " + money + " detail respone:\n" + response);
				accounts.setErr(response.getError());
				accounts.setDesc(response.getDescription());
			}
		} catch (Exception ex) {
			StringBuilder br = new StringBuilder();
			br.setLength(0);
			br.append("ERROR TopupMoney msisdn ");
			br.append(msisdn);
			br.append(" money ");
			br.append(money);
			if (response != null) {
				br.append(" RESPONSE:\n").append(response);
			} else {
				br.append(" Response is null");
			}
			logger.error(br, ex);
			logger.error(AppManager.logException(timeSt, ex));
		}
		return accounts;
	}

    /**
     * 20191016-Bacnx Topup prepaid by party code
     *
     * @param msisdn
     * @param money
     * @param command
     * @param partyCode
     * @return
     */
    public Topup topupPrePaidByPartyCode(String msisdn, String money, String command, String partyCode) {
        long timeSt = System.currentTimeMillis();
        Topup accounts = null;
        ExchMsg response = null;
        try {
            logger.info("start topupPrePaid for sub " + msisdn + " money " + money);
            ExchMsg request = new ExchMsg();
            request.setSynchronous(true);
            Double amount = 0d;
            amount = Double.parseDouble(money);
            // Chuyen price ve so duong neu la so am.
            amount = Math.abs(amount);
            if (command != null && command.trim().length() > 0) {
                request.setCommand(command);
            } else {
                request.setCommand("OCSHW_PAYMENT");
            }
            if (partyCode != null && partyCode.trim().length() > 0) {
                request.set("PARTYCODE", partyCode);
            }
            request.set("ISDN", msisdn);
            request.set("AMOUNT", formatAmount(String.valueOf(amount)));
            response = (ExchMsg) channel.sendAll(request, REQUEST_TIME_OUT, true);
            accounts = new Topup();
            accounts.setErr("ERROR");
            if (response == null) {
                logger.error("ERROR topupPrePaid, response is null sub " + msisdn + " money " + money);
                accounts.setErr("Response topupPrePaid is null");
                return accounts;
            }
            if (response.getError().equals("0")) {
                logger.info("topupPrePaid success for sub " + msisdn + " money " + money);
                String balance = money;
                accounts.setBalance(balance);
                accounts.setErr(response.getError());
            } else {
                logger.error("ERROR topupPrePaid msisdn " + msisdn + " money " + money + " detail respone:\n" + response);
                accounts.setErr(response.getError());
                accounts.setDesc(response.getDescription());
            }
        } catch (Exception ex) {
            StringBuilder br = new StringBuilder();
            br.setLength(0);
            br.append("ERROR TopupMoney msisdn ");
            br.append(msisdn);
            br.append(" money ");
            br.append(money);
            if (response != null) {
                br.append(" RESPONSE:\n").append(response);
            } else {
                br.append(" Response is null");
            }
            logger.error(br, ex);
            logger.error(AppManager.logException(timeSt, ex));
        }
        return accounts;
    }

    public static String getCharacterDataFromElement(Element e) {
        Node child = e.getFirstChild();
        if (child instanceof CharacterData) {
            CharacterData cd = (CharacterData) child;
            return cd.getData();
        }
        return "?";
    }

	public String formatAmount(String amount) {
		String[] info = amount.split("\\.");
		if (info.length < 2) {
			System.out.println("Length: " + info.length);
			return amount;
		}
		String mod = info[1];
		mod = ff(mod);
		return info[0] + (mod.length() > 0 ? "." + mod : "");
	}

	private String ff(String s) {
		if (s.equals("")) {
			return s;
		}
		System.out.println("Char" + s.charAt(s.length() - 1));

		if (s.charAt(s.length() - 1) == '0') {
			return ff(s.substring(0, s.length() - 1));
		}

		return s;
	}

	public String registerMI(String msisdn, String packageName) {
		String wsdlMI = ResourceBundle.getBundle("vas").getString("wsdl_MI");
		PostMethod post = new PostMethod(wsdlMI);
		String soapResponse = "";
		String result = "";
		String errCode = "";
		String description = "";
		long start = System.currentTimeMillis();
		try {
			String request = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:impl=\"http://impl.mi.ws.viettel.com/\"> "
					+ "   <soapenv:Header/> "
					+ "   <soapenv:Body> "
					+ "      <impl:Register> "
					+ "         <!--Optional:--> "
					+ "         <msisdn>" + msisdn + "</msisdn> "
					+ "         <!--Optional:--> "
					+ "         <userName>bccs_mi</userName> "
					+ "         <!--Optional:--> "
					+ "         <passWord>bccsmi321</passWord> "
					+ "         <!--Optional:--> "
					+ "         <package>" + packageName + "</package> "
					+ "         <!--Optional:--> "
					+ "         <sendSMS>?</sendSMS> "
					+ "      </impl:Register> "
					+ "   </soapenv:Body> "
					+ "</soapenv:Envelope>";

			RequestEntity entity = new StringRequestEntity(request, "text/xml", "UTF-8");
			post.setRequestHeader("SOAPAction", "false");
			post.setRequestEntity(entity);
			httpTransport.executeMethod(post);
			soapResponse = post.getResponseBodyAsString(609600000);
			//            Parse reponse
			XmlConfig soapMsg = new XmlConfig();
			soapMsg.load(soapResponse, logger);
			org.jdom.Element root = soapMsg.getDocument().getRootElement();
			org.jdom.Element ele = XmlUtil.findElement(root, "errorCode");
			errCode = ele.getText();
			org.jdom.Element ele2 = XmlUtil.findElement(root, "description");
			if (ele2 != null) {
				description = ele2.getText();
			} else {
				description = "can not get description";
			}
			result = errCode + ";" + description;
			logTime("Time to registerMI msisdn : " + msisdn + " result: " + result, start);
			return result;
		} catch (Exception ex) {
			logTime("Exception to registerMI msisdn:  " + msisdn + " errCode " + errCode + " description " + description, start);
			logger.error(AppManager.logException(start, ex));
			return result;
		} finally {
			post.releaseConnection();
		}
	}

    public String topupPostPaid(String isdn, String money) {
        String soapResponse = "30";
        String wsdl = ResourceBundle.getBundle("vas").getString("wsdl_charging");
        String soapAction = ResourceBundle.getBundle("vas").getString("soapAction");
        String returnTag = ResourceBundle.getBundle("vas").getString("returnTag");
        PostMethod post = new PostMethod(wsdl);
        long start = System.currentTimeMillis();
        try {
            if (money.contains(".")) {
                money = money.substring(0, money.indexOf("."));
                            }
            if (isdn.startsWith("258")) {
                isdn = isdn.substring(3);
                        }
            String soapRequest = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ser=\"http://services.payment.viettel.com/\"> "
                    + "   <soapenv:Header/> "
                    + "   <soapenv:Body> "
                    + "      <ser:insertTransaction> "
                    + "        <isdn>" + isdn + "</isdn> "
                    + "          <isdnCharge>" + isdn + "</isdnCharge> "
                    + "        <amount>" + money + "</amount> "
                    + "        <pin>1</pin> "
                    + "        <domainCode>ewallet</domainCode> "
                    + "      </ser:insertTransaction> "
                    + "   </soapenv:Body> "
                    + "</soapenv:Envelope>";
            RequestEntity entity = new StringRequestEntity(soapRequest, "text/xml", "UTF-8");
            post.setRequestEntity(entity);
            post.setRequestHeader("SOAPAction", soapAction);
            httpTransport.executeMethod(post);
            soapResponse = post.getResponseBodyAsString();
            if (soapResponse.contains(returnTag)) {
                logger.info("Result soap: " + soapResponse);
                int startReturn = soapResponse.indexOf("<" + returnTag + ">") + returnTag.length() + 2;
                int endReturn = soapResponse.indexOf("</" + returnTag + ">");
                int startReturnDesc = soapResponse.indexOf("<" + "description" + ">") + "description".length() + 2;
                int endReturnDesc = soapResponse.indexOf("</" + "description" + ">");
                String desc = soapResponse.substring(startReturnDesc, endReturnDesc).trim();
                if (desc.equals("Success")) {
                    soapResponse = soapResponse.substring(startReturn, endReturn).trim();
                } else {
                    soapResponse = "111";
                    }
                    } else {
                soapResponse = "30";
                    }
            logTime("Time to callCharging msisdn : " + isdn + " result: " + soapResponse, start);
            return soapResponse;
        } catch (Exception ex) {
            logTime("Exception to callCharging msisdn:  " + isdn + " errCode " + soapResponse, start);
            logger.error(AppManager.logException(start, ex));
            return soapResponse;
        } finally {
            post.releaseConnection();
        }
    }

	public String lockCard(String msisdn, String pin) {
		ExchMsg request = new ExchMsg();
		long timeSt = System.currentTimeMillis();
		ExchMsg response = null;
		String err = "";
		String balance = "";
		String[] typeIds;
		String[] balances;
		String[] balances2;
		String balanceIdKey = "PARAMNAME";
		String balanceValueKey = "PARAMVALUE";
		try {
			request.setCommand("VCHW_SETUVCCARD");
			request.set("CARDPIN", pin);
			response = (ExchMsg) channel.sendAll(request, REQUEST_TIME_OUT, true);
			err = response.getError();
			if ("0".equals(err)) {
                String ori = response.getOriginal();
                if (!ori.contains("FACEVALUE=")) { //20190920 Huynq add to check HW VC
				typeIds = ((String) response.get(balanceIdKey)).split("&");
				balances = ((String) response.get(balanceValueKey)).split("\\|");
				balances2 = ((String) response.get(balanceValueKey)).split("&");
				for (int i = 0; i < typeIds.length; i++) {
					if (typeIds[i].equals("RESLEFT1")) {
						if (balances.length >= i) {
							balance = balances[i];
						} else if (balances2.length >= i) {
							balance = balances2[i];
						}
						break;
					}
				}
				if (!"".equals(balance)) {
					int iBasicBalance = Integer.valueOf(balance);
					balance = "" + (iBasicBalance / 100);
				} else {
					logger.error("Failt to get value of scraft card when already locked card sub " + msisdn + " pin " + pin);
				}
                } else { //vVC
                    balance = ori.substring(ori.indexOf("FACEVALUE=") + 10, ori.indexOf("|"));
			}
            }
			logger.info("End lockCard isdn " + msisdn + " pin " + pin + " errorCode " + err + " value " + balance);
			return balance;
		} catch (Exception ex) {
			logger.error("Had exception lockCard isdn " + msisdn + " pin " + pin + " " + ex.toString() + " respone: \n" + response);
			logger.error(AppManager.logException(timeSt, ex));
			return balance;
		}
	}

	public Topup topupPrePaidPartner(String msisdn, String money) {
		long timeSt = System.currentTimeMillis();
		Topup accounts = null;
		ExchMsg response = null;
		try {
			logger.info("start topupPrePaid for sub " + msisdn + " money " + money);
			ExchMsg request = new ExchMsg();
			request.setSynchronous(true);
			Double amount = 0d;
			amount = Double.parseDouble(money);
			// Chuyen price ve so duong neu la so am.
			amount = Math.abs(amount);
			request.setCommand("OCSHW_PAYMENT_PARTNER");
			request.set("ISDN", msisdn);
			request.set("AMOUNT", formatAmount(String.valueOf(amount)));
			response = (ExchMsg) channel.sendAll(request, REQUEST_TIME_OUT, true);
			accounts = new Topup();
			accounts.setErr("ERROR");
			if (response == null) {
				logger.error("ERROR topupPrePaidPartner, response is null sub " + msisdn + " money " + money);
				accounts.setErr("Response topupPrePaidPartner is null");
				return accounts;
			}
			if (response.getError().equals("0")) {
				logger.info("topupPrePaidPartner success for sub " + msisdn + " money " + money);
				String balance = money;
				accounts.setBalance(balance);
				accounts.setErr(response.getError());
			} else {
				logger.error("ERROR topupPrePaidPartner msisdn " + msisdn + " money " + money + " detail respone:\n" + response);
				accounts.setErr(response.getError());
				accounts.setDesc(response.getDescription());
			}
		} catch (Exception ex) {
			StringBuilder br = new StringBuilder();
			br.setLength(0);
			br.append("ERROR topupPrePaidPartner msisdn ");
			br.append(msisdn);
			br.append(" money ");
			br.append(money);
			if (response != null) {
				br.append(" RESPONSE:\n").append(response);
			} else {
				br.append(" Response is null");
			}
			logger.error(br, ex);
			logger.error(AppManager.logException(timeSt, ex));
		}
		return accounts;
	}

	public Topup topupPosPaidPartner(String msisdn, String money) {
		long timeSt = System.currentTimeMillis();
		Topup accounts = null;
		ExchMsg response = null;
		try {
			logger.info("start topupPrePaid for sub " + msisdn + " money " + money);
			ExchMsg request = new ExchMsg();
			request.setSynchronous(true);
			Double amount = 0d;
			amount = Double.parseDouble(money);
			// Chuyen price ve so duong neu la so am.
			amount = Math.abs(amount);
			request.setCommand("OCSHW_PAYMENT_GW");
			request.set("ISDN", msisdn);
			request.set("AMOUNT", formatAmount(String.valueOf(amount)));
			response = (ExchMsg) channel.sendAll(request, REQUEST_TIME_OUT, true);
			accounts = new Topup();
			accounts.setErr("ERROR");
			if (response == null) {
				logger.error("ERROR topupPrePaidPartner, response is null sub " + msisdn + " money " + money);
				accounts.setErr("Response topupPrePaidPartner is null");
				return accounts;
			}
			if (response.getError().equals("0")) {
				logger.info("topupPrePaidPartner success for sub " + msisdn + " money " + money);
				String balance = money;
				accounts.setBalance(balance);
				accounts.setErr(response.getError());
			} else {
				logger.error("ERROR topupPrePaidPartner msisdn " + msisdn + " money " + money + " detail respone:\n" + response);
				accounts.setErr(response.getError());
				accounts.setDesc(response.getDescription());
			}
		} catch (Exception ex) {
			StringBuilder br = new StringBuilder();
			br.setLength(0);
			br.append("ERROR topupPrePaidPartner msisdn ");
			br.append(msisdn);
			br.append(" money ");
			br.append(money);
			if (response != null) {
				br.append(" RESPONSE:\n").append(response);
			} else {
				br.append(" Response is null");
			}
			logger.error(br, ex);
			logger.error(AppManager.logException(timeSt, ex));
		}
		return accounts;
	}

	public String addPrice(String msisdn, String planId, String effectDate, String addDay) {
		ExchMsg request = new ExchMsg();
		long timeSt = System.currentTimeMillis();
		ExchMsg response = null;
		String err = "";
		try {
			request.setCommand("OCSHW_SUBSCRIBEPRODUCT");
			if (!msisdn.startsWith("258")) {
				msisdn = "258" + msisdn;
			}
			request.set("MSISDN", msisdn);
			request.set("PRODUCTID", planId);
			if (!"".equals(effectDate)) {
				request.set("EFFECTIVE_DATE", effectDate);
			}
			if ("-1".equals(addDay)) {
//                note set negative value
			} else if (!"".equals(addDay)) {
				Calendar cal = Calendar.getInstance();
				cal.setTime(new Date());
				cal.add(Calendar.DATE, Integer.valueOf(addDay));
				request.set("EXPIRE_DATE", sdfPro.format(cal.getTime()));
			}
			response = (ExchMsg) channel.sendAll(request, REQUEST_TIME_OUT, true);
			err = response.getError();
			logger.info("End addPrice isdn " + msisdn + " planId " + planId + " result " + err);
			return err;
		} catch (Throwable ex) {
			logger.error("Had exception addPrice isdn " + msisdn + " planId " + planId);
			logger.error(AppManager.logException(timeSt, ex));
			return err;
		}
	}

	public String checkActiveStatusOnOCS(String msisdn) {
		long timeSt = System.currentTimeMillis();
		String result = "-1";
		ExchMsg response = null;
		try {
			logger.info("start to checkActiveStatusOnOCS for sub " + msisdn);
			ExchMsg request = new ExchMsg();
			request.setCommand("OCSHW_INTEGRATIONENQUIRY");
			if (!msisdn.startsWith("258")) {
				msisdn = "258" + msisdn;
			}
			request.set("MSISDN", msisdn);
//            logger.info("Before send " + request.toString());
			response = (ExchMsg) channel.sendAll(request, REQUEST_TIME_OUT, true);
//            logger.info("After send " + response.toString());
			if (response == null || response.getError() == null) {
				logger.error("ERROR checkActiveStatusOnOCS, response is null, sub " + msisdn);
				return result;
			}
			if (response.getError().equals("0")) {
				result = (String) response.get("LIFE_CYCLE_STATE");
			} else {
				logger.error("ERROR checkActiveStatusOnOCS, sub " + msisdn + " detail response: " + response);
			}
		} catch (Exception ex) {
			StringBuilder br = new StringBuilder();
			br.setLength(0);
			br.append("ERROR checkActiveStatusOnOCS, msisdn ");
			br.append(msisdn);
			if (response != null) {
				br.append(" RESPONSE:\n").append(response);
			} else {
				br.append(" Response is null");
			}
			logger.error(br.toString(), ex);
			logger.error(AppManager.logException(timeSt, ex));
		}
		return result;
	}

    public List<Offer> parseListOffer(String originalXML) throws ParserConfigurationException, SAXException, IOException {
        List<Offer> listOffer = new ArrayList<Offer>();
        Offer offer = null;

        String xmlRecords = originalXML;

        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(xmlRecords));
        Document doc = db.parse(is);
        NodeList nList = doc.getElementsByTagName("Offer");
        System.out.println(nList.getLength());
        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node node = nList.item(temp);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                org.w3c.dom.Element eElement = (org.w3c.dom.Element) node;
                offer = new Offer();
                offer.setName(eElement.getElementsByTagName("Name").item(0).getTextContent());
                offer.setId(eElement.getElementsByTagName("Id").item(0).getTextContent());
                offer.setVersion(eElement.getElementsByTagName("Version").item(0).getTextContent());
                offer.setExpDate(eElement.getElementsByTagName("ExpDate").item(0).getTextContent());
                offer.setEffDate(eElement.getElementsByTagName("EffDate").item(0).getTextContent());
                offer.setIdMember(eElement.getElementsByTagName("IdMember").item(0).getTextContent());
                offer.setListMember(eElement.getElementsByTagName("ListMember").item(0).getTextContent());
                offer.setState(eElement.getElementsByTagName("State").item(0).getTextContent());
                offer.setRecurringDate(eElement.getElementsByTagName("RecurringDate").item(0).getTextContent());
                offer.setUpgradeOrNot(eElement.getElementsByTagName("UpgradeOrNot").item(0).getTextContent());
                offer.setUpgradeTime(eElement.getElementsByTagName("UpgradeTime").item(0).getTextContent());
                offer.setDowngradeTime(eElement.getElementsByTagName("DowngradeTime").item(0).getTextContent());
                listOffer.add(offer);
            }
        }
        return listOffer;
    }

    public String getOriginalOfCommand(String msisdn, String command, HashMap<String, String> lstParams) {
        ExchMsg request = new ExchMsg();
        long timeSt = System.currentTimeMillis();
        ExchMsg response = null;
        String original = "";
        if (!msisdn.startsWith("258")) {
            msisdn = "258" + msisdn;
        }
        try {
            request.setCommand(command);
            for (String tmpKey : lstParams.keySet()) {
                String key = tmpKey;
                String value = lstParams.get(key);
                request.set(key, value);
            }
            response = (ExchMsg) channel.sendAll(request, REQUEST_TIME_OUT, true);
            original = response.getOriginal();
            logger.info("End getOriginalOfCommand isdn " + msisdn + ", original " + original);
            return original;
        } catch (Exception ex) {
            logger.error("Had exception getOriginalOfCommand isdn " + msisdn);
            logger.error(AppManager.logException(timeSt, ex));
        } finally {
            return original;
        }
    }

    public String modiSubProduct(String msisdn, String productId, String effDate, String effectType, String expireDate) {
        ExchMsg request = new ExchMsg();
        long timeSt = System.currentTimeMillis();
        ExchMsg response = null;
        String err = "";
        try {
            if (!msisdn.startsWith("258")) {
                msisdn = "258" + msisdn;
            }
            request.setCommand("OCSHW_MODISUBPRODUCT");
            request.set("MSISDN", msisdn);
            request.set("PRODUCTID", productId);
            if (effDate != null && effDate.length() > 0) {
                request.set("EFF_DATE", effDate);
            }
            if (effectType != null && effectType.length() > 0) {
                request.set("EFFECT_TYPE", effectType);
            }
            if (expireDate != null && expireDate.length() > 0) {
                request.set("EXPIRE_DATE", expireDate);
            }
            response = (ExchMsg) channel.sendAll(request, REQUEST_TIME_OUT, true);
            err = response.getError();
            logger.info("End modiSubProduct isdn " + msisdn + " productId " + productId + " result " + err + ", desc: " + response.getDescription());
            return err;
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("Had exception modiSubProduct isdn " + msisdn + " productId " + productId);
            logger.error(AppManager.logException(timeSt, ex));
            return err;
        }
    }

    public boolean checkValidProductID(String msisdn, String productId) {
        SimpleDateFormat parseDateVocs = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a");
        long timeSt = System.currentTimeMillis();
        boolean result = false;
        ExchMsg response = null;
        try {
            if (!msisdn.startsWith("258")) {
                msisdn = "258" + msisdn;
            }
            logger.info("start to checkValidProductID for sub " + msisdn + " productId " + productId);
            ExchMsg request = new ExchMsg();
            request.setCommand("OCSHW_INTEGRATIONENQUIRY");
            request.set("MSISDN", msisdn);
            response = (ExchMsg) channel.sendAll(request, REQUEST_TIME_OUT, true);
            if (response == null || response.getError() == null) {
                logger.error("ERROR checkValidProductID, response is null, sub "
                        + msisdn + " productId " + productId + " default return false");
                return result;
            }
            if (response.getError().equals("0")) {
                String product = (String) response.get("PRODUCT_LIST");
                String expireStr = (String) response.get("EXPIRED_DATE_LIST");
                logger.info("list product of sub " + msisdn + ": " + product);
                logger.info("list expire of product " + msisdn + ": " + expireStr);
                if (product != null && product.trim().length() > 0) {
                    String[] listProduct = product.split("&");
                    String[] expireInfo = expireStr.split("&");
                    for (int idx = 0; idx < listProduct.length; idx++) {
                        String key = listProduct[idx];
                        Date expire = parseDateVocs.parse(expireInfo[idx]);
                        if (expire.getTime() > System.currentTimeMillis()) {
                            if (key != null && key.trim().equals(productId)) {
                                result = true;
                                break;
                            }
                        }
                    }
                } else {
                    logger.error("ProductList is null so return false, sub " + msisdn);
                }
            } else {
                logger.error("ERROR checkValidProductID, sub " + msisdn + " default return false, detail response: " + response);
            }
        } catch (Exception ex) {
            StringBuilder br = new StringBuilder();
            br.setLength(0);
            br.append("ERROR checkValidProductID, msisdn ");
            br.append(msisdn);
            if (response != null) {
                br.append(" RESPONSE:\n").append(response);
            } else {
                br.append(" Response is null");
            }
            logger.error(br.toString(), ex);
            logger.error(AppManager.logException(timeSt, ex));
        }
        return result;
    }

    public String[] removeMemberGroupCUG(String groupId, String isdn) {
        ExchMsg request = new ExchMsg();
        ExchMsg response = null;
        long start = System.currentTimeMillis();
        try {
            request.setCommand("OCS_RM_VPN_MEMBER");
            if (!isdn.startsWith("258")) {
                isdn = "258" + isdn;
            }
            request.set("MSISDN", isdn);
            request.set("USER_GROUP_ID", groupId);
            response = (ExchMsg) channel.sendAll(request, REQUEST_TIME_OUT, true);
            logTime("Time to removeMemberGroupCUG groupId " + groupId + ", errorcode " + response.getError(), start);
            String res = response.getError() + "|" + response.getDescription();
            return res.split("\\|");
        } catch (Exception e) {
            logTime("Exception to removeMemberGroupCUG groupId " + groupId, start);
            logger.error(AppManager.logException(start, e));
            return null;
        }
    }
}

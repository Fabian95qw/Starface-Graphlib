package nucom.module.msgraphs.token;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import nucom.module.msgraphs.utility.LogHelper;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import de.vertico.starface.module.core.runtime.IRuntimeEnvironment;

public class TokenCache implements Runnable 
{	
	private TokenSerial TS = null;
	private ScheduledExecutorService SES;
	
	private TokenCacheManager TCM = null;
	private Log log = null;	
	
	public TokenCache(String ClientID, String Token, String RefreshToken, Integer Expires, IRuntimeEnvironment context)
	{
		this.log=context.getLog();
		TCM =(TokenCacheManager)context.provider().fetch(TokenCacheManager.class);
		
		TS = new TokenSerial(ClientID, Token, RefreshToken, DateUtils.addSeconds(new Date(), Expires-60).getTime());
		
		log.debug("[TCP] Expires: " + TS.getExpiresDateFormatted());
		log.debug("[TCP] Schedule in: " + TS.ExpiresinSeconds());
		
		SES = Executors.newScheduledThreadPool(1);
		SES.schedule(this, Expires-60, TimeUnit.SECONDS);
	}

	public TokenCache(TokenSerial TS, IRuntimeEnvironment context)
	{
		this.log=context.getLog();
		TCM =(TokenCacheManager)context.provider().fetch(TokenCacheManager.class);
		this.TS=TS;				
		
		log.debug("[TCP] Expires: "+ TS.getExpiresDateFormatted());
		log.debug("[TCP] Schedule in: " + TS.ExpiresinSeconds());
		
		SES = Executors.newScheduledThreadPool(1);
		SES.schedule(this, TS.ExpiresinSeconds(), TimeUnit.SECONDS);	
	}
	
	private String encode(String S)
	{
		try 
		{
			return URLEncoder.encode(S, "UTF-8");
		} 
		catch (UnsupportedEncodingException e)
		{
			LogHelper.EtoStringLog(log, e);
		}
		return null;
	}
	
	@Override
	public void run() 
	{
		log.debug("[TCP] Refreshing Token...");		
		
		 HttpsURLConnection HTTPC=null;

		 try
		 {
		
			 String BaseURL ="https://login.microsoftonline.com/common/oauth2/v2.0/token";
			 URL U = new URL (BaseURL);
			 HTTPC = (HttpsURLConnection)U.openConnection();
			 HTTPC.setRequestMethod("POST");
			 HTTPC.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			
			 String ID="client_id="+TS.getClientID();
			 String GrantType="grant_type="+"urn:ietf:params:oauth:grant-type:refresh_token";
			 String Scope="scope="+encode("offline_access .default");
			 String ParamToken="refresh_token="+TS.getRefreshToken();
			 String Param = GrantType+"&"+ID+"&"+Scope+"&"+ParamToken;

			 byte[] Data = Param.getBytes();
			 		 
			 HTTPC.setRequestProperty("charset", "utf-8");
			 HTTPC.setRequestProperty("Content-Length", Integer.toString(Data.length));
			 HTTPC.setUseCaches(false);
			 HTTPC.setDoOutput(true);
			 
			 DataOutputStream DOS =null;
			 try
			 {
				DOS = new DataOutputStream(HTTPC.getOutputStream());
			    DOS.write(Data);
			 }
			 catch(Exception e)
			 {
				 LogHelper.EtoStringLog(log, e);
				 DOS.close();
			 }
			 DOS.close();
			 
			 BufferedReader BR = new BufferedReader(new InputStreamReader(HTTPC.getInputStream()));
			 StringBuilder SB = new StringBuilder();
			 
			 String Line;
			 while((Line = BR.readLine()) != null)
			 {
				 SB.append(Line);
			 }
			 BR.close();
			 HTTPC.disconnect();
			 
			 log.debug(SB.toString());
			 JSONParser JP = new JSONParser();
			 JSONObject JSO = (JSONObject)JP.parse(SB.toString());
			 String Token = (String) JSO.get("access_token");
			 String SExpires = (String) JSO.get("expires_in");
			 Integer Expires = Integer.parseInt(SExpires);
			 
			 log.debug("New Token: " +Token);
			 log.debug("Expires: " + Expires);
			 
			 SES.schedule(this, Expires-60, TimeUnit.SECONDS);
			 String RefreshToken ="";
			 try
			 {
				 RefreshToken = (String) JSO.get("refresh_token");
			 }
			 catch(Exception e)
			 {
				 log.debug("No Refresh Token provided! Token will run out in:" + Expires+ " Seconds!");
			 }
			 
			 TS = new TokenSerial(TS.getClientID(), Token, RefreshToken, DateUtils.addSeconds(new Date(), Expires-60).getTime());
			 log.debug("New Expire: "+ TS.getExpiresDateFormatted());
			 TCM.SaveCache(this);
		 }
		 catch(Exception e)
		 {
			 try
			 {
				 BufferedReader BR = new BufferedReader(new InputStreamReader(HTTPC.getErrorStream()));
				 StringBuilder SB = new StringBuilder();
				 
				 String Line;
				 while((Line = BR.readLine()) != null)
				 {
					 SB.append(Line);
				 }
				 
				 BR.close();
				 HTTPC.disconnect();
				 
				 log.debug(SB.toString());
				 
			 }
			 catch(Exception e1)
			 {
				 LogHelper.EtoStringLog(log, e1);
			 }
		 } 
	}
	
	public String getToken() 
	{
		return TS.getToken();
	}

	public boolean hasToken() 
	{
		if(TS.isexpired())
		{
			return false;
		}
		return (TS.getToken() != null && !TS.getToken().isEmpty());
	}

	public boolean isExpired()
	{
		return TS.isexpired();
	}
	
	public String getClientID()
	{
		return TS.getClientID();
	}

	public TokenSerial getTS() 
	{
		return TS;
	}
	
}
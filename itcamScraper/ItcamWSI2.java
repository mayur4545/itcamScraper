package gov.ca.dmv.ea.perf;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import	java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/** 
 * Description: Download statistics from ITCAM VE based on a date
 * File: ItcamWSI2.java
 * Module:  gov.ca.dmv.ea.perf
 * Created: Oct. 9, 2015 
 * @author MWBXL4  
 * @version $Revision: 1.0 $
 * 
 * 2015/10/09: Initial version for getting the daily performance data (THRU, RESP, MEM, CPU, SESS) for WSI2 done
 * 
 * 11/12/2015 Changed to use a Properties file and command arguments
 *            Added steps to create folders for months and dates
 * 12/1/2015  Updated to process preceding 0 for the date
 * 
 * TODO:    Probably split the data per application (context root)
 * 
 * Last Changed: $Date: 2015/10/09 13:50:00 $
 * Last Changed By: $Author: mwbxl4 $
 */

public class ItcamWSI2 {
	  private List<String> cookies;
	  private HttpURLConnection conn;

	  //private final static String m="11";
	  //private final static String dd="11";
	  //private final static String yyyy="2015";
	  //private final static String DATE_DOWNLOAD = "2015-08-31";	 
	  //private final static String SAVE_DIR = "C:/Users/mwbxl4/Documents/WSI2_PROD_PERF/";
	  private final static String [] Month = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
	  private final static	String sTime="00%3A00"; // 00:00
	  private final static	String eTime="23%3A59"; // 23:59
	  
	  private final static	String portalgroup = "3";
	  private final static	String wasgroup = "4";
	  private final static	String wsgroup = "7";
	  
	  private final static	String portal1 = "5";
	  private final static	String portal2 = "6";
	  private final static	String foa1 = "11";
	  private final static	String foa2 = "10";
	  private final static	String app1 = "9";
	  private final static	String app2 = "8";
	  private final static	String ws1 = "12";
	  private final static	String ws2 = "13";
	  
	  private final static	String thru = "14";
	  private final static	String resp = "1";
	  private final static	String heap = "11";
	  private final static	String cpu = "16";
	  private final static	String sess = "18";
	  private final static	String perMin = "3";
	 	  
	  private final static String USER_AGENT = "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.1; WOW64; Trident/7.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; InfoPath.3; .NET4.0C; .NET4.0E)";
	  private final static String ACCEPT = "application/x-ms-application, image/jpeg, application/xaml+xml, image/gif, image/pjpeg, application/x-ms-xbap, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*";
	  private final static String ACCEPT_LANG = "en-US";

	  static int mi = 0;

	  //11/12/2015
	  static String m;
	  static String dd;
	  static String yyyy;	 
	  static String SAVE_DIR;
	  static String HOST_IP;
	  static String ROOT_URL;
	  static String PASSWORD;
	  static String USERID;
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		// ITCAM takes month - 1 format for the month!!!
		String prevUrl, di;
		
		ItcamWSI2 http = new ItcamWSI2();

		//11/12/2015
		if(args.length==5) {
			http.getProps(args[0]);
			m = args[1];
			di = args[2];
			yyyy = args[3];
			PASSWORD = args[4];
		} else {
			System.out.println("Usage: ItcamWSI2 propfile mm dd yyy password!");
			return;				
		}
		// 12-1-2015
		int	d = Integer.parseInt(di);
		dd = new Integer(d).toString();

		mi = Integer.parseInt(m);
		if(mi>0) mi--;
		String mm = new Integer(mi).toString();

		//11/12/2015
		File file = new File(SAVE_DIR+yyyy+"/"+Month[mi]);
		// if file doesnt exists, then create it
		if (!file.exists()) {
			file.mkdir();
		}
		file = new File(SAVE_DIR+yyyy+"/"+Month[mi]+"/"+Month[mi]+"_"+dd+"_"+yyyy);
		// if file doesnt exists, then create it
		if (!file.exists()) {
			file.mkdir();
		}
		
		String url = "http://"+HOST_IP+ROOT_URL+"/en/common/login.jsp";
	    		
		// make sure cookies is turn on
		CookieHandler.setDefault(new CookieManager());


		// 1. Send a "GET" request, so that you can extract the form's data.
		String page = http.GetPageContent(url, "http://"+HOST_IP+ROOT_URL+"/home");
		String postParams = http.getFormParams(page, USERID, PASSWORD);

		// 2. Construct above post's content and then send a POST request for
		// authentication
		http.sendPost("http://"+HOST_IP+ROOT_URL+"/en/common/j_security_check", postParams);

		// 3. success then go to 
		String response;
		response = http.GetPageContent("http://"+HOST_IP+ROOT_URL+"/home", "http://"+HOST_IP+ROOT_URL+"/home");

		/* Get throughput from foa1 */
		prevUrl = "http://"+HOST_IP+ROOT_URL+"/home";
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?newReport=1&reportType=0";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step0=1&recur=0";
		response = http.GetPageContent(url, prevUrl);		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&resetSignal=0%3B&GROUPBOX="+wasgroup+"&SERVERBOX="+foa1;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?step2=1&reportType=0&serverId="+foa1+"&aggregation=&metric="+thru+"&requestType="+perMin+"&requestName=";
		response = http.GetPageContent(url, prevUrl);
		
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&finish=1&presets=8&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "THRU_FOA1.csv");

		/* Get throughput from foa2 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+wasgroup+"&SERVERBOX="+foa2;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=0&serverId="+foa2+"&modifying=1&aggregation=&metric="+thru+"&requestType="+perMin+"&requestName=";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "THRU_FOA2.csv");

		/* Get throughput from app1 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+wasgroup+"&SERVERBOX="+app1;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=0&serverId="+app1+"&modifying=1&aggregation=&metric="+thru+"&requestType="+perMin+"&requestName=";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "THRU_APP1.csv");

		/* Get throughput from app2 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+wasgroup+"&SERVERBOX="+app2;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=0&serverId="+app2+"&modifying=1&aggregation=&metric="+thru+"&requestType="+perMin+"&requestName=";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "THRU_APP2.csv");

		/* Get throughput from portal1 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+portalgroup+"&SERVERBOX="+portal1;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=0&serverId="+portal1+"&modifying=1&aggregation=&metric="+thru+"&requestType="+perMin+"&requestName=";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "THRU_PORTAL1.csv");

		/* Get throughput from portal2 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+portalgroup+"&SERVERBOX="+portal2;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=0&serverId="+portal2+"&modifying=1&aggregation=&metric="+thru+"&requestType="+perMin+"&requestName=";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "THRU_PORTAL2.csv");

		/* Get throughput from ws1 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+wsgroup+"&SERVERBOX="+ws1;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=0&serverId="+ws1+"&modifying=1&aggregation=&metric="+thru+"&requestType="+perMin+"&requestName=";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "THRU_WEBS1.csv");

		/* Get throughput from ws2 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+wsgroup+"&SERVERBOX="+ws2;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=0&serverId="+ws2+"&modifying=1&aggregation=&metric="+thru+"&requestType="+perMin+"&requestName=";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "THRU_WEBS2.csv");

		/* Get response time for foa1 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+wasgroup+"&SERVERBOX="+foa1;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		//url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=0&serverId="+p114+"&modifying=1&aggregation=&metric="+resp+"&requestType="+perMin+"&requestName=%2FEASE";
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=0&serverId="+foa1+"&modifying=1&aggregation=&metric="+resp+"&requestType="+perMin+"&requestName=";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "RESP_FOA1.csv");
		
		/* Get response time for foa2 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+wasgroup+"&SERVERBOX="+foa2;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		//url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=0&serverId="+p114+"&modifying=1&aggregation=&metric="+resp+"&requestType="+perMin+"&requestName=%2FEASE";
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=0&serverId="+foa2+"&modifying=1&aggregation=&metric="+resp+"&requestType="+perMin+"&requestName=";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "RESP_FOA2.csv");
		
		/* Get response time for app1 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+wasgroup+"&SERVERBOX="+app1;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		//url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=0&serverId="+p114+"&modifying=1&aggregation=&metric="+resp+"&requestType="+perMin+"&requestName=%2FEASE";
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=0&serverId="+app1+"&modifying=1&aggregation=&metric="+resp+"&requestType="+perMin+"&requestName=";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "RESP_APP1.csv");
		
		/* Get response time for app2 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+wasgroup+"&SERVERBOX="+app2;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		//url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=0&serverId="+p114+"&modifying=1&aggregation=&metric="+resp+"&requestType="+perMin+"&requestName=%2FEASE";
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=0&serverId="+app2+"&modifying=1&aggregation=&metric="+resp+"&requestType="+perMin+"&requestName=";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "RESP_APP2.csv");
		
		/* Get response time for portal1 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+portalgroup+"&SERVERBOX="+portal1;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		//url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=0&serverId="+p114+"&modifying=1&aggregation=&metric="+resp+"&requestType="+perMin+"&requestName=%2FEASE";
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=0&serverId="+portal1+"&modifying=1&aggregation=&metric="+resp+"&requestType="+perMin+"&requestName=";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "RESP_PORTAL1.csv");
		
		/* Get response time for portal2 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+portalgroup+"&SERVERBOX="+portal2;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		//url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=0&serverId="+p114+"&modifying=1&aggregation=&metric="+resp+"&requestType="+perMin+"&requestName=%2FEASE";
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=0&serverId="+portal2+"&modifying=1&aggregation=&metric="+resp+"&requestType="+perMin+"&requestName=";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "RESP_PORTAL2.csv");
		
		/* Get response time for ws1 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+wsgroup+"&SERVERBOX="+ws1;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		//url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=0&serverId="+p114+"&modifying=1&aggregation=&metric="+resp+"&requestType="+perMin+"&requestName=%2FEASE";
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=0&serverId="+ws1+"&modifying=1&aggregation=&metric="+resp+"&requestType="+perMin+"&requestName=";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "RESP_WEBS1.csv");
		
		/* Get response time for ws2 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+wsgroup+"&SERVERBOX="+ws2;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		//url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=0&serverId="+p114+"&modifying=1&aggregation=&metric="+resp+"&requestType="+perMin+"&requestName=%2FEASE";
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=0&serverId="+ws2+"&modifying=1&aggregation=&metric="+resp+"&requestType="+perMin+"&requestName=";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "RESP_WEBS2.csv");
		
		
		/* Get heap usage from foa1 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?newReport=1&reportType=4";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step0=1&recur=0";
		response = http.GetPageContent(url, prevUrl);		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&resetSignal=0%3B&GROUPBOX="+wasgroup+"&SERVERBOX="+foa1;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?step2=1&reportType=4&serverId="+foa1+"&aggregation=&metric="+heap;
		response = http.GetPageContent(url, prevUrl);
		
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&finish=1&presets=8&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "MEM_FOA1.csv");
		
		/* Get CPU usage from foa1 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+wasgroup+"&SERVERBOX="+foa1;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=4&serverId="+foa1+"&modifying=1&aggregation=&metric="+cpu;
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "CPU_FOA1.csv");

		/* Get Session usage from foa1 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+wasgroup+"&SERVERBOX="+foa1;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=4&serverId="+foa1+"&modifying=1&aggregation=&metric="+sess;
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "SESS_FOA1.csv");

		/* Get heap usage from foa2 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+wasgroup+"&SERVERBOX="+foa2;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=4&serverId="+foa2+"&modifying=1&aggregation=&metric="+heap;
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "MEM_FOA2.csv");

		/* Get CPU usage from foa2 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+wasgroup+"&SERVERBOX="+foa2;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=4&serverId="+foa2+"&modifying=1&aggregation=&metric="+cpu;
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "CPU_FOA2.csv");

		/* Get Session usage from foa2 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+wasgroup+"&SERVERBOX="+foa2;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=4&serverId="+foa2+"&modifying=1&aggregation=&metric="+sess;
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "SESS_FOA2.csv");

		/* Get heap usage from app1 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+wasgroup+"&SERVERBOX="+app1;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=4&serverId="+app1+"&modifying=1&aggregation=&metric="+heap;
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "MEM_APP1.csv");
	
		/* Get CPU usage from app1 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+wasgroup+"&SERVERBOX="+app1;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=4&serverId="+app1+"&modifying=1&aggregation=&metric="+cpu;
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "CPU_APP1.csv");

		/* Get Session usage from app1 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+wasgroup+"&SERVERBOX="+app1;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=4&serverId="+app1+"&modifying=1&aggregation=&metric="+sess;
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "SESS_APP1.csv");

		/* Get heap usage from app2 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+wasgroup+"&SERVERBOX="+app2;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=4&serverId="+app2+"&modifying=1&aggregation=&metric="+heap;
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "MEM_APP2.csv");

		/* Get CPU usage from app2 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+wasgroup+"&SERVERBOX="+app2;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=4&serverId="+app2+"&modifying=1&aggregation=&metric="+cpu;
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "CPU_APP2.csv");

		/* Get Session usage from app2 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+wasgroup+"&SERVERBOX="+app2;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=4&serverId="+app2+"&modifying=1&aggregation=&metric="+sess;
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "SESS_APP2.csv");

		/* Get heap usage from portal1 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+portalgroup+"&SERVERBOX="+portal1;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=4&serverId="+portal1+"&modifying=1&aggregation=&metric="+heap;
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "MEM_PORTAL1.csv");
	
		/* Get CPU usage from portal1 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+portalgroup+"&SERVERBOX="+portal1;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=4&serverId="+portal1+"&modifying=1&aggregation=&metric="+cpu;
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "CPU_PORTAL1.csv");

		/* Get Session usage from portal1 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+portalgroup+"&SERVERBOX="+portal1;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=4&serverId="+portal1+"&modifying=1&aggregation=&metric="+sess;
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "SESS_PORTAL1.csv");

		/* Get heap usage from portal2 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+portalgroup+"&SERVERBOX="+portal2;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=4&serverId="+portal2+"&modifying=1&aggregation=&metric="+heap;
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "MEM_PORTAL2.csv");

		/* Get CPU usage from portal2 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+portalgroup+"&SERVERBOX="+portal2;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=4&serverId="+portal2+"&modifying=1&aggregation=&metric="+cpu;
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "CPU_PORTAL2.csv");

		/* Get Session usage from portal2 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+portalgroup+"&SERVERBOX="+portal2;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=4&serverId="+portal2+"&modifying=1&aggregation=&metric="+sess;
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "SESS_PORTAL2.csv");

		/* Get heap usage from ws1 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+wsgroup+"&SERVERBOX="+ws1;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=4&serverId="+ws1+"&modifying=1&aggregation=&metric="+heap;
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "MEM_WEBS1.csv");
	
		/* Get CPU usage from ws1 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+wsgroup+"&SERVERBOX="+ws1;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=4&serverId="+ws1+"&modifying=1&aggregation=&metric="+cpu;
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "CPU_WEBS1.csv");

		/* Get heap usage from ws2 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+wsgroup+"&SERVERBOX="+ws2;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=4&serverId="+ws2+"&modifying=1&aggregation=&metric="+heap;
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "MEM_WEBS2.csv");

		/* Get CPU usage from ws2 */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?skinPath=skins%2Fsky&reportId=0&noReload=1&modifyStep1=1&dest=1";
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport1?step1=1&modifyStep2=1&GROUPBOX="+wsgroup+"&SERVERBOX="+ws2;
		response = http.GetPageContent(url, prevUrl);
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport2?skinPath=skins%2Fsky&step2=&reportType=4&serverId="+ws2+"&modifying=1&aggregation=&metric="+cpu;
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/createReport3?skinPath=skins%2Fsky&addHourList=&removeHourList=&addWeekList=&removeWeekList=&addDayList=&removeDayList=&addMonthList=&removeMonthList=&step3=1&modifying=1&finish=1&sortOrder=ASC&sortBy=0&reportStartMonth="+mm+"&reportStartDay="+dd+"&reportStartYear="+yyyy+"&reportStartTime="+sTime+"&reportEndMonth="+mm+"&reportEndDay="+dd+"&reportEndYear="+yyyy+"&reportEndTime="+eTime+"&reportDataGrouping=minute";
		response = http.GetPageContent(url, prevUrl);
		
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/reportTrend?skinPath=skins%2Fsky&export=1";
		http.SaveContent(url, prevUrl, "CPU_WEBS2.csv");

		/* logout */
		prevUrl = url;
		url = "http://"+HOST_IP+ROOT_URL+"/ve/logout";
		String result = http.GetPageContent(url, prevUrl);
		//System.out.println(result);

	}

	  private void sendPost(String url, String postParams) throws Exception {

		URL obj = new URL(url);
		conn = (HttpURLConnection) obj.openConnection();

		// Acts like a browser
		conn.setUseCaches(false);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Host", HOST_IP);
		conn.setRequestProperty("User-Agent", USER_AGENT);
		conn.setRequestProperty("Accept", ACCEPT);
		conn.setRequestProperty("Accept-Language", ACCEPT_LANG);
		for (String cookie : this.cookies) {
			conn.addRequestProperty("Cookie", cookie.split(";", 1)[0]);
		}
		conn.setRequestProperty("Connection", "keep-alive");
		conn.setRequestProperty("Referer", "http://"+HOST_IP+ROOT_URL+"/home");
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setRequestProperty("Content-Length", Integer.toString(postParams.length()));

		conn.setDoOutput(true);
		conn.setDoInput(true);

		// Send post request
		DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
		wr.writeBytes(postParams);
		wr.flush();
		wr.close();

		int responseCode = conn.getResponseCode();
		System.out.println("\nSending 'POST' request to URL : " + url);
		System.out.println("Post parameters : " + postParams);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = 
	             new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		
		// For debug
		//System.out.println(response.toString());
		
		//doDownload(response.toString());

	  }

	  private String GetPageContent(String url, String prevUrl) throws Exception {

		URL obj = new URL(url);
		conn = (HttpURLConnection) obj.openConnection();

		// default is GET
		conn.setRequestMethod("GET");

		conn.setUseCaches(false);

		// act like a browser
		conn.setRequestProperty("User-Agent", USER_AGENT);
		conn.setRequestProperty("Accept", ACCEPT);
		conn.setRequestProperty("Accept-Language", ACCEPT_LANG);
		conn.setRequestProperty("Host", HOST_IP);
		conn.setRequestProperty("Connection", "keep-alive");
		conn.setRequestProperty("Referer", prevUrl);
		if (cookies != null) {
			for (String cookie : this.cookies) {
				conn.addRequestProperty("Cookie", cookie.split(";", 1)[0]);
			}
		}
		int responseCode = conn.getResponseCode();
		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = 
	            new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		// Get the response cookies
		setCookies(conn.getHeaderFields().get("Set-Cookie"));

		return response.toString();

	  }

	  private void SaveContent(String url, String prevUrl, String fname) throws Exception {

		URL obj = new URL(url);
		conn = (HttpURLConnection) obj.openConnection();

		// default is GET
		conn.setRequestMethod("GET");

		conn.setUseCaches(false);

		// act like a browser
		conn.setRequestProperty("User-Agent", USER_AGENT);
		conn.setRequestProperty("Accept", ACCEPT);
		conn.setRequestProperty("Accept-Language", ACCEPT_LANG);
		conn.setRequestProperty("Host", HOST_IP);
		conn.setRequestProperty("Connection", "keep-alive");
		conn.setRequestProperty("Referer", prevUrl);
		if (cookies != null) {
			for (String cookie : this.cookies) {
				conn.addRequestProperty("Cookie", cookie.split(";", 1)[0]);
			}
		}
		int responseCode = conn.getResponseCode();
		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = 
	            new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String inputLine;
		// StringBuffer response = new StringBuffer();

		File file = new File(SAVE_DIR+yyyy+"/"+Month[mi]+"/"+Month[mi]+"_"+dd+"_"+yyyy+"/"+fname);
		// if file doesnt exists, then create it
		if (!file.exists()) {
			file.createNewFile();
		}

		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);

		while ((inputLine = in.readLine()) != null) {
			// System.out.println("Line read ["+inputLine+"]");

			bw.write(inputLine);
			bw.newLine();
				
			// response.append(inputLine);
		}
		in.close();
		bw.close();

		// Get the response cookies
		setCookies(conn.getHeaderFields().get("Set-Cookie"));

		// return response.toString();

	  }

	  public String getFormParams(String html, String username, String password)
			throws UnsupportedEncodingException {
		StringBuilder result = new StringBuilder();
		System.out.println("Extracting form's data...");

		Document doc = Jsoup.parse(html);

		Elements lform = doc.getElementsByTag("form");
		for (Element loginform: lform) {

			Elements inputElements = loginform.getElementsByTag("input");
			List<String> paramList = new ArrayList<String>();
			for (Element inputElement : inputElements) {
				String key = inputElement.attr("name");
				String value = inputElement.attr("value");
	
				if (key.equals("j_username"))
					value = username;
				else if (key.equals("j_password"))
					value = password;
				// Skip the button
				if(value.equals("Login"))	continue;
				
				paramList.add(key + "=" + URLEncoder.encode(value, "UTF-8"));
			}
	
	
			// build parameters list
			for (String param : paramList) {
				if (result.length() == 0) {
					result.append(param);
				} else {
					result.append("&" + param);
				}
			}
		}

		//System.out.println(result.toString());
		
		return result.toString();
	  }

	  public List<String> getCookies() {
		return cookies;
	  }

	  public void setCookies(List<String> cookies) {
		this.cookies = cookies;
	  }
	  
	  //11/12/2015
	  public void getProps(String fn) {
			Properties prop = new Properties();
			FileInputStream input = null;
			try {
				input = new FileInputStream(fn);

				// load a properties file
				prop.load(input);

				// set the properties value
				USERID = prop.getProperty("USERID");
				HOST_IP = prop.getProperty("HOST_IP");
				ROOT_URL = prop.getProperty("ROOT_URL");
				SAVE_DIR = prop.getProperty("SAVE_DIR");

			} catch (IOException io) {
				io.printStackTrace();
				System.exit(-1);
			} finally {
				if (input != null) {
					try {
						input.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

			}

	  }
}

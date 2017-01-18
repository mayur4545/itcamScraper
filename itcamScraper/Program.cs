using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using System.Diagnostics;
using System.Collections;
using System.Threading;
using Excel = Microsoft.Office.Interop.Excel;
using System.Text.RegularExpressions;

namespace itcamScraper
{
    class Program
    {
        
        static void Main(string[] args)
        {
            
            //Copies Java Jar files and spreadsheets to My Documents folder
            copyFileFolder();

            //Check last files uploaded last 30 days
            ArrayList dates = checkTargetFolder();
            Console.WriteLine("ITCAM Scraper will process " + dates.Count.ToString() + " days worth of data");
            string itCamPass = new StreamReader(Environment.CurrentDirectory + "\\itCamPassword.txt").ReadLine();
            string networkFolder = new StreamReader(Environment.CurrentDirectory + "\\networkTargetFolderPath.txt").ReadLine();
            string myDocFolder = new StreamReader(Environment.CurrentDirectory + "\\myDocsTargetFolderPath.txt").ReadLine();

            for (int i=0; i<dates.Count; i++)
            {
                DateTime pdate = DateTime.Parse(dates[i].ToString());
                string[] dateStrings = pdate.ToString("MMM dd yyyy").Split(' ');
                string month = dateStrings[0];
                string day = dateStrings[1];
                if (day.StartsWith("0"))
                {
                    day = day.TrimStart('0');
                }
                string year = dateStrings[2];

                string netPath = networkFolder + "\\WSI2_PROD_PERF\\" + year + "\\" + month + "\\" + month + "_" + day + "_" + year;
                string sourcePath = myDocFolder + "\\WSI2_PROD_PERF\\" + year + "\\" + month + "\\" + month + "_" + day + "_" + year;

                Console.WriteLine("Processing ITCAM data for " + dates[i].ToString());
                //Thread.Sleep(5000);
                string javaBatFile = "itcamscraper.bat";
                string javaCommand = "java -cp DailyGathering.jar;jsoup-1.8.3.jar gov.ca.dmv.ea.perf.ItcamWSI2 ItcamWSI2.props " + dates[i].ToString() + " \"" + itCamPass + "\"";
                //run Java Application to download csv files for 1 specific date
                createBatFile(javaBatFile, dates[i].ToString(), javaCommand);
                Console.WriteLine("Executing: " + javaBatFile);
                runBatFile(javaBatFile);
                if (checkCSVs(sourcePath))
                {
                    //run AutoIt script to open and run VB Script in Excel file
                    Console.WriteLine("Executing: runAutoItScript for date" + dates[i].ToString());
                    runAutoItScript(dates[i].ToString());
                    //kill excel process
                    Console.WriteLine("Killing Excel Process(es)");
                    killSpecificExcelFileProcess("EXCEL");
                    Console.WriteLine("Waiting 5 seconds for Excel to be killed");
                    Thread.Sleep(5000); //wait 5 seconds to kill Excel Process before copying files
                    convertChartToImage(sourcePath + "\\THRU_WSI2_Graph.xlsx", sourcePath, "THRU_WSI2_Graph");
                    convertChartToImage(sourcePath + "\\SESS_WSI2_Graph.xlsx", sourcePath, "SESS_WSI2_Graph");
                    convertChartToImage(sourcePath + "\\CPU_WSI2_Graph.xlsx", sourcePath, "CPU_WSI2_Graph");
                    convertChartToImage(sourcePath + "\\MEM_WSI2_Graph.xlsx", sourcePath, "MEM_WSI2_Graph");
                    convertChartToImage(sourcePath + "\\RESP_WSI2_Graph.xlsx", sourcePath, "RESP_WSI2_Graph");

                    //Copy Finished files to target folder
                    try
                    {
                        Console.WriteLine("Copying " + sourcePath + " to " + netPath);
                        CopyFolder(sourcePath, netPath);
                    }
                    catch (Exception ex)
                    {
                        // logError(ex.ToString() + " CopyFolder(sourcePath, netPath) " + "sourcePath= " + sourcePath + " netPath= " + netPath);
                    }
                }
                else
                {
                    Console.WriteLine("Could not verify ITCAM .csv files were downloaded with statistical data.  PLEASE CHECK YOUR ITCAM CREDENTIALS AND RETRY.");
                    Thread.Sleep(15000);
                    Environment.Exit(0);
                }

            }

        }

        private static void convertChartToImage(string strFilePath, string strDestPath, string name)
        {
            try
            {
                Excel.Application excel = new Excel.Application();
                Excel.Workbook wb = excel.Workbooks.Open(strFilePath);

                foreach (Excel.Worksheet ws in wb.Worksheets)
                {
                    Excel.ChartObjects chartObjects = (Excel.ChartObjects)(ws.ChartObjects(Type.Missing));

                    foreach (Excel.ChartObject co in chartObjects)
                    {
                        Excel.Chart chart = (Excel.Chart)co.Chart;
                        //                  app.Goto(co, true);
                        chart.Export(strDestPath + @"\" + name + ".png", "PNG", false);
                    }
                }
                wb.Close();

            }
            catch (Exception ex)
            {
                logError(ex.ToString() + "\n Could not convert Excel chart to image \n");
            }

        }


        private static bool checkCSVs(string sourcePath)
        {   //Checks if the first csv found in the directory contains "javascript", indicating the ITCAM creds used were unsuccessful
            string[] files = Directory.GetFiles(sourcePath, "*.csv");
            return !(new StreamReader(files[0]).ReadToEnd().ToUpper().Contains("JAVASCRIPT"));
        }

        private static void killSpecificExcelFileProcess(string processName)
        {
            var processes = from p in Process.GetProcessesByName(processName)
                            select p;

            foreach (var process in processes)
            {

                process.Kill();
            }
        }

        private static ArrayList checkTargetFolder()
        {
            ArrayList dates = new ArrayList();
            ArrayList scrapeDates = new ArrayList();
            String scrapeDate = DateTime.Now.ToString("MM dd yyyy");
            for(int i=0; i<30; i++)
            {
                dates.Add(DateTime.Now.AddDays(-i).ToString("MM dd yyyy"));
            }
            for(int j=0; j<dates.Count; j++)
            {
                if(!folderExists(dates[j].ToString()))
                {
                    scrapeDates.Add(dates[j].ToString());
                }
            }
            return scrapeDates;
        }

        private static bool folderExists(string date)
        {
            string networkFolder = new StreamReader(Environment.CurrentDirectory + "\\networkTargetFolderPath.txt").ReadLine() + "\\WSI2_PROD_PERF\\";
            string myDocFolder = new StreamReader(Environment.CurrentDirectory + "\\myDocsTargetFolderPath.txt").ReadLine();
            
            DateTime pdate = DateTime.Parse(date);
            string[] dateStrings = pdate.ToString("MMM dd yyyy").Split(' ');
            string month = dateStrings[0];
            string day = dateStrings[1];
            if(day.StartsWith("0"))
            {
                day = day.TrimStart('0');
            }
            string year = dateStrings[2];

            string path = networkFolder + "\\" + year + "\\" + month + "\\" + month + "_" + day + "_" + year ;
            string sourcePath = myDocFolder + "\\WSI2_PROD_PERF\\" + year + "\\" + month + "\\" + month + "_" + day + "_" + year;
            Console.WriteLine(path + " found=" + Directory.Exists(path));
            //Updated code for checking if today's date already exists, delete folder and run the script again to get latest data for today.
            string[] today = DateTime.Now.ToString("MMM dd yyyy").Split(' ');
            if (today[0] == month && today[1]==day && today[2]==year)
            {
                if(Directory.Exists(path))
                {
                    Console.WriteLine("Today's date already found, deleting to get today's latest data " + " found=" + Directory.Exists(path));
                    DeleteDirectory(path);
                }
                if (Directory.Exists(sourcePath))
                {
                    Console.WriteLine("Today's date already found, deleting to get today's latest data " + " found=" + Directory.Exists(sourcePath));
                    DeleteDirectory(sourcePath);
                }
                Thread.Sleep(1500);
                return false;
            }
            string[] yesterday = DateTime.Now.AddDays(-1).ToString("MMM dd yyyy").Split(' ');
            if (yesterday[0] == month && yesterday[1] == day && yesterday[2] == year)
            {
                if (Directory.Exists(sourcePath))
                {
                    StreamReader sr = new StreamReader(sourcePath + "\\CPU_WEBS1.csv");
                    string sampleCPUfile = sr.ReadToEnd();
                    sr.Close();
                    int count = Regex.Matches(sampleCPUfile, "N/A").Count;
                    if (count > 5)  //Assumes at least 5 minutes of data is not available, will delete yesterday's incomplete reports and download it again.
                    {
                        Console.WriteLine(count.ToString() + " minutes of data is not available in " +  sourcePath + "\\CPU_WEBS1.csv ; will delete yesterday's incomplete reports and download it again.");
                        if (Directory.Exists(path))
                        {
                            Console.WriteLine("yesterday's date already found, deleting to get yesterday's latest data " + " found=" + Directory.Exists(path));
                            DeleteDirectory(path);
                        }
                        Console.WriteLine("yesterday's date already found, deleting to get yesterday's latest data " + " found=" + Directory.Exists(sourcePath));
                        DeleteDirectory(sourcePath);
                        Thread.Sleep(1500);
                        return false;
                    }
                }
            }
            return Directory.Exists(path);
            
        }
        public static void DeleteDirectory(string target_dir)
        {
            string[] files = Directory.GetFiles(target_dir);
            string[] dirs = Directory.GetDirectories(target_dir);

            foreach (string file in files)
            {
                File.SetAttributes(file, FileAttributes.Normal);
                File.Delete(file);
            }

            foreach (string dir in dirs)
            {
                DeleteDirectory(dir);
            }

            Directory.Delete(target_dir, true);
        }
        private static void runAutoItScript(string scrapeDate)
        {
            string batFile = "autoItExcel.bat";
            string myDocFolder = new StreamReader(Environment.CurrentDirectory + "\\myDocsTargetFolderPath.txt").ReadLine();
            DateTime pdate = DateTime.Parse(scrapeDate);
            string[] dateStrings = pdate.ToString("MMM dd yyyy").Split(' ');
            string month = dateStrings[0];
            string day = dateStrings[1];
            if (day.StartsWith("0"))
            {
                day = day.TrimStart('0');
            }
            string year = dateStrings[2];
            string command =  myDocFolder + "\\WSI2_PROD_PERF\\ExcelMacro.exe " + "\"" + myDocFolder + "\\WSI2_PROD_PERF\\" + year + "\\WSI2PerfReports.xlsm\"" + " \"" + month + "_" + day + "_" + year + "\"";
            //run Java Application to download csv files for 1 specific date
            createBatFile(batFile, scrapeDate, command);
            runBatFile(batFile);
        }

        private static void copyFileFolder()
        {
            //Get path from text file
            try
            {
                string myDocFolder = new StreamReader(Environment.CurrentDirectory + "\\myDocsTargetFolderPath.txt").ReadLine();
                
                string sourceFolder = Environment.CurrentDirectory + "\\source";
                if (Directory.Exists(myDocFolder + "\\" + "WSI2_PROD_PERF"))
                {
                    Console.WriteLine(myDocFolder + "\\" + "WSI2_PROD_PERF " + "    Folder already exists");
                }
                else
                {
                    CopyFolder(sourceFolder, myDocFolder);
                    Console.WriteLine(sourceFolder + " copied successfully to " + myDocFolder);
                }
            }
            catch (Exception ex)
            {
                logError(ex.ToString());
            }
        }

        private static void runBatFile(string batFile)
        {
            Process proc = null;
            try
            {
                string myDocFolder = new StreamReader(Environment.CurrentDirectory + "\\myDocsTargetFolderPath.txt").ReadLine();
                proc = new Process();
                proc.StartInfo.WorkingDirectory = myDocFolder + "\\WSI2_PROD_PERF\\";
                proc.StartInfo.FileName = batFile;
                proc.StartInfo.CreateNoWindow = false;
                proc.Start();
                Console.WriteLine("proc.StartInfo.WorkingDirectory: " + proc.StartInfo.WorkingDirectory + " " + batFile + " executed");
                proc.WaitForExit();
            }
            catch (Exception ex)
            {
                logError(ex.StackTrace.ToString() + "string batFile= " + batFile + "proc.StartInfo.WorkingDirectory= " + proc.StartInfo.WorkingDirectory);
            }

        }

        private static void logError(string errorMsg)
        {
            try
            {
                Directory.CreateDirectory(Environment.CurrentDirectory + "\\errorLog");
                StreamWriter sw = new StreamWriter(Environment.CurrentDirectory + "\\errorLog\\" + "errorLog" + DateTime.Now.ToString("yyyy_MM_dd_HH_ss") + ".txt");
                sw.WriteLine(errorMsg);
                sw.Close();
                Console.Write(errorMsg + " Enter any key to exit");
                Console.ReadKey();
            }
            catch(Exception ex)
            {
                Console.Write(ex.ToString() + " Enter any key to exit");
                Console.ReadKey();
            }
        }

        private static void createBatFile(string batFile, string scrapeDate, string command)
        {
            try
            {
                string myDocFolder = new StreamReader(Environment.CurrentDirectory + "\\myDocsTargetFolderPath.txt").ReadLine();
                StreamWriter sw = new StreamWriter(myDocFolder + "\\WSI2_PROD_PERF\\" + batFile);  
                sw.WriteLine(command);
                sw.Close();
            }
            catch (Exception ex)
            {
                logError(ex.StackTrace.ToString() + "string batFile= " + batFile + ", string scrapeDate= " + scrapeDate + " string command= " + command);
            }
        }

        static public void CopyFolder(string sourceFolder, string destFolder)
        {
            if (!Directory.Exists(destFolder))
                Directory.CreateDirectory(destFolder);
            string[] files = Directory.GetFiles(sourceFolder);
            foreach (string file in files)
            {
                string name = Path.GetFileName(file);
                string dest = Path.Combine(destFolder, name);
                File.Copy(file, dest, true);
                File.SetAttributes(dest, FileAttributes.Normal);
            }
            string[] folders = Directory.GetDirectories(sourceFolder);
            foreach (string folder in folders)
            {
                string name = Path.GetFileName(folder);
                string dest = Path.Combine(destFolder, name);
                CopyFolder(folder, dest);
            }
        }
    }
}


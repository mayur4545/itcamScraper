using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using System.Diagnostics;
using System.Collections;
using System.Threading;

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
                Console.WriteLine("Processing ITCAM data for " + dates[i].ToString());
                //Thread.Sleep(5000);
                string javaBatFile = "itcamscraper.bat";
                string javaCommand = "java -cp DailyGathering.jar;jsoup-1.8.3.jar gov.ca.dmv.ea.perf.ItcamWSI2 ItcamWSI2.props " + dates[i].ToString() + " \"" + itCamPass + "\"";
                //run Java Application to download csv files for 1 specific date
                createBatFile(javaBatFile, dates[i].ToString(), javaCommand);
                Console.WriteLine("Executing: " + javaBatFile);
                runBatFile(javaBatFile);
                //run AutoIt script to open and run VB Script in Excel file
                Console.WriteLine("Executing: runAutoItScript for date" + dates[i].ToString());
                runAutoItScript(dates[i].ToString());
                //kill excel process
                Console.WriteLine("Killing Excel Process(es)");
                killSpecificExcelFileProcess("EXCEL");
                Console.WriteLine("Waiting 5 seconds for Excel to be killed");
                Thread.Sleep(5000); //wait 5 seconds before copying files
                //Copy Finished files to target folder
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
            String scrapeDate = DateTime.Now.AddDays(-1).ToString("MM dd yyyy");
            for(int i=1; i<30; i++)
            {
                dates.Add(DateTime.Now.AddDays(-i).ToString("MM dd yyyy"));
            }
            for(int j=0; j<dates.Count; j++)
            {
                if(folderExists(dates[j].ToString()))
                {
                    scrapeDates.Add(dates[j].ToString());
                }
            }
            return dates;
        }

        private static bool folderExists(string date)
        {
            string networkFolder = new StreamReader(Environment.CurrentDirectory + "\\networkTargetFolderPath.txt").ReadLine() + "\\WSI2_PROD_PERF\\";
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
            Console.Write(path + " found=" + Directory.Exists(path));
            return Directory.Exists(path);
            
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

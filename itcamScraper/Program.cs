using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using System.Diagnostics;


namespace itcamScraper
{
    class Program
    {
        
        static void Main(string[] args)
        {
            string batFile = "itcamscraper.bat";
           // createBatFile(batFile);
           // runBatFile(batFile);


            //Check last files uploaded last 30 days
            copyFileFolder();
            createBatFile(batFile);
            runBatFile(batFile);
            //run AutoIt script to open and run VB Script in Excel file
            runAutoItScript();

        }

        private static void runAutoItScript()
        {
          
        }

        private static void copyFileFolder()
        {
            //Get path from text file
            try
            {
                string myDocFolder = new StreamReader(Environment.CurrentDirectory + "\\targetFolderPath.txt").ReadLine();
               
                string sourceFolder = Environment.CurrentDirectory + "\\source";
                CopyFolder(sourceFolder, myDocFolder);
                Console.WriteLine(sourceFolder + " copied successfully to " + myDocFolder);
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
                string myDocFolder = new StreamReader(Environment.CurrentDirectory + "\\targetFolderPath.txt").ReadLine();
                proc = new Process();
                proc.StartInfo.WorkingDirectory = myDocFolder + "WSI2_PROD_PERF\\";
                Console.WriteLine("proc.StartInfo.WorkingDirectory: " + proc.StartInfo.WorkingDirectory + " executed");
                proc.StartInfo.FileName = batFile;
                proc.StartInfo.CreateNoWindow = false;
                proc.Start();
                proc.WaitForExit();
                Console.WriteLine( batFile + " executed");
            }
            catch (Exception ex)
            {
                logError(ex.StackTrace.ToString());
            }

        }

        private static void logError(string errorMsg)
        {
            try
            {
                Directory.CreateDirectory(Environment.CurrentDirectory + "\\errorLog");
                StreamWriter sw = new StreamWriter(Environment.CurrentDirectory + "\\errorLog\\" + "errorLog" + DateTime.Now.AddDays(-1).ToString("yyyy_MM_dd_HH_ss") + ".txt");
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

        private static void createBatFile(string batFile)
        {
            try
            {
                string myDocFolder = new StreamReader(Environment.CurrentDirectory + "\\targetFolderPath.txt").ReadLine();
                StreamWriter sw = new StreamWriter(myDocFolder + "\\WSI2_PROD_PERF\\" + batFile);
                String scrapeDate = DateTime.Now.AddDays(-1).ToString("MM dd yyyy");
                String javaCommand = "java -cp DailyGathering.jar;jsoup-1.8.3.jar gov.ca.dmv.ea.perf.ItcamWSI2 ItcamWSI2.props " + scrapeDate + " \"mwmz366\"";
                sw.WriteLine(javaCommand);
                sw.Close();
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.StackTrace.ToString());
                Console.ReadKey();
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

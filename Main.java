package com.company;

import java.io.*;
import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {

        System.out.println("HYBRID cache program Start!\n\n");

        String Filename = args[0];
        System.out.println("Cache List is :" + Filename);

        ArrayList<LogEntry> CacheArr = new ArrayList<>();
        CacheParm CP1 = new CacheParm();

        CP1.epabw = Double.parseDouble(args[1]);
        System.out.println("epa Bandwidth is :" + CP1.epabw);
        CP1.epacd = Double.parseDouble(args[2]);
        System.out.println("epa Connection Delay is :" + CP1.epacd);
        CP1.clarkbw = Double.parseDouble(args[3]);
        System.out.println("clark Bandwidth is :" + CP1.clarkbw);
        CP1.clarkcd = Double.parseDouble(args[4]);
        System.out.println("clark Connection Delay is :" + CP1.clarkcd);
        CP1.Wb = Double.parseDouble(args[5]);
        System.out.println("Wb is :" + CP1.Wb);
        CP1.Wf = Double.parseDouble(args[6]);
        System.out.println("Wf is :" + CP1.Wf);
        CP1.cachesize = Double.parseDouble(args[7]) * 1024 * 1024; // As it is in Megabytes
        System.out.println("Cache Size is :" + CP1.cachesize);
        double cacheUtilization = 0;

        BufferedReader br = null;
        String logline = "";
        String SplitBy = " ";
        int count = 0;

        try {

            br = new BufferedReader(new FileReader(Filename));
            while ((logline = br.readLine()) != null) {

                // Check if cache is full
                cacheUtilization = CacheUtil(CacheArr);
                // use comma as separator
                String[] accessline = logline.split(SplitBy);

                LogEntry LE1 = new LogEntry();

                LE1.server = accessline[0];
                LE1.document = accessline[3];
                LE1.size = Double.parseDouble(accessline[4].trim());
                LE1.freq = 1;
                LE1.uv = 0;
                int inCache = -1;
                if(CacheArr.size() > 0)
                    inCache = CacheCheck(LE1.document, CacheArr);

                if(inCache < 0)
                    LE1.uv = CalculateUV(LE1, CP1);

                System.out.print("\nLine " + count + ":Space Left = " + (CP1.cachesize - cacheUtilization) + "\t\tItems in cache = " + CacheArr.size());

                switch(inCache)
                {
                    case -1:
                            System.out.print("\t\tEmpty Cache returned. Added Item " + LE1.document + " with UV " + LE1.uv);
                            CacheArr.add(LE1);
                            break;

                    case -2:
                            if (cacheUtilization + LE1.size < CP1.cachesize) {
                                System.out.print("\t\tItem not in Cache and fits without replacement: " + LE1.document + " with UV " + LE1.uv);
                                CacheArr.add(LE1);
                            } else {
                                LE1.uv = CalculateUV(LE1, CP1);
                                CacheReplacement(LE1, CacheArr, CP1.cachesize);
                            }
                            break;

                    default:
                            ++CacheArr.get(inCache).freq;
                            CacheArr.get(inCache).uv = CalculateUV(CacheArr.get(inCache), CP1);
                            System.out.print("\t\tUpdated Item " + LE1.document + " with UV: " + CacheArr.get(inCache).uv);
                            break;
                }

                count++;
                //if(count > 101110) break;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("\n\n\t"+ --count + " Records scanned.");

    }//main fn

    private static double CalculateUV(LogEntry LE1, CacheParm CP1)
    {
        double calculateduv = 0;
        if(LE1.server.equals("epa"))
        {
            calculateduv = (((CP1.epacd + (CP1.Wb/CP1.epabw))*(Math.pow(LE1.freq,CP1.Wf)))/LE1.size);
        }
        else if(LE1.server.equals("clark"))
        {
            calculateduv = (((CP1.clarkbw + (CP1.Wb/CP1.clarkbw))*(Math.pow(LE1.freq,CP1.Wf)))/LE1.size);
        }
        //System.out.print("\nDocument: "  + LE1.document + "\tOld UV: " + LE1.uv + "\tNew uv: " + calculateduv);
        return calculateduv;
    }

    private static int CacheCheck(String Document, ArrayList<LogEntry> CacheArr)
    {
        for(int i = 0; i< CacheArr.size(); i++)
        {
            if(CacheArr.get(i).document.equals(Document)) return i;
        }
        return -2;
    }

    private static int CacheReplacement(LogEntry LE1, ArrayList<LogEntry> CacheArr, double cachesize)
    {
        double replacementsize = 0,currentUtilization = CacheUtil(CacheArr);
        ArrayList<Integer> lowerUVItems = new ArrayList<>();

        try {
            for (int i = 0; i < CacheArr.size(); i++) {
                if (CacheArr.get(i).uv < LE1.uv) {
                    replacementsize = replacementsize + CacheArr.get(i).size;
                    lowerUVItems.add(i);
                }
                if (replacementsize > LE1.size)
                    break;
            }
            if (replacementsize == 0) {
                System.out.print("\t\tNo Lower UV. Item not put in Cache : " + LE1.document);
                return -1;
            } else if (((currentUtilization - replacementsize) + LE1.size) < cachesize) {
                for (int i = 0; i < lowerUVItems.size(); i++) {
                    System.out.print("\n\t\t\t\t\t\t\t\t\t\t\t\tItem replaced: " + CacheArr.get(lowerUVItems.get(i).intValue()).document + " with UV " + CacheArr.get(lowerUVItems.get(i).intValue()).uv);
                    CacheArr.remove(lowerUVItems.get(i).intValue());
                }
                System.out.println("\n\t\t\t\t\t\t\t\t\t\t\t\tItem added: " + LE1.document + " with UV " + LE1.uv);
                CacheArr.add(LE1);
                return 1;
            }
        }
        catch(IndexOutOfBoundsException e){
            e.printStackTrace();
        }
        return 0;
    }

    private static double CacheUtil(ArrayList<LogEntry> CacheArr)
    {
        double cacheUtilization = 0;
        for(int i = 0; i< CacheArr.size(); i++)
        {
            cacheUtilization = cacheUtilization + CacheArr.get(i).size;
        }
        return cacheUtilization;
    }

}//class

/*
 * Created on Apr 15, 2004
 *
 * (c) 2004 San Francisco County Transportation Authority.
 * 
 */
package org.sfcta.fastpass;

import java.util.*;
import java.io.*;

import com.pb.common.util.AppProperties;
import com.svcon.jdbf.*;

/**
 * @author tad
 *
 * (c) 2004 San Francisco County Transportation Authority.
 */
public class FastPass {

	public static int A = 0;
	public 	static int B = 1;
	public 	static int TIME = 2;
	public 	static int MODE = 3;
	public 	static int PLOT = 4;
	public 	static int COLOR = 5;
	public 	static int STOP_A = 6;
	public 	static int STOP_B = 7;
	public 	static int DIST = 8;
	public 	static int NAME = 9;
	public 	static int SEQ = 10;
	public 	static int OWNER = 11;
	public 	static int VOL = 12;
	public 	static int BRDA = 13;
	public 	static int XITA = 14;
	public 	static int BRDB = 15;
	public 	static int XITB = 16;
    
	public static String[] mTimePeriods = {"am","md","pm","ev","ea"};  
	public static String[] mPaths = //{"nsatw","nswta","nswtw",
//	        					"sfwlw","sfabw","sfapw","sfwba","sfwbw","sfwmw","sfwpa","sfwpw"};
		{"sfwlw","nswtw"};

    Hashtable mLineInterest = new Hashtable();
    Hashtable mStationInterest = new Hashtable();
    String mSelectedTimePeriods = "";
    
    public static void main(String[] args) {
        System.err.println("SFCTA FastPass:    Transit Assignment Summary Tool");

        new FastPass(args);
    }

    /**
     * Constructor.  Read the ctl file, and start everything. 
     */
    public FastPass(String[] args) {
        
        try {
            AppProperties ctlFile = new AppProperties(args[0]);

            String lines = ctlFile.getProperty("Lines","");
            if (!"".equals(lines))
                prepareLineLevelBoardings(lines);

            String stations = ctlFile.getProperty("Stations","");
            if (!"".equals(stations))
                prepareStationLevelBoardings(stations);

            mSelectedTimePeriods = ctlFile.getProperty("TimePeriods","am,md,pm,ev,ea");
        } catch (Exception e) {System.exit(8);}

        readDBFs();
        results();
    }

    /**
     * Populate the list of stations we're interested in for the station
     * level analysis.
     * 
     * @param lines Comma-separated list of station nodes to be reported.
     */
    void prepareStationLevelBoardings(String nodes) {
        StringTokenizer st = new StringTokenizer(nodes,";,\t ");
        while (st.hasMoreTokens()) {
            String node = st.nextToken();
            mStationInterest.put(node,new TransitStop(node));
        }
    }

    /**
     * Populate the list of lines we're interested in for the station
     * level analysis.
     * 
     * @param lines Comma-separated list of transit line names
     */
     void prepareLineLevelBoardings(String lines) {
        StringTokenizer st = new StringTokenizer(lines,";,\t ");
        while (st.hasMoreTokens()) {
            String trnLine = st.nextToken();
            mLineInterest.put(trnLine,new TransitLine(trnLine));
        }
    }

    /**
     * Read the required DBF files and populate the data vectors.
     */
    void readDBFs() {
        int steps = mPaths.length * (1+mSelectedTimePeriods.length())/3;
        for (int i=0; i<steps;i++) System.err.print("-"); System.err.println("");
        
        for (int period = 0; period < mTimePeriods.length; period++) {

          // Be sure this time period is part of the analysis
          if (-1 == mSelectedTimePeriods.indexOf(mTimePeriods[period]))
                continue;

          for (int path = 0; path < mPaths.length; path++) {

            String dbfFile = mPaths[path]+mTimePeriods[period]+".dbf";
            DBFReader dbf = null;
            
            try {
                System.err.print("=");
                dbf = new DBFReader(dbfFile);

                while (dbf.hasNextRecord()) {
                    Object[] fields = dbf.nextRecord();

                    long seq = ((Long)fields[SEQ]).longValue();
                    
                    // Sequence "0" records are zone-centroids; skip.
                    if (0 == seq) {
                        continue;
                    }

                    // Check station interest.
                    // If first link in a sequence, check its ANODE.
                    if (1 == seq && null != mStationInterest.get(fields[A].toString())
                            && "1".equals(fields[STOP_A].toString())) {
                        TransitStop ts = (TransitStop) mStationInterest.get(fields[A].toString());
                        ts.addVol(fields[NAME].toString(),period,fields[BRDA],fields[XITA]);
                    }
                    // And for all links, check their BNODE.
                    if (null != mStationInterest.get(fields[B].toString())
                            && "1".equals(fields[STOP_B].toString())) {
                        TransitStop ts = (TransitStop) mStationInterest.get(fields[B].toString());
                        ts.addVol(fields[NAME].toString(),period,fields[BRDB],fields[XITB]);
                    }
                    
                    
                    // Check for line interest
                    String name = fields[NAME].toString();
                    TransitLine line = (TransitLine) mLineInterest.get(name);

                    if (line != null) {
                      line.addLink(new TransitLink(fields),period);
                    }

                }
                dbf.close();
                dbf = null;
            } catch (JDBFException je) {je.printStackTrace();}
          }
        }
    }
    

    /**
     * Summarize the results and print them out.
     */
    void results() {
        System.out.println("\n");
        StringBuffer sb;
        PrintWriter pw = null;
        try {
            pw = new PrintWriter (
                    		 new BufferedWriter (
                    		 new FileWriter("fastpass.rpt")));
            
            sb = reportStationLevelResults();
            pw.println(sb);

            sb = reportLineLevelResults();
            pw.println(sb);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    StringBuffer reportStationLevelResults() {
        Iterator stations = mStationInterest.values().iterator();
        StringBuffer sb = new StringBuffer();
        while (stations.hasNext()) {
            TransitStop trStop = (TransitStop) stations.next();
            sb.append(trStop.reportStations());
        }
        return sb;
    }
    

    StringBuffer reportLineLevelResults() {
        Iterator lines = mLineInterest.values().iterator();
        StringBuffer sb = new StringBuffer();
        while (lines.hasNext()) {
            TransitLine trLine = (TransitLine) lines.next();
            sb.append(trLine.reportStations());
        }
        return sb;
    }

    String ralign(String text, int width) {
        StringBuffer sb = new StringBuffer();
        int padding = width - text.length();
        while (--padding >= 0) sb.append(" ");
        sb.append(text);
        return (sb.toString());
    }

    
}


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
import jxl.*;
import jxl.write.*;

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
	public static String[] mPaths = 
		{"nsatw","nswta","nswtw","sfwlw","sfabw","sfapw","sfwba","sfwbw","sfwmw","sfwpa","sfwpw"};
//		{"sfwlw","nswtw"};

    Hashtable mLineInterest = new Hashtable();
    Hashtable mStationInterest = new Hashtable();
    Hashtable mSummaryInterest = new Hashtable();
    String mSelectedTimePeriods = "";
    String mOutFile = "fastpass.xls";
    boolean mPrepareSummary = true;
    WritableWorkbook wb = null;
    
    public static void main(String[] args) {
        System.err.println("SFCTA FastPass:    Transit Assignment Summary Tool");
        if (args.length == 0) {
            System.err.println("Usage:\nfastpass  ctlfile  [outfile]\n\n");
            System.exit(8);
        }

        new FastPass(args);
    }

    /**
     * Constructor.  Read the ctl file, and start everything. 
     */
    public FastPass(String[] args) {
        
        try {
            AppProperties ctlFile = new AppProperties(args[0]);
            if (args.length > 1)
                mOutFile = args[1];

            String lines = ctlFile.getProperty("Lines","");
            if (!"".equals(lines))
                prepareLineLevelBoardings(lines);

            String stations = ctlFile.getProperty("Stations","");
            if (!"".equals(stations))
                prepareStationLevelBoardings(stations);

            String summary = ctlFile.getProperty("Summary","");
            if (!"".equals(summary))
                mPrepareSummary = true;

            mSelectedTimePeriods = ctlFile.getProperty("TimePeriods","am,md,pm,ev,ea");

            wb = Workbook.createWorkbook(new File(mOutFile));

	        readDBFs();
	        results();
        } catch (IOException e) {
            System.err.println("Error writing output file; is it already open?");
        } catch (Exception e) {
            e.printStackTrace();
        }
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

                    TransitLink link = null;
                    
                    if (line != null) {
                        link = new TransitLink(fields);
                        line.addLink(link, period);
                    }

                    // Accumulate summary stats
                    if (mPrepareSummary) {
                        TransitLine sumLine = (TransitLine) mSummaryInterest.get(name);

                        if (null == sumLine) {
                            sumLine = new TransitLine(name);
                            mSummaryInterest.put(name,sumLine);
                        }
                        
                        if (null == link) {
                            link = new TransitLink(fields);
                        }
                        sumLine.addSummary(link,period);
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

        reportStationLevelResults();
        reportLineLevelResults();
        reportSummaryResults();
    }

    void reportStationLevelResults() {
		try {
            WritableSheet sheet = wb.createSheet("Stations",1);
	        Iterator stations = mStationInterest.values().iterator();
	        while (stations.hasNext()) {
	            TransitStop trStop = (TransitStop) stations.next();
	            trStop.reportStations(sheet);
	        }
	    } catch (Exception e) {}
    }
    
    void reportSummaryResults() {

		TransitLine.lineCount = 0;

		try {
            WritableSheet sheet = wb.createSheet("LineStats",0);
            
			WritableCellFormat font =  new WritableCellFormat (new WritableFont(
				WritableFont.ARIAL, 10, WritableFont.BOLD, false));
			sheet.addCell(new Label(0,0,"Transit Line Summary", font));
			sheet.addCell(new Label(1,1,"Daily", font));            
			sheet.addCell(new Label(5,1,"AM", font));            
			sheet.addCell(new Label(9,1,"MD", font));            
			sheet.addCell(new Label(13,1,"PM", font));            
			sheet.addCell(new Label(17,1,"EV", font));            
			sheet.addCell(new Label(21,1,"EA", font));         
			sheet.addCell(new Label(0,2,"Line Name", font));
			for (int i = 0; i<6; i++) {
				sheet.addCell(new Label(i*4+1,2,"Boards", font));			
				sheet.addCell(new Label(i*4+2,2,"PassMi", font));			
				sheet.addCell(new Label(i*4+3,2,"PassHr", font));			
			}         

            // Sort the lines
            Vector c = new Vector(mSummaryInterest.values());
            Collections.sort(c);
            Enumeration lines = c.elements();

            while (lines.hasMoreElements()) {
                TransitLine trLine = (TransitLine) lines.nextElement();
                trLine.reportSummary(sheet);
            }
            wb.write();
            wb.close();
        } catch (Exception e) {
            System.err.println("Error writing output file; is it already open?");
        }
    }
    

    void reportLineLevelResults() {
        // Sort the lines
        Vector c = new Vector(mLineInterest.values());
        Collections.sort(c);
        Enumeration lines = c.elements();
		TransitLine.lineCount = 1;
		
		try {
            WritableSheet sheet = wb.createSheet("Line Detail",2);
            WritableCellFormat font =  new WritableCellFormat (new WritableFont(
            		WritableFont.ARIAL, 10, WritableFont.BOLD, false));
            sheet.addCell(new Label(0,0,"Line Detail Report", font));

	        while (lines.hasMoreElements()) {
	            TransitLine trLine = (TransitLine) lines.nextElement();
	            trLine.reportStations(sheet);
	        }
	    } catch (Exception e) {}
    }

    String ralign(String text, int width) {
        StringBuffer sb = new StringBuffer();
        int padding = width - text.length();
        while (--padding >= 0) sb.append(" ");
        sb.append(text);
        return (sb.toString());
    }

    /**
     * Split line name into sections by text/numerical divisions. 
     * @return Array of text chunks that comprise this line name.
     */
    public static Vector getNameChunks(String name) {
        Vector mNameChunks = new Vector();
        StringBuffer chunk = new StringBuffer();
        chunk.append(name.charAt(0));
        boolean isDigit = Character.isDigit(name.charAt(0));

        for (int i = 1; i<name.length(); i++) {
            char z = name.charAt(i);
            if (Character.isDigit(z) == isDigit) {
                chunk.append(z);
            } else {
                mNameChunks.add(chunk.toString());
                isDigit = Character.isDigit(z);
                chunk = new StringBuffer();
                chunk.append(z);
            }
        }
        mNameChunks.add(chunk.toString());
            
        return mNameChunks;
    }
}


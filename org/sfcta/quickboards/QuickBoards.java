/*
 * Created on Apr 15, 2004 by Billy Charlton
 *
 * (c) 2010, 2013 San Francisco County Transportation Authority.
 * 
 *http://github.com/sfcta/quickboards
 *
 *Timesheet, Copyright 2013 San Francisco County Transportation Authority
 *                          San Francisco, CA, USA
 *                          http://www.sfcta.org/
 *                          info@sfcta.org
 *
 *This file is part of Quickboards.
 *
 *Quickboards is free software: you can redistribute it and/or modify
 *it under the terms of the GNU General Public License as published by
 *the Free Software Foundation, either version 3 of the License, or
 *(at your option) any later version.
 *
 *Quickboards is distributed in the hope that it will be useful,
 *but WITHOUT ANY WARRANTY; without even the implied warranty of
 *MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *GNU General Public License for more details.
 *
 *You should have received a copy of the GNU General Public License
 *along with Timesheet.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.sfcta.quickboards;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.*;


import com.pb.common.util.AppProperties;
import com.svcon.jdbf.*;
import jxl.*;
import jxl.write.*;

/**
 * @author tad
 *
 * (c) 2010 San Francisco County Transportation Authority.
 */
public class QuickBoards {

	public int DBFCOLUMNS = 17;

	public   int A =      0;
	public 	 int B =      1;
	public 	 int TIME =   2;
    public   int MODE =   3;
    public   int FREQ =   4;
	public 	 int PLOT =   5;
	public 	 int COLOR =  6;
	public 	 int STOP_A = 7;
	public 	 int STOP_B = 8;
	public 	 int DIST =   9;
	public 	 int NAME =  10;
	public 	 int SEQ =   11;
	public 	 int OWNER = 12;
	public 	 int VOL =   13;
	public 	 int BRDA =  14;
	public 	 int XITA =  15;
	public 	 int BRDB =  16;
	public 	 int XITB =  17;
    
	public static String[] mTimePeriods = {"am","md","pm","ev","ea"};  
	public static WritableCellFormat bold_font = new WritableCellFormat (
			new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD, false));
	public Vector vPaths = new Vector();
    
    //		{"sfwlw","nswtw"};

    Hashtable mLineInterest = new Hashtable();
    Hashtable mStationInterest = new Hashtable();
    Hashtable mSummaryInterest = new Hashtable();
    Hashtable mLinesNotInterested = new Hashtable();
    Vector mLinePatterns = new Vector();
    String mSelectedTimePeriods = "";
    String mOutFile = "quickboards.xls";
	String mNodesFile = null;
    boolean mPrepareSummary = true;
    WritableWorkbook wb = null;
    static Hashtable mNodeLookup;
    
    public static void main(String[] args) {
        System.err.println("\nSFCTA QuickBoards:    Transit Assignment Summary Tool");
        if (args.length == 0) {
            System.err.println("Usage:\nquickboards  ctlfile  [outfile]\n");
            System.err.println("Control file keywords:\n\n");
            System.err.println("NodesFile=  [f:\\champ\\util\\nodes.xls] path of nodes.xls lookup file");
            System.err.println("TimePeriods=[am,md,pm,ev,ea] list of time periods to analyze");
            System.err.println("LineStats=  [t|f] true/false to create summary stats for all lines");
            System.err.println("Lines=      [] csv list of TP+ line names for station-level boardings");
            System.err.println("Stations=   [] csv list of TP+ nodes for detailed boardings by line");
            System.err.println("Paths=      [] csv list of five-letter codes of transit dbf's to load (nswtw,nswta,nsatw,sfwlw,sfabw,sfapw,sfwba,sfwbw,sfwmw,sfwpa,sfwpw,viswmw)");
            System.err.println("Summary=    [t|f] true/false to generate line-level summary (true)\n");
            
            System.exit(2);
        }

        new QuickBoards(args);
    }

    /**
     * Constructor.  Read the ctl file, and start everything. 
     */
    public QuickBoards(String[] args) {
        
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

            mNodesFile = ctlFile.getProperty("NodesFile","f:\\champ\\util\\nodes.xls");

            String summary = ctlFile.getProperty("Summary","t");
            if ("f".equals(summary))
                mPrepareSummary = false;

            mSelectedTimePeriods = ctlFile.getProperty("TimePeriods","am,md,pm,ev,ea");

            // Parse out the DBFs to read
            String paths = ctlFile.getProperty("Paths","nswtw,nswta,nsatw,sfwlw,sfabw,sfapw,sfwba,sfwbw,sfwmw,sfwpa,sfwpw,viswmw");
            if (!paths.equals("")) {
                StringTokenizer st = new StringTokenizer(paths,",");
                while (st.hasMoreTokens()) {
                    vPaths.add(st.nextToken());
                }
            } 
            
            wb = Workbook.createWorkbook(new File(mOutFile));
            mNodeLookup = populateNodeLookup();

            readDBFs();
	        results();

        } catch (IOException e) {
            System.err.println("Error writing output file; is it already open?");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    Hashtable populateNodeLookup() {
        Hashtable table = new Hashtable();
        Workbook nlookup = null;
        int i = 0;

        try {
                System.err.print("Reading node equivalency: ");
                nlookup = Workbook.getWorkbook(new File(mNodesFile));
                Sheet sheet = nlookup.getSheet(0);

                // Now read cells until we hit the end.
                while (true) {
                    String node = sheet.getCell(0,i).getContents();
                    String value= sheet.getCell(1,i).getContents();
//                    if (null == node || node.equals(""))
//                        break;
                    table.put(node,value);
//                    System.out.println(node);
                    i++;
                }
            } catch (IOException e) {
                System.err.println("Couldn't open nodes.xls; be sure it exists in run directory");
            } catch (Exception e) {
                System.out.println(""+i+" node labels found.");
                nlookup.close();
            }
        return table;
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

            // This cruft just converts asterisks into regex patterns.
            // I hate regex patterns. 
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < trnLine.length(); i++) {
                char c = trnLine.charAt(i);
                sb.append(c=='*' ? ".*" : ""+c); 
            }
            // Convert to all uppercase since TP+ always outputs uppercase
            // even if you don't want it to
            String linePattern = sb.toString().toUpperCase();
            
            // and add the line pattern to the list we're interested in.
            mLinePatterns.add(Pattern.compile(linePattern));
        }
     }
            
     /**
      * Discover the field positions in the DBF file.  These change depending
      * on what version of TP+/Cube produced the output files.
      * 
      * @param dbf File to read
      * @return Hashtable of field names and their position in the file, or null if error 
      */
    void learnFieldPositions(DBFReader dbf) {
        Hashtable table;
        table = new Hashtable();
        DBFCOLUMNS = dbf.getFieldCount();
        try {
            for (int i=0; i < DBFCOLUMNS; i++) {
                JDBField f = dbf.getField(i);
                table.put(f.getName(),new Integer(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(8);
        }
        
        A =      getPosition(table, "A");
        B =      getPosition(table, "B");
        TIME =   getPosition(table, "TIME");
        MODE =   getPosition(table, "MODE");
        FREQ =   getPosition(table, "FREQ");
        PLOT =   getPosition(table, "PLOT");
        COLOR =  getPosition(table, "COLOR");
        STOP_A = getPosition(table, "STOP_A");
        STOP_B = getPosition(table, "STOP_B");
        DIST =   getPosition(table, "DIST");
        NAME =   getPosition(table, "NAME");
        SEQ =    getPosition(table, "SEQ");
        OWNER =  getPosition(table, "OWNER");
        VOL =    getPosition(table, "AB_VOL");
        BRDA =   getPosition(table, "AB_BRDA");
        XITA =   getPosition(table, "AB_XITA");
        BRDB =   getPosition(table, "AB_BRDB");
        XITB =   getPosition(table, "AB_XITB");
    }


    
    
    
    /** 
     * Find the DBF field we're looking for.
     * @param fields The table of fields and their positions
     * @param s Name of the field we're interested in
     * @return Field position in the array
     */
    int getPosition (Hashtable fields, String s) {
        int i = -1;
        Integer g = (Integer) fields.get(s);
        if (g != null)
            i = g.intValue();
     
        return i;
    }
    
    /**
     * MAIN LOOP! ---------------------------------------
     * Read the required DBF files and populate the data vectors.
     */
    void readDBFs() {
        int steps = vPaths.size() * (1+mSelectedTimePeriods.length())/3;
        for (int i=0; i<steps;i++) System.err.print("-"); System.err.println("");
        
        for (int period = 0; period < mTimePeriods.length; period++) {

          // Be sure this time period is part of the analysis
          if (-1 == mSelectedTimePeriods.indexOf(mTimePeriods[period]))
                continue;

          // Figure out file path
          File x = new File (mOutFile);
          String parentDir = x.getAbsoluteFile().getParent();
          
          // Loop on each DBF file 
          Enumeration pthEnum = vPaths.elements();
          while (pthEnum.hasMoreElements()) {
            String pth = (String) pthEnum.nextElement();

            String dbfFile = parentDir + File.separator + 
            	pth + mTimePeriods[period]+".dbf";
            DBFReader dbf = null;
            
            try {
                System.err.print("=");

                // Skip files that aren't there...
                if (! (new File(dbfFile).exists())) {
                    System.err.println(" File missing: "+dbfFile);
                    continue;
                }
                    
                // Otherwise open 'er on up.
                dbf = new DBFReader(dbfFile);
                
                // Find out where the relevant columns are
                learnFieldPositions(dbf);

                while (dbf.hasNextRecord()) {
                    Object[] fields = null;
                    fields = dbf.nextRecord();
                    if (fields.length < DBFCOLUMNS) {
                        System.err.println("\n\nERROR: "+dbfFile
                                + "\ndoes not have the correct number of columns."
                                + "\nThe transit assignment probably failed.\n");
                        System.exit(8);
                    }

                    long seq = ((Long)fields[SEQ]).longValue();
                    
                    // Sequence "0" records are zone-centroids and other nonsense.
                    // Let's see if it's a supplink that we care about, otherwise skip it.
                    if (0 == seq) {
                        String a = fields[A].toString();
                        String b = fields[B].toString();

                        if (null != mStationInterest.get(a)) {
                            TransitStop ts = (TransitStop) mStationInterest.get(a);
                            if (Integer.parseInt(b)<=1899) b = "0";
                            ts.addSuppNodes(b,period,fields[VOL],false);
                        } 

                        if (null != mStationInterest.get(b)) {
                            TransitStop ts = (TransitStop) mStationInterest.get(b);
                            if (Integer.parseInt(a)<=1899) a = "0";
                            ts.addSuppNodes(a,period,fields[VOL],true);
                        }
                         
                        // No other work needs to be done on a SEQ==0 record
                        continue;
                    }

                    // Check station interest. -------------------------------
                    // If first link in a sequence, check its ANODE.
                    if (1 == seq && null != mStationInterest.get(fields[A].toString())
                            && "1".equals(fields[STOP_A].toString())) {
                        TransitStop ts = (TransitStop) mStationInterest.get(fields[A].toString());
                        ts.addVol(fields[NAME].toString(),
                                period,fields[BRDA],
                                fields[XITA]);
                    }
                    
                    // And for all links, check their BNODE.
                    if (null != mStationInterest.get(fields[B].toString())
                            && "1".equals(fields[STOP_B].toString())) {
                        TransitStop ts = (TransitStop) mStationInterest.get(fields[B].toString());
                        ts.addVol(fields[NAME].toString(),period,
                                fields[BRDB],
                                fields[XITB]);
                    }
                    
                    // Populate link fields
                    String name = fields[NAME].toString();
                    TransitLink link = new TransitLink(fields,
                             A,  B, MODE, DIST, VOL, BRDA, BRDB, XITA, XITB, STOP_A, STOP_B, TIME, SEQ, FREQ); 

                    // See if we're interested in this line's details specifically
                    TransitLine line = (TransitLine) mLineInterest.get(name);
                    if (line == null && mLinesNotInterested.get(name) == null) {
                        for (int i=0; i<mLinePatterns.size(); i++) {
                            Pattern p = (Pattern) mLinePatterns.elementAt(i);
                            Matcher m = p.matcher(name);
                            if (m.matches()) {
                                line = new TransitLine(name);
                                mLineInterest.put(name, line);
                                break;
                            }
                        }
                        // Didn't match; let's save that result so we don't have to check each time
                        mLinesNotInterested.put(name,"");
                    }

                    // Accumulate summary stats -------------------------------
                    if (mPrepareSummary) {
                        TransitLine sumLine = (TransitLine) mSummaryInterest.get(name);
                        if (null == sumLine) {
                            if (null == line) line = new TransitLine(name);
                            mSummaryInterest.put(name,line);
                        } else
                            line = sumLine;
                    }

                    if (line == null) {
                        line = new TransitLine(name);
                    } 

                    line.addSummary(link,period);
                    line.addLink(link, period);

                    // why is this here?                    DBFCOLUMNS = 17;
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

        reportStationLevelResults();
        reportLineLevelResults();
        reportSummaryResults();
    }

    void reportStationLevelResults() {
		try {
            WritableSheet sheet = wb.createSheet("Stations",1);
            sheet.setColumnView(0, 17);
            sheet.setColumnView(1, 30);
            for (int col=2; col<=18; col++) {
                if (col % 3 == 1) { sheet.setColumnView(col, 2); }
                else              { sheet.setColumnView(col, 9); }
            }           
	        Iterator stations = mStationInterest.values().iterator();
	        while (stations.hasNext()) {
	            TransitStop trStop = (TransitStop) stations.next();
	            trStop.reportStations(sheet, mNodeLookup);
	        }
	    } catch (Exception e) {}
    }
    
    void reportSummaryResults() {

		TransitLine.lineCount = 0;

		try {
            WritableSheet sheet = wb.createSheet("LineStats",0);
            sheet.setColumnView(0, 17);
            sheet.setColumnView(7, 2);
            sheet.setColumnView(8, 2);
            sheet.setColumnView(14, 2);
            sheet.setColumnView(21, 2);
            sheet.setColumnView(28, 2);

            sheet.addCell(new Label(0,0,"Transit Line Summary", bold_font));

            sheet.addCell(new Label( 1,1,"Boardings",bold_font));
            sheet.addCell(new Label( 9,1,"Headways",bold_font));   // 9 instead of 8, since no daily headway exists.
            sheet.addCell(new Label(15,1,"Passenger-Miles",bold_font));
            sheet.addCell(new Label(22,1,"Passenger-Hours",bold_font));
            sheet.addCell(new Label(29,1,"Max Load Points",bold_font));
            
            sheet.addCell(new Label(0,2,"Line Name", bold_font));
            for (int i = 0; i<4; i++) {
                sheet.addCell(new Label(1+7*i,2,(i==1 ? " ":"Daily"), bold_font)); // No Daily headway label;-)              
                sheet.addCell(new Label(2+7*i,2,"AM", bold_font));
                sheet.addCell(new Label(3+7*i,2,"MD", bold_font));
                sheet.addCell(new Label(4+7*i,2,"PM", bold_font));
                sheet.addCell(new Label(5+7*i,2,"EV", bold_font));
                sheet.addCell(new Label(6+7*i,2,"EA", bold_font));
                for (int j=1; j<=6; j++) {
                    if (i==1 && j==1) { continue; }
                    if (i==1)   { sheet.setColumnView(j+7*i, 6); } // headways
                    else        { sheet.setColumnView(j+7*i, 9); } // others are bigger
                }
            }
            
			// Max Load Point labels
            sheet.addCell(new Label(29+0,2,"AM", bold_font));            
            sheet.addCell(new Label(29+3,2,"MD", bold_font));            
            sheet.addCell(new Label(29+6,2,"PM", bold_font));            
            sheet.addCell(new Label(29+9,2,"EV", bold_font));            
            sheet.addCell(new Label(29+12,2,"EA", bold_font));         
            for (int col=29; col<=29+13; col++) {
                if (col % 3 == 2)
                    { sheet.setColumnView(col,20); }
                else if (col %3 == 0)
                    { sheet.setColumnView(col, 6); }
                else
                    { sheet.setColumnView(col, 2); }
            }
            
            // Sort the lines
            Vector c = new Vector(mSummaryInterest.values());
            Collections.sort(c);
            Enumeration lines = c.elements();

            while (lines.hasMoreElements()) {
                TransitLine trLine = (TransitLine) lines.nextElement();
                trLine.reportSummary(sheet,mNodeLookup);
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
            sheet.setColumnView(0, 17);
            sheet.setColumnView(1, 6);
            sheet.setColumnView(2, 20);
            for (int col=3; col<=25; col++) {
                if (col % 4 == 2) {
                    sheet.setColumnView(col, 2);
                }
                else {
                    sheet.setColumnView(col, 9); 
                }
            }
            sheet.setColumnView(6, 2);
            sheet.setColumnView(10, 2);
            sheet.setColumnView(14, 2);
            sheet.setColumnView(18, 2);
            sheet.setColumnView(22, 2);

            sheet.addCell(new Label(0,0,"Line Detail Report", bold_font));

	        while (lines.hasMoreElements()) {
	            TransitLine trLine = (TransitLine) lines.nextElement();
	            trLine.reportStations(sheet, mNodeLookup);
	        }
	    } catch (Exception e) {}
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


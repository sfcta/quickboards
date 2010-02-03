/*
 * Created on Apr 15, 2004
 *
 * (c) 2010 San Francisco County Transportation Authority.
 * 
 */
package org.sfcta.quickboards;
import java.util.*;

import jxl.write.*;
import jxl.write.Number;

/**
 * @author billy
 *
 * (c) 2010 San Francisco County Transportation Authority.
 */
public class TransitLine implements Comparable {

    static int PERIODS = 5;
    static String[] label = {"AM","MD","PM","EV","EA"};
    static double[] timePeriodFactors = {1.0, 0.44, 0.18, 0.37, 0.22, 0.58}; 
    // Hmm -- but these don't match http://intranet/Modeling/HelpfulFactors
    // The following do:
    // static double[] timePeriodFactors = {1.0, 0.4176, 0.1848, 0.4044, 0.2076, 0.5556}; 
    static Hashtable mNodeLookup = null;
    public static int lineCount = 0;

    String name = "";
    Vector nameChunks = null;

    // These member variables represent Daily and then the five time periods
    int    mBoards[]    = {0,0,0,0,0,0};
    double mPassMiles[] = {0,0,0,0,0,0};
    double mPassHours[] = {0,0,0,0,0,0};
    long   mMaxLoadPoint[] = {0,0,0,0,0,0}; 
    double mMaxLoad[] =      {0,0,0,0,0,0};
    double mHeadway[] = {0,0,0,0,0,0};

    Hashtable[] links = new Hashtable[PERIODS];
    
    /**
     * @param name Name of line, same as the transit line name in TP+.
     */
    public TransitLine(String name) {
        this.name = name;
        nameChunks = QuickBoards.getNameChunks(name);
    }

    
    /**
     * Add data from a transit link into the data for this
     * transit line. Boards, exits, and volumes will be
     * aggregated if this a-b combination already exists.
     * 
     * @param link
     * @param period
     */
    void addLink(TransitLink link, int period) {
        // Create the hashtable if this is the first link in this period.
        if (null == links[period])
            links[period] = new Hashtable();

        // Check to see if this link already exists in this time period
        // This will happen if a previously-read DBF file contains this link.
        TransitLink tlink = (TransitLink) links[period].get(link.seq);
        if (null == tlink) {
            // It's a new link; add it to the hashtable.
            links[period].put(link.seq, link);
            tlink = link;
            this.mHeadway[period+1] = tlink.freq;
        } else {
            // Link already exists, aggregate results.
            tlink.brda += link.brda;
            tlink.brdb += link.brdb;
            tlink.xita += link.xita;
            tlink.xitb += link.xitb;
            tlink.vol += link.vol;

            // Be sure to flag stopnodes if they weren't stops previously 
            if (tlink.stopa==0) tlink.stopa = link.stopa;
            if (tlink.stopb==0) tlink.stopb = link.stopb;

        }

        // Replace max load point if necessary
        if (tlink.stopa > 0  &&  tlink.vol > mMaxLoad[period+1]) {
            mMaxLoad[period+1] = tlink.vol;
            mMaxLoadPoint[period+1] = Long.parseLong(tlink.a);
        }
    }

    /**
     * Accumulate summary info for this link
     * @param link
     * @param period
     */
    void addSummary(TransitLink link, int period) {

        // incr period since daily is at position 0.
        period++;

        if (link.seq.intValue()==1 && link.stopa==1) {
            mBoards[period] += link.brda;
            mBoards[0] += link.brda;
        }
        
        if (link.stopb==1) {
            mBoards[period] += link.brdb;
        	mBoards[0] += link.brdb;
        }
        
        mPassMiles[period] += 0.01 * (link.vol * link.dist);
        mPassHours[period] += 0.01 / 60.0 * (link.vol * link.time);

        mPassMiles[0] += 0.01 * (link.vol * link.dist);
        mPassHours[0] += 0.01 / 60.0 * (link.vol * link.time);

    }

    void reportSummary(WritableSheet sheet, Hashtable lookup) {
        try {
            lineCount++;
            
            sheet.addCell(new Label(0,2+lineCount,name));
            
            for (int i = 0; i<=PERIODS; i++) {
                if (mBoards[i]>0)
                    sheet.addCell(new Number(1+i,2+lineCount,mBoards[i]));
                if (mHeadway[i] >0 )
                    sheet.addCell(new Number(8+i,2+lineCount,(float)(mHeadway[i])));
                if (mPassMiles[i]>1)
                    sheet.addCell(new Number(15+i,2+lineCount,(int)(0.5+mPassMiles[i])));
                if (mPassHours[i]>1)
                    sheet.addCell(new Number(22+i,2+lineCount,(int)(0.5+mPassHours[i])));
                
                // Max Load Point summary (skip daily):                    
                if (i>0) {
                    if (mMaxLoad[i]>0) {
                        String textName = (String) lookup.get(Long.toString(mMaxLoadPoint[i]));
                        sheet.addCell(new Label (26+i*3,2+lineCount,textName));                        
                        sheet.addCell(new Number(27+i*3,2+lineCount,(int)(0.5+mMaxLoad[i]*timePeriodFactors[i])));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /** 
     * Helper function to sort transit lines. Transit line names 
     * are divided into text and numerical portions so they sort 
     * in a more natural order.
     *  
     * @return -1,0, or 1 depending on sort order.
     */
    public int compareTo(Object o) {
        Vector compChunks = ((TransitLine)o).nameChunks;
        int c = 0;

        //Compare each chunk individually.
        for (int i = 0; i< nameChunks.size(); i++) {

            // Check if compared line has fewer name chunks.
            if (i >= compChunks.size())
                return 1;

            String mine = ((String)nameChunks.elementAt(i));
            String theirs = ((String)compChunks.elementAt(i)); 

            int myvalue = -1;
            int theirvalue = -1;

            try {
                myvalue = Integer.parseInt(mine);
            } catch (NumberFormatException nfe) {}
            try {
                theirvalue = Integer.parseInt(theirs);
            } catch (NumberFormatException nfe) {}

            // Compare numbers
            if (myvalue >0 && theirvalue > 0) {
                if (myvalue == theirvalue) 
                    continue;
                return (myvalue > theirvalue ? 1 : -1);
            }

            // Compare text
            c = mine.compareTo(theirs);
            if (c!=0)
                return c;
            
            // Go back and check the next chunk since the compare 
            // says these chunks are equivalent.
        }

        // Loop ends if this line has fewer name chunks.
        if (c==0) {
            c = -1;
        }

        return c; 
    }
    
    /**
     * Summarize and report the boardings, alightings, and volumes of this
     * transit line, by station.
     * 
     * @return Formatted text string of transit line boardings by station. 
     */
    void reportStations(WritableSheet sheet, Hashtable lookup) {
        StationList list = new StationList();
        mNodeLookup = lookup;
        
        // Loop for each time period

        for (int pd = 0 ; pd < PERIODS; pd++) {

            // Don't report a time period if the line doesn't run during that period.
            if (null == links[pd])
                continue;

            // Loop for each link, in order, along the transit line
            TransitLink link = null;
            int seq = 0;
            
            for (seq = 1; seq <= links[pd].size(); seq++) {

                link = (TransitLink) links[pd].get(new Long(seq));

                if (link == null) {
                    System.err.println("ERROR: Network Problem with: "+name+" "+label[pd]+" seq: "+seq);
                    continue;
                }

                if (link.stopa == 1) {
                    list.addStation(link.a, pd, link.brda, link.xita, link.vol);
                }
            }

            // Now do final stop, which uses BNODE instead of ANODE.
            link = (TransitLink) links[pd].get(new Long(seq-1));

            if (link == null) {
                System.err.println("ERROR: Network Problem with: "+name+" "+label[pd]+" seq: "+seq);
            } else {
                list.addStation(link.b, pd, link.brdb, link.xitb, 0);
            }

        }

        try {
            WritableCellFormat font =  new WritableCellFormat (new WritableFont(
            		WritableFont.ARIAL, 10, WritableFont.BOLD, false));
            
            lineCount+=2;
            
            sheet.addCell(new Label( 0,lineCount,name,font));
            sheet.addCell(new Label( 3,lineCount,"Daily", font));            
            sheet.addCell(new Label( 7,lineCount,"AM", font));            
            sheet.addCell(new Label(11,lineCount,"MD", font));            
            sheet.addCell(new Label(15,lineCount,"PM", font));            
            sheet.addCell(new Label(19,lineCount,"EV", font));            
            sheet.addCell(new Label(23,lineCount,"EA", font));         

            sheet.addCell(new Label(0,lineCount+1,name, font));
            sheet.addCell(new Label(1,lineCount+1,"Node", font));         
            sheet.addCell(new Label(2,lineCount+1,"Station", font));         
            for (int i = 0; i<6; i++) {
            	sheet.addCell(new Label(i*4+3,lineCount+1,"Boards", font));			
            	sheet.addCell(new Label(i*4+4,lineCount+1,"Exits", font));			
            	sheet.addCell(new Label(i*4+5,lineCount+1,"Volume", font));			
            }         
            lineCount+=2;

            // And now print it all out
            list.write(sheet,name);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    class StationList {
        Hashtable staLookup = new Hashtable();
        LinkedList listOfStations = new LinkedList();

        void addStation(String node, int period, long brd, long xit, long vol) {

            StationData sd = (StationData) staLookup.get(node);
            if (null == sd) {
                sd = new StationData(node);
                staLookup.put(node, sd);
                listOfStations.add(sd);
            }

            sd.addVolumes(period, brd, xit, vol);
        }

        void write(WritableSheet sheet,String linename) {
            Iterator i = listOfStations.iterator();
            while (i.hasNext()) {
                try {
                    sheet.addCell(new Label(0,lineCount,linename));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ((StationData)i.next()).write(sheet);
                lineCount++;
            }
        }
    }

    class StationData {
        String name;
        long[] j = {0,0,0, 0,0,0, 0,0,0, 0,0,0, 0,0,0, 0,0,0};

        public StationData(String s) {
            name = s;
        }

        void addVolumes (int period, long brd, long xit, long vol) {
            j[0] += brd;
            j[1] += xit;
            j[2] += vol;
            
            j[3+period*3] += brd;
            j[4+period*3] += xit;
            j[5+period*3] += vol;
        }
        void write(WritableSheet sheet) {
            try {

                // Try to retrieve node name; just use node number if it 
                // doesn't exist in database.
                String textName = (String) mNodeLookup.get(name);
                if (null == textName)
                    textName = name;
                

                sheet.addCell(new Label(1,lineCount,name));
                sheet.addCell(new Label(2,lineCount,textName));
                for (int i = 0; i < 18; i++) {
//                    if (j[i]!=0)
                        sheet.addCell(new Number((i/3)+i+3,lineCount,j[i]));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

/*
 * Created on Apr 15, 2004
 *
 * (c) 2004 San Francisco County Transportation Authority.
 * 
 */
package org.sfcta.fastpass;
import java.util.*;
import jxl.write.*;
import jxl.write.Number;

/**
 * @author billy
 *
 * (c) 2004 San Francisco County Transportation Authority.
 */
public class TransitLine implements Comparable {

    static int PERIODS = 5;
    public static int lineCount = 0;
    String name = "";
    Vector nameChunks = null;
    
    Hashtable[] links = new Hashtable[PERIODS];
    static String[] label = {"AM","MD","PM","EV","EA"};
           
    /**
     * @param name Name of line, same as the transit line name in TP+.
     */
    public TransitLine(String name) {
        this.name = name;
        nameChunks = FastPass.getNameChunks(name);
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
        } else {
            // Link already exists, aggregate results.
            tlink.brda += link.brda;
            tlink.brdb += link.brdb;
            tlink.xita += link.xita;
            tlink.xitb += link.xitb;
            tlink.vol += link.vol;

            // Be sure to flag stopnodes if they weren't stops previously 
            if (tlink.stopa==0)
                tlink.stopa = link.stopa;
            if (tlink.stopb==0)
                tlink.stopb = link.stopb;
        }
        
    }

    int mBoards[] = {0,0,0,0,0,0};
    double mPassMiles[] = {0,0,0,0,0,0};
    double mPassHours[] = {0,0,0,0,0,0};
    
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
        
        mPassMiles[period] += 0.1 * (link.vol * link.dist);
        mPassHours[period] += 0.1 / 60.0 * (link.vol * link.time);

        mPassMiles[0] += 0.1 * (link.vol * link.dist);
        mPassHours[0] += 0.1 / 60.0 * (link.vol * link.time);

    }

    void reportSummary(WritableSheet sheet) {
        try {
            lineCount++;
            sheet.addCell(new Label(0,2+lineCount,name));
            
            for (int i = 0; i<=PERIODS; i++) {
                if (mBoards[i]>0)
                    sheet.addCell(new Number(i*4+1,2+lineCount,mBoards[i]));
                if (mPassMiles[i]>1)
                    sheet.addCell(new Number(i*4+2,2+lineCount,(int)(0.5+mPassMiles[i])));
                if (mPassHours[i]>1)
                    sheet.addCell(new Number(i*4+3,2+lineCount,(int)(0.5+mPassHours[i])));
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
    void reportStations(WritableSheet sheet) {
        StationList list = new StationList();
        
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
            
            sheet.addCell(new Label( 0,lineCount,"Line: "+name,font));
            sheet.addCell(new Label( 1,lineCount,"Daily", font));            
            sheet.addCell(new Label( 5,lineCount,"AM", font));            
            sheet.addCell(new Label( 9,lineCount,"MD", font));            
            sheet.addCell(new Label(13,lineCount,"PM", font));            
            sheet.addCell(new Label(17,lineCount,"EV", font));            
            sheet.addCell(new Label(21,lineCount,"EA", font));         

            sheet.addCell(new Label(0,lineCount+1,"Station", font));         
            for (int i = 0; i<6; i++) {
            	sheet.addCell(new Label(i*4+1,lineCount+1,"Boards", font));			
            	sheet.addCell(new Label(i*4+2,lineCount+1,"Exits", font));			
            	sheet.addCell(new Label(i*4+3,lineCount+1,"Volume", font));			
            }         
            lineCount+=2;

            // And now print it all out
            list.write(sheet);
            
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

        void write(WritableSheet sheet) {
            Iterator i = listOfStations.iterator();
            while (i.hasNext()) {
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
                sheet.addCell(new Label(0,lineCount,name));
                for (int i = 0; i < 18; i++) {
                    if (j[i]!=0)
                        sheet.addCell(new Number((i/3)+i+1,lineCount,j[i]));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    String lalign(String text, int width) {
        StringBuffer sb = new StringBuffer();
        int padding = width - text.length();
        sb.append(text);
        while (--padding >= 0) sb.append(" ");
        return (sb.toString());
    }
}

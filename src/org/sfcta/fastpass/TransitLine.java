/*
 * Created on Apr 15, 2004
 *
 * (c) 2004 San Francisco County Transportation Authority.
 * 
 */
package org.sfcta.fastpass;
import java.util.*;


/**
 * @author billy
 *
 * (c) 2004 San Francisco County Transportation Authority.
 */
public class TransitLine {

    static int PERIODS = 5;
    String name = "";
    Hashtable[] links = new Hashtable[PERIODS];
    static String[] label = {"AM","MD","PM","EV","EA"};
           
    /**
     * @param name Name of line, same as the transit line name in TP+.
     */
    public TransitLine(String name) {
        this.name = name;
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


    /**
     * Summarize and report the boardings, alightings, and volumes of this
     * transit line, by station.
     * 
     * @return Formatted text string of transit line boardings by station. 
     */
    StringBuffer reportStations() {
        StringBuffer sb = new StringBuffer();
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
        // And now print it all out
        sb.append("\r\nLine: "+name+"   Period: \r\n");
        sb.append("        DAILY----------------   AM-------------------   MD-------------------   PM-------------------   EV-------------------   EA-------------------\r\n");
        sb.append(" STA    BOARD   EXITS     VOL   BOARD   EXITS     VOL   BOARD   EXITS     VOL   BOARD   EXITS     VOL   BOARD   EXITS     VOL   BOARD   EXITS     VOL\r\n");
        sb.append(list.toString());
        
        return sb;
    }

    String ralign(int value, int width) {
	    if (value==0) {
	        return (ralign(" ",width));
	    }
        return ralign(Integer.toString(value),width);
    }

    String ralign(long value, int width) {
	    if (value==0) {
	        return (ralign(" ",width));
	    }
        return ralign(Long.toString(value),width);
    }

    String ralign(String text, int width) {
        StringBuffer sb = new StringBuffer();
        int padding = width - text.length();
        while (--padding >= 0) sb.append(" ");
        sb.append(text);
        return (sb.toString());
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

        public String toString() {
            StringBuffer sb = new StringBuffer();
            Iterator i = listOfStations.iterator();
            while (i.hasNext()) {
                sb.append(""+i.next());
            }
            return sb.toString();
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
        public String toString() {
            StringBuffer sb = new StringBuffer(ralign(name,5));

            for (int i = 0; i < 18; i++) {
                sb.append(ralign(j[i],8));
            }

            sb.append("\r\n");
            return sb.toString();
        }
    }
}

/*
 * Created on Apr 19, 2004
 *
 * (c) 2004 San Francisco County Transportation Authority.
 * 
 */
package org.sfcta.fastpass;

import java.util.*;

/**
 * @author tad
 *
 * (c) 2004 San Francisco County Transportation Authority.
 */
public class TransitStop {
    static int PERIODS = 5;
    static String TOTAL = "TOTAL";
    static String[] label = {"AM","MD","PM","EV","EA"};
    Hashtable lines = new Hashtable();
    String node;
    LineStop mTotalRiders;
    
    /**
     * Keeps data for a transit stop. 
     */
    public TransitStop(String node) {
        this.node = node;
        mTotalRiders = new LineStop(TOTAL);
        lines.put(TOTAL,mTotalRiders);
    }

    public void addVol(String line, int period, Object brds, Object xits) {
        LineStop ls = (LineStop)lines.get(line);

        if (null == ls) {
            ls = new LineStop(line);
            lines.put(line,ls);
        }

        // Keep running total of boards/alights in 0 and 1
        ls.riders[0]   += Long.parseLong(brds.toString());
        ls.riders[1] += Long.parseLong(xits.toString());

        ls.riders[2+2*period]   += Long.parseLong(brds.toString());
        ls.riders[3+2*period] += Long.parseLong(xits.toString());

        // And keep grand total too
        if (!line.equals(TOTAL))
            addVol(TOTAL, period,brds,xits);
    }

    public StringBuffer reportStations() {
        StringBuffer sb = new StringBuffer();
        
        sb.append("Sta "+node+":       DAILY        AM          MD          PM          EV          EA\n");
        sb.append("               BRD  EXIT   BRD  EXIT   BRD  EXIT   BRD  EXIT   BRD  EXIT   BRD  EXIT\n");

        // Show the total first
        sb.append(punchRiders((LineStop)lines.get(TOTAL)));
        sb.append("------------ ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- -----\n");
        Iterator it = lines.values().iterator();
        while (it.hasNext()) {
            LineStop line = (LineStop) it.next();
            if (line.name == TOTAL)
                continue;
            sb.append(punchRiders(line));
        }

        sb.append("------------ ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- -----\n");
        return sb;
    }

    StringBuffer punchRiders(LineStop line) {
        StringBuffer sb = new StringBuffer();

        sb.append(lalign(line.name,12));
        for (int i= 0; i<= PERIODS; i++) {
            sb.append(ralign(line.riders[i*2],6));
            sb.append(ralign(line.riders[1+i*2],6));
        }
        sb.append("\n");
        return sb;
    }

    
	class LineStop {
	    public String name;
	    public long riders[] = {0,0,0,0,0,0,0,0,0,0,0,0};
	
	    public LineStop(String name) {
	        this.name = name;
	    }
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

    String lalign(String text, int width) {
        StringBuffer sb = new StringBuffer();
        int padding = width - text.length();
        sb.append(text);
        while (--padding >= 0) sb.append(" ");
        return (sb.toString());
    }

}


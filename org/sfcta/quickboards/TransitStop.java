/*
 * Created on Apr 19, 2004
 *
 * (c) 2004 San Francisco County Transportation Authority.
 * 
 */
package org.sfcta.quickboards;

import java.util.*;

import jxl.write.*;
import jxl.write.Number;

/**
 * @author tad
 *
 * (c) 2004 San Francisco County Transportation Authority.
 */
public class TransitStop {
    static int PERIODS = 5;
    static int lineCount = 0;
    static String TOTAL = "TOTAL";
    static String[] label = {"AM","MD","PM","EV","EA"};
    Hashtable lines = new Hashtable();
    Hashtable mSuppNodes = new Hashtable();
    
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

    public void addSuppNodes(String node, String vol, boolean inbound) {
        // Get the vector of inbound/outbound supplink flows  
        int[] flows = (int[]) mSuppNodes.get(node);
        if (null == flows) {
            flows = new int[2];
            flows[0] = 0;
            flows[1] = 0;
            mSuppNodes.put(node,flows);
        }

        try {
            if (inbound) 
                flows[0] += Integer.parseInt(vol);
            else
                flows[1] += Integer.parseInt(vol);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public void reportStations(WritableSheet sheet, Hashtable lookup) {
		try {
            WritableCellFormat font =  new WritableCellFormat (new WritableFont(
            		WritableFont.ARIAL, 10, WritableFont.BOLD, false));
           	sheet.addCell(new Label(0,0,"Station Report", font));
            
            lineCount+=2;

            // Try to retrieve node name; just use node number if it 
            // doesn't exist in database.
            String textName = (String) lookup.get(node);
            if (null == textName)
                textName = "Station "+node;

            
            sheet.addCell(new Label( 0,lineCount,textName,font));
            sheet.addCell(new Label( 0,lineCount+1,""+node,font));
			sheet.addCell(new Label( 1,lineCount,"Daily", font));            
			sheet.addCell(new Label( 4,lineCount,"AM", font));            
			sheet.addCell(new Label( 7,lineCount,"MD", font));            
			sheet.addCell(new Label(10,lineCount,"PM", font));            
			sheet.addCell(new Label(13,lineCount,"EV", font));            
			sheet.addCell(new Label(16,lineCount,"EA", font));         
			for (int i = 0; i<6; i++) {
				sheet.addCell(new Label(i*3+1,lineCount+1,"Boards", font));			
				sheet.addCell(new Label(i*3+2,lineCount+1,"Exits", font));			
			}         

			lineCount+=2;
			
            // Sort the lines
            Vector c = new Vector(lines.values());
            Collections.sort(c);
            Enumeration enumm = c.elements();

            // Print the lines out
            while (enumm.hasMoreElements()) {
                LineStop line = (LineStop) enumm.nextElement();
                if (line.name == TOTAL)
                    continue;
                punchRiders(line,sheet,false);
            }

            // Show the total last
            punchRiders((LineStop)lines.get(TOTAL),sheet,true);
            
            // Now print access/egress links
            lineCount++;
            enumm = mSuppNodes.keys();
            while (enumm.hasMoreElements()) {
                String node = (String)enumm.nextElement();
                punchSuppLinks(sheet, lookup, node, (int[]) mSuppNodes.get(node));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void punchSuppLinks(WritableSheet sheet, Hashtable lookup, String node, int[] flows) {
        // need to add time-period stuff. let's just get daily, for now.
        try {
            // search for node name
            String textName = (String) lookup.get(node);
            if (null == textName)
                textName = node;
            if (textName.equals("0")) textName="zones";
                
            sheet.addCell(new Label(0,lineCount,"From "+textName));
            sheet.addCell(new Number(1,lineCount,flows[0]));
            lineCount++;
            sheet.addCell(new Label(0,lineCount,"To "+textName));
            sheet.addCell(new Number(1,lineCount,flows[1]));
            lineCount++;
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
    void punchRiders(LineStop line, WritableSheet sheet, boolean useBold) {

        try {
            WritableCellFormat wcf = null;

            if (useBold)
                wcf =  new WritableCellFormat (new WritableFont(
            		WritableFont.ARIAL, 10, WritableFont.BOLD, false));
            else
                wcf =  new WritableCellFormat (new WritableFont(
                		WritableFont.ARIAL, 10, WritableFont.NO_BOLD, false));
                
            sheet.addCell(new Label(0,lineCount,line.name,wcf));

            for (int i= 0; i<= PERIODS; i++) {
                if (line.riders[i*2]>0)
                    sheet.addCell(new Number(i*3+1,lineCount,line.riders[i*2],wcf));
                if (line.riders[i*2+1]>0)
                    sheet.addCell(new Number(i*3+2,lineCount,line.riders[i*2+1],wcf));
            }
            lineCount++;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
	class LineStop implements Comparable {
	    Vector nameChunks = null;
	    public String name;
	    public long riders[] = {0,0,0,0,0,0,0,0,0,0,0,0}; // Two for each time period and two for daily.
	
	    public LineStop(String name) {
	        this.name = name;
	        nameChunks = QuickBoards.getNameChunks(name);
	    }
	    
	    /** 
	     * Helper function to sort transit lines. Transit line names 
	     * are divided into text and numerical portions so they sort 
	     * in a more natural order.
	     *  
	     * @return -1,0, or 1 depending on sort order.
	     */
	    public int compareTo(Object o) {
	        Vector compChunks = ((LineStop)o).nameChunks;
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
	}
}


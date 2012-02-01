/*
 * Created on Apr 19, 2004
 *
 * (c) 2004 San Francisco County Transportation Authority.
 * 
 */
package org.sfcta.quickboards;

import java.util.*;

import jxl.write.*;

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
    static WritableCellFormat nonbold_num_font =  new WritableCellFormat (
    		new WritableFont(WritableFont.ARIAL, 10, WritableFont.NO_BOLD, false),
    		new NumberFormat("#,##0"));
    
    
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

    public void addSuppNodes(String node, int period, Object vol, boolean inbound) {
        // Get the vector of inbound/outbound supplink flows  
        double[][] flows = (double[][]) mSuppNodes.get(node);
        if (null == flows) {
            flows = new double[2][PERIODS];
            for (int i = 0; i<PERIODS; i++) {
                flows[0][i] = 0;
                flows[1][i] = 0;
            }
            mSuppNodes.put(node,flows);
        }

        try {
            if (inbound) 
                flows[0][period] += (double) ((Long)vol).longValue();
            else
                flows[1][period] += (double) ((Long)vol).longValue();
        } catch (ClassCastException e) {
            if (inbound) 
                flows[0][period] += ((Double)vol).doubleValue();
            else
                flows[1][period] += ((Double)vol).doubleValue();
        }
    }
    
    public void addVol(String line, int period, Object brds, Object xits) {
        LineStop ls = (LineStop)lines.get(line);

        if (null == ls) {
            ls = new LineStop(line);
            lines.put(line,ls);
        }

        // Keep running total of boards/alights in 0 and 1
        try {
            ls.riders[0] += (double) ((Long)brds).longValue();
            ls.riders[1] += (double) ((Long)xits).longValue();
            ls.riders[2+2*period] += (double) ((Long)brds).longValue();
            ls.riders[3+2*period] += (double) ((Long)xits).longValue();
        } catch (ClassCastException e) {
            ls.riders[0] += ((Double)brds).doubleValue();
            ls.riders[1] += ((Double)xits).doubleValue();
            ls.riders[2+2*period] += ((Double)brds).doubleValue();
            ls.riders[3+2*period] += ((Double)xits).doubleValue();
        }

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

            
            sheet.addCell(new Label( 1,lineCount+1,""+node,font));
			sheet.addCell(new Label( 2,lineCount,"Daily", font));            
			sheet.addCell(new Label( 5,lineCount,"AM", font));            
			sheet.addCell(new Label( 8,lineCount,"MD", font));            
			sheet.addCell(new Label(11,lineCount,"PM", font));            
			sheet.addCell(new Label(14,lineCount,"EV", font));            
			sheet.addCell(new Label(17,lineCount,"EA", font));         
			for (int i = 0; i<PERIODS+1; i++) {
				sheet.addCell(new Label(i*3+2,lineCount+1,"Boards", font));			
				sheet.addCell(new Label(i*3+3,lineCount+1,"Exits", font));			
			}         

			lineCount+=2;
			
            // Sort the lines
            Vector c = new Vector(lines.values());
            Collections.sort(c);
            Enumeration enumm = c.elements();

            // Print the lines out
            while (enumm.hasMoreElements()) {
            	sheet.addCell(new Label( 0,lineCount,textName,font));
                LineStop line = (LineStop) enumm.nextElement();
                if (line.name == TOTAL)
                    continue;
                punchRiders(line,sheet,false);
            }

            // Show the total last
            sheet.addCell(new Label( 0,lineCount,textName,font));
            punchRiders((LineStop)lines.get(TOTAL),sheet,true);
            lineCount++;
            
            // Now print access/egress links
			for (int i = 0; i<PERIODS+1; i++) {
				sheet.addCell(new Label(i*3+2,lineCount,"From", font));			
				sheet.addCell(new Label(i*3+3,lineCount,"To", font));			
			}         
            lineCount++;
            enumm = mSuppNodes.keys();
            while (enumm.hasMoreElements()) {
            	sheet.addCell(new Label( 0,lineCount,textName,font));
                String node = (String)enumm.nextElement();
                punchSuppLinks(sheet, lookup, node, (double[][]) mSuppNodes.get(node));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void punchSuppLinks(WritableSheet sheet, Hashtable lookup, String node, double[][] flows) {
        // need to add time-period stuff. let's just get daily, for now.
        try {
            // search for node name
            String textName = (String) lookup.get(node);
            if (null == textName)
                textName = node;
            if (textName.equals("0")) textName="zones";

            double inbound_total=0;
            double outbound_total=0;
            sheet.addCell(new Label(1,lineCount,textName+" access"));
            for (int i = 0; i<PERIODS; i++) {
                sheet.addCell(new jxl.write.Number(3*i+5,lineCount,flows[0][i],nonbold_num_font));
                sheet.addCell(new jxl.write.Number(3*i+6,lineCount,flows[1][i],nonbold_num_font));
                inbound_total += flows[0][i];
                outbound_total += flows[1][i];
            }
            sheet.addCell(new jxl.write.Number(2,lineCount,inbound_total,nonbold_num_font));
            sheet.addCell(new jxl.write.Number(3,lineCount,outbound_total,nonbold_num_font));
            lineCount++;
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
    void punchRiders(LineStop line, WritableSheet sheet, boolean useBold) {

        try {
            WritableCellFormat wcf = null;

            if (useBold)
                wcf =  new WritableCellFormat (
                        new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD, false),
                        new NumberFormat("#,##0"));
            else
                wcf = nonbold_num_font;
                
            sheet.addCell(new Label(1,lineCount,line.name,wcf));

            for (int i= 0; i<= PERIODS; i++) {
                if (line.riders[i*2]>0)
                    sheet.addCell(new jxl.write.Number(i*3+2,lineCount,line.riders[i*2],wcf));
                if (line.riders[i*2+1]>0)
                    sheet.addCell(new jxl.write.Number(i*3+3,lineCount,line.riders[i*2+1],wcf));
            }
            lineCount++;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
	class LineStop implements Comparable {
	    Vector nameChunks = null;
	    public String name;
	    public double riders[] = {0,0,0,0,0,0,0,0,0,0,0,0}; // Two for each time period and two for daily.
	
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


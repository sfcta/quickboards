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

/**
 * @author tad
 *
 * (c) 2004 San Francisco County Transportation Authority.
 */
public class TransitLink {

    long mode = 0;
    String a = "";
    String b = "";
    long dist = 0;
    long time =0;
    long stopa = 0;
    long stopb = 0;
    double vol=0.0;
    double brda=0;
    double brdb=0;
    double xita=0;
    double xitb=0;
    double freq=0.0;
    
    Long seq;
    
    /**
     * 
     */
    public TransitLink(Object[] fields, int A, int B,int MODE,int DIST,int VOL,int BRDA,int BRDB,int XITA,int XITB,int STOP_A,int STOP_B,int TIME,int SEQ, int FREQ) {
        this.a = fields[A].toString();
        this.b = fields[B].toString();

        this.mode = ((Long)fields[MODE]).longValue();
        this.dist = ((Long)fields[DIST]).longValue();
        try {
            this.vol =  (double) ((Long)fields[VOL]).longValue();
            this.brda = (double) ((Long)fields[BRDA]).longValue();
            this.brdb = (double) ((Long)fields[BRDB]).longValue();
            this.xita = (double) ((Long)fields[XITA]).longValue();
            this.xitb = (double) ((Long)fields[XITB]).longValue();            
        } catch (ClassCastException e) {
            this.vol =  ((Double)fields[VOL]).doubleValue();
            this.brda = ((Double)fields[BRDA]).doubleValue();
            this.brdb = ((Double)fields[BRDB]).doubleValue();
            this.xita = ((Double)fields[XITA]).doubleValue();
            this.xitb = ((Double)fields[XITB]).doubleValue(); 
        }

        this.stopa= ((Long)fields[STOP_A]).longValue();
        this.stopb= ((Long)fields[STOP_B]).longValue();
        this.time = ((Long)fields[TIME]).longValue();
        this.freq = ((Double)fields[FREQ]).doubleValue();

        this.seq = ((Long)fields[SEQ]);
    }
}

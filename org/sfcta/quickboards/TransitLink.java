/*
 * Created on Apr 15, 2004
 *
 * (c) 2004 San Francisco County Transportation Authority.
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
    long vol=0;
    long brda=0;
    long brdb=0;
    long xita=0;
    long xitb=0;
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
        this.vol  = ((Long)fields[VOL]).longValue();
        this.brda = ((Long)fields[BRDA]).longValue();
        this.brdb = ((Long)fields[BRDB]).longValue();
        this.xita = ((Long)fields[XITA]).longValue();
        this.xitb = ((Long)fields[XITB]).longValue();
        this.stopa= ((Long)fields[STOP_A]).longValue();
        this.stopb= ((Long)fields[STOP_B]).longValue();
        this.time = ((Long)fields[TIME]).longValue();
        this.freq = ((Double)fields[FREQ]).doubleValue();

        this.seq = ((Long)fields[SEQ]);
    }
}

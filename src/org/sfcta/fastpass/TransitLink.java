/*
 * Created on Apr 15, 2004
 *
 * (c) 2004 San Francisco County Transportation Authority.
 * 
 */
package org.sfcta.fastpass;

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
    Long seq;
    
    /**
     * 
     */
    public TransitLink(Object[] fields) {
        this.a = fields[FastPass.A].toString();
        this.b = fields[FastPass.B].toString();

        this.mode = ((Long)fields[FastPass.MODE]).longValue();
        this.dist = ((Long)fields[FastPass.DIST]).longValue();
        this.vol  = ((Long)fields[FastPass.VOL]).longValue();
        this.brda = ((Long)fields[FastPass.BRDA]).longValue();
        this.brdb = ((Long)fields[FastPass.BRDB]).longValue();
        this.xita = ((Long)fields[FastPass.XITA]).longValue();
        this.xitb = ((Long)fields[FastPass.XITB]).longValue();
        this.stopa= ((Long)fields[FastPass.STOP_A]).longValue();
        this.stopb= ((Long)fields[FastPass.STOP_B]).longValue();
        this.time = ((Long)fields[FastPass.TIME]).longValue();

        this.seq = ((Long)fields[FastPass.SEQ]);
    }
}

package skayrux.shootout2d;

import java.net.InetAddress;

public class datagame_serverobject {
    public InetAddress srvip;
    public int srvport;
    public int srvnmap;
    public int srvgtype;
    public int srvplcount;
    public int srvbotscount;
    public int srvversion;
    public int srvping;
    public String srvverstr;
    public String srvname;
    public boolean srvpass;
    public int srvstatus;
    public String mapname;
    public String mapuid;
    public boolean custommap;

    public datagame_serverobject(InetAddress srvip, int srvport, int srvnmap, int srvgtype, int srvplcount, int srvbotscount, int srvversion, int srvping, String srvverstr, String srvname, boolean srvpass, int srvstatus) {
        this.srvip = srvip;
        this.srvport = srvport;
        this.srvnmap = srvnmap;
        this.srvgtype = srvgtype;
        this.srvplcount = srvplcount;
        this.srvbotscount = srvbotscount;
        this.srvversion = srvversion;
        this.srvping = srvping;
        this.srvverstr = srvverstr;
        this.srvname = srvname;
        this.srvpass = srvpass;
        this.srvstatus = srvstatus;
        this.custommap = false;
    }

    public datagame_serverobject(InetAddress srvip, int srvport, int srvnmap, int srvgtype, int srvplcount, int srvbotscount, int srvversion, int srvping, String srvverstr, String srvname, String mapname, boolean srvpass, int srvstatus) {
        this(srvip, srvport, srvnmap, srvgtype, srvplcount, srvbotscount, srvversion, srvping, srvverstr, srvname, srvpass, srvstatus);
        this.mapname = mapname;
    }

    public datagame_serverobject(InetAddress srvip, int srvport, String mapname, String mapuid, int srvgtype, int srvplcount, int srvbotscount, int srvversion, int srvping, String srvverstr, String srvname, boolean srvpass, int srvstatus) {
        this.srvip = srvip;
        this.srvport = srvport;
        this.srvgtype = srvgtype;
        this.srvplcount = srvplcount;
        this.srvbotscount = srvbotscount;
        this.srvversion = srvversion;
        this.srvping = srvping;
        this.srvverstr = srvverstr;
        this.srvname = srvname;
        this.srvpass = srvpass;
        this.srvstatus = srvstatus;
        this.mapname = mapname;
        this.mapuid = mapuid;
        this.custommap = true;
    }
}

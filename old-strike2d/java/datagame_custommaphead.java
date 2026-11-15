package skayrux.shootout2d;

public class datagame_custommaphead {
    public byte gver;
    public String mapname;
    public String mapuid;
    public int usersrvid;
    public int userrid;
    public short mapw;
    public short maph;
    public String timestamp;
    public int maxmapk;
    public short mapminx;
    public short mapminy;
    public short mapmaxx;
    public short mapmaxy;

    public datagame_custommaphead(byte gver, String mapname, String mapuid, int usersrvid, int userrid, short mapw, short maph, String timestamp, int maxmapk, short mapminx, short mapminy, short mapmaxx, short mapmaxy){
        this.gver=gver;
        this.mapname=mapname;
        this.mapuid=mapuid;
        this.usersrvid=usersrvid;
        this.userrid=userrid;
        this.mapw=mapw;
        this.maph=maph;
        this.timestamp=timestamp;
        this.maxmapk=maxmapk;
        this.mapminx=mapminx;
        this.mapminy=mapminy;
        this.mapmaxx=mapmaxx;
        this.mapmaxy=mapmaxy;
    }
}

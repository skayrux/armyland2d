package skayrux.shootout2d;

public class datagame_selectedmap {
    public int nmap;
    public boolean custommap;
    public String mapname, mapuid, timestamp;
    public int width, height;

    public datagame_selectedmap(String mapname, int nmap){
        this.mapname=mapname;
        this.nmap=nmap;
        this.custommap=false;
    }

    public datagame_selectedmap(String mapname, String mapuid, String timestamp, int width, int height){
        this.mapname=mapname;
        this.mapuid=mapuid;
        this.timestamp=timestamp;
        this.width=width;
        this.height=height;
        this.custommap=true;
    }
}

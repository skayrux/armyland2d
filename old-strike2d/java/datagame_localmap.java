package skayrux.shootout2d;

import java.util.Date;

public class datagame_localmap {
    public String mapname, mapuid, timestamp;
    public int width, height;
    public Date updated;
    public int userid;
    public boolean mymap, online;

    public datagame_localmap(String mapname, String mapuid, int userid, int width, int height, String timestamp, boolean mymap, boolean online){
        this.mapname=mapname;
        this.mapuid=mapuid;
        this.userid=userid;
        this.width=width;
        this.height=height;
        this.mymap=mymap;
        this.online=online;
        this.timestamp=timestamp;
        try {
            long ms=Long.parseLong(timestamp);
            this.updated = new Date(ms);
        } catch (Exception e) {
            this.updated = new Date();
        }
    }
}
